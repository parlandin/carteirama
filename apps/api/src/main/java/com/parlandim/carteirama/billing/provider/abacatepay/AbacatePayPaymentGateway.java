package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.billing.GatewayEvent;
import com.parlandim.carteirama.billing.InvalidWebhookSignatureException;
import com.parlandim.carteirama.billing.PaymentGateway;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.InvoiceStatus;
import com.parlandim.carteirama.model.enums.PaymentMethod;
import com.parlandim.carteirama.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class AbacatePayPaymentGateway implements PaymentGateway {

    static final String PAID_EVENT = "transparent.completed";
    private static final Duration MIN_PIX_VALIDITY = Duration.ofMinutes(5);

    private final AbacatePayClient client;
    private final AbacatePayPixStore pixStore;
    private final AbacatePayInvoices abacatePayInvoices;
    private final InvoiceRepository invoices;
    private final AbacatePayProperties properties;
    private final BusinessClock clock;
    private final String subscriptionPageUrl;
    private final JsonMapper json = JsonMapper.builder().build();

    public AbacatePayPaymentGateway(AbacatePayClient client, AbacatePayPixStore pixStore,
                                    AbacatePayInvoices abacatePayInvoices, InvoiceRepository invoices,
                                    AbacatePayProperties properties, BusinessClock clock, String frontendUrl) {
        this.client = client;
        this.pixStore = pixStore;
        this.abacatePayInvoices = abacatePayInvoices;
        this.invoices = invoices;
        this.properties = properties;
        this.clock = clock;
        this.subscriptionPageUrl = frontendUrl.replaceAll("/+$", "") + "/app/assinatura";
    }

    @Override
    public String createCheckout(Account account, Subscription subscription) {
        if (subscription.getGatewaySubscriptionId() == null) {
            subscription.setGatewaySubscriptionId(AbacatePayInvoices.subscriptionId(subscription.getId()));
        }
        Optional<Invoice> open = invoices.findTop24ByAccountIdOrderByDueDateDesc(account.getId()).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.pending)
                .min(Comparator.comparing(Invoice::getDueDate));
        if (open.isPresent()) {
            return payUrl(open.get());
        }
        LocalDate today = clock.today();
        LocalDate next = subscription.getNextDueDate();
        if (account.getStatus() == AccountStatus.active && next != null
                && next.isAfter(today.plusDays(properties.invoiceDaysBefore()))) {
            throw ApiException.conflict("nothing_to_pay", "Sua assinatura está em dia. A próxima fatura aparece aqui "
                    + properties.invoiceDaysBefore() + " dias antes do vencimento.");
        }
        LocalDate due = next != null ? next : trialEnd(account);
        if (due == null || due.isBefore(today)) {
            due = today;
        }
        return payUrl(abacatePayInvoices.issue(subscription.getGatewaySubscriptionId(), subscription.getPriceCents(), due));
    }

    @Override
    public PaymentLink paymentLink(Invoice invoice) {
        String brCode = pixStore.reusableBrCode(invoice.getId(), invoice.getAmountCents(),
                clock.now().plus(MIN_PIX_VALIDITY)).orElseGet(() -> {
            Account account = invoice.getAccount();
            String payer = PixLabels.payerName(account);
            AbacatePayClient.Pix pix = client.createPix(invoice.getAmountCents(), properties.pixExpiresIn(),
                    PixLabels.description(payer, invoice.getDueDate(), invoice.getNumber()),
                    PixLabels.externalId(payer, invoice.getDueDate(), clock.now()),
                    Map.of("invoice_id", invoice.getId().toString(), "invoice_number", invoice.getNumber(),
                            "account_id", account.getId().toString()),
                    PixLabels.customer(account, payer));
            pixStore.save(invoice.getId(), invoice.getAmountCents(), pix);
            return pix.brCode();
        });
        return new PaymentLink(brCode, null, null);
    }

    @Override
    public void cancelAtPeriodEnd(Subscription subscription) {
    }

    @Override
    public void resume(Subscription subscription) {
    }

    @Override
    public GatewayEvent parseWebhook(Map<String, String> headers, String body) {
        if (!validSecret(headers.get("x-webhook-secret"))) {
            throw new InvalidWebhookSignatureException();
        }
        JsonNode event;
        try {
            event = json.readTree(body);
        } catch (JacksonException e) {
            throw invalidPayload();
        }
        String eventId = event.path("id").asString(null);
        String type = event.path("event").asString("");
        if (eventId == null || eventId.isBlank()) {
            throw invalidPayload();
        }
        if (!PAID_EVENT.equals(type)) {
            return ignored(eventId, type, body);
        }
        String pixId = event.path("data").path("transparent").path("id").asString(null);
        Optional<AbacatePayPixStore.PixInvoice> found = pixId == null ? Optional.empty() : pixStore.findInvoice(pixId);
        if (found.isEmpty() || found.get().chargeId() == null || found.get().subscriptionId() == null) {
            log.warn("Evento {} da AbacatePay para um Pix desconhecido ({})", eventId, pixId);
            return ignored(eventId, type, body);
        }
        String status = client.pixStatus(pixId);
        if (!"PAID".equals(status)) {
            log.warn("Evento {} da AbacatePay diz pago, mas o Pix {} está {}", eventId, pixId, status);
            throw new ApiException(HttpStatus.CONFLICT, "payment_not_confirmed", "O pagamento ainda não foi confirmado.");
        }
        pixStore.markStatus(pixId, status);
        AbacatePayPixStore.PixInvoice paid = found.get();
        LocalDate next = paid.dueDate().plusMonths(1);
        if (paid.nextDueDate() != null && paid.nextDueDate().isAfter(next)) {
            next = paid.nextDueDate();
        }
        return new GatewayEvent(eventId, GatewayEvent.Type.PAYMENT_CONFIRMED, type, null, paid.subscriptionId(),
                paid.chargeId(), paid.amountCents(), paid.dueDate(), clock.now(), PaymentMethod.pix, next, body);
    }

    private boolean validSecret(String received) {
        if (received == null || received.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(received.getBytes(StandardCharsets.UTF_8),
                properties.webhookSecret().strip().getBytes(StandardCharsets.UTF_8));
    }

    private String payUrl(Invoice invoice) {
        return subscriptionPageUrl + "/faturas/" + invoice.getId();
    }

    private static LocalDate trialEnd(Account account) {
        return account.getTrialEndsAt() == null ? null
                : LocalDate.ofInstant(account.getTrialEndsAt(), BusinessClock.ZONE);
    }

    private static GatewayEvent ignored(String eventId, String type, String body) {
        return new GatewayEvent(eventId, GatewayEvent.Type.IGNORED, type, null, null, null, null, null, null, null,
                null, body);
    }

    private static ApiException invalidPayload() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_payload", "Corpo do webhook inválido.");
    }
}
