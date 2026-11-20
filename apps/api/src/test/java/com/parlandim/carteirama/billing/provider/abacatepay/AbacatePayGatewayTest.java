package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.billing.GatewayEvent;
import com.parlandim.carteirama.billing.InvalidWebhookSignatureException;
import com.parlandim.carteirama.billing.PaymentGateway;
import com.parlandim.carteirama.billing.WebhookService;
import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.repository.InvoiceRepository;
import com.parlandim.carteirama.repository.SubscriptionRepository;
import com.parlandim.carteirama.support.IntegrationTest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbacatePayGatewayTest extends IntegrationTest {

    private static final String SECRET = "segredo-do-webhook-com-mais-de-32-caracteres";

    @Autowired
    private WebhookService webhookService;
    @Autowired
    private InvoiceRepository invoices;
    @Autowired
    private SubscriptionRepository subscriptions;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private BusinessClock clock;

    private HttpServer server;
    private final List<String> created = new ArrayList<>();
    private final List<String> checked = new ArrayList<>();
    private volatile String checkStatus = "PAID";
    private volatile boolean rejectCustomer = false;
    private AbacatePayPaymentGateway gateway;
    private AbacatePayBillingJobs jobs;
    private TransactionTemplate tx;
    private TransactionTemplate readOnlyTx;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/transparents/create", ex -> {
            String request = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (rejectCustomer && request.contains("\"customer\"")) {
                reply(ex, 422, """
                        {"data":null,"success":false,"error":"Invalid customer"}
                        """);
                return;
            }
            created.add(request);
            int n = created.size();
            reply(ex, """
                    {"data":{"id":"pix_char_%d","amount":2499,"status":"PENDING","devMode":true,"brCode":"000201pix%d",
                    "brCodeBase64":"data:image/png;base64,AA==","expiresAt":"%s"},"success":true,"error":null}
                    """.formatted(n, n, Instant.now().plus(Duration.ofHours(1))));
        });
        server.createContext("/transparents/check", ex -> {
            checked.add(ex.getRequestURI().getQuery());
            reply(ex, """
                    {"data":{"id":"pix_char_1","status":"%s","expiresAt":"%s"},"success":true,"error":null}
                    """.formatted(checkStatus, Instant.now().plus(Duration.ofHours(1))));
        });
        server.start();

        var properties = new AbacatePayProperties("chave", SECRET, "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofHours(1), 5);
        var client = new AbacatePayClient(properties.baseUrl(), properties.apiKey());
        var pixStore = new AbacatePayPixStore(jdbc, transactionManager);
        var abacatePayInvoices = new AbacatePayInvoices(webhookService, invoices);
        gateway = new AbacatePayPaymentGateway(client, pixStore, abacatePayInvoices, invoices, properties, clock,
                "http://app.test/");
        jobs = new AbacatePayBillingJobs(jdbc, abacatePayInvoices, properties, clock);
        tx = new TransactionTemplate(transactionManager);
        readOnlyTx = new TransactionTemplate(transactionManager);
        readOnlyTx.setReadOnly(true);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void reply(HttpExchange ex, String body) throws IOException {
        reply(ex, 200, body);
    }

    private static void reply(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private String checkout(UUID accountId) {
        return tx.execute(s -> {
            var subscription = subscriptions.findByAccountId(accountId).orElseThrow();
            return gateway.createCheckout(subscription.getAccount(), subscription);
        });
    }

    private PaymentGateway.PaymentLink paymentLink(UUID invoiceId) {
        return readOnlyTx.execute(s -> gateway.paymentLink(invoices.findById(invoiceId).orElseThrow()));
    }

    private UUID onlyInvoice(UUID accountId) {
        return jdbc.queryForObject("SELECT id FROM invoices WHERE account_id = ?", UUID.class, accountId);
    }

    private static String paidEvent(String eventId, String type, String pixId) {
        return """
                {"id":"%s","event":"%s","apiVersion":2,"devMode":true,
                "data":{"transparent":{"id":"%s","amount":2499,"status":"PAID"},
                "payerInformation":{"method":"PIX"}}}
                """.formatted(eventId, type, pixId);
    }

    @Test
    void checkoutGeraFaturaPixEConfirmaPeloWebhook() {
        UUID accountId = accountIdOf(registered());

        String url = checkout(accountId);
        UUID invoiceId = onlyInvoice(accountId);
        assertThat(url).isEqualTo("http://app.test/app/assinatura/faturas/" + invoiceId);
        assertThat(checkout(accountId)).isEqualTo(url);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices", Integer.class)).isEqualTo(1);

        LocalDate trialEnd = jdbc.queryForObject("SELECT (trial_ends_at AT TIME ZONE 'America/Sao_Paulo')::date FROM accounts WHERE id = ?",
                LocalDate.class, accountId);
        assertThat(jdbc.queryForMap("SELECT amount_cents, due_date, status::text AS status FROM invoices WHERE id = ?", invoiceId))
                .containsEntry("amount_cents", 2499)
                .containsEntry("due_date", java.sql.Date.valueOf(trialEnd))
                .containsEntry("status", "pending");

        var link = paymentLink(invoiceId);
        assertThat(link.pixCode()).isEqualTo("000201pix1");
        assertThat(link.boletoUrl()).isNull();
        assertThat(link.cardUrl()).isNull();
        assertThat(paymentLink(invoiceId).pixCode()).isEqualTo("000201pix1");
        assertThat(created).hasSize(1);
        assertThat(created.getFirst()).contains("\"method\":\"PIX\"", "\"amount\":2499", "\"expiresIn\":3600",
                "\"invoice_id\":\"" + invoiceId + "\"");

        String body = paidEvent("log_1", AbacatePayPaymentGateway.PAID_EVENT, "pix_char_1");
        assertThatThrownBy(() -> gateway.parseWebhook(Map.of("x-webhook-secret", "errado"), body))
                .isInstanceOf(InvalidWebhookSignatureException.class);
        assertThatThrownBy(() -> gateway.parseWebhook(Map.of(), body))
                .isInstanceOf(InvalidWebhookSignatureException.class);

        checkStatus = "PENDING";
        assertThatThrownBy(() -> gateway.parseWebhook(Map.of("x-webhook-secret", SECRET), body))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("payment_not_confirmed");

        checkStatus = "PAID";
        GatewayEvent event = gateway.parseWebhook(Map.of("x-webhook-secret", SECRET), body);
        assertThat(checked).last().isEqualTo("id=pix_char_1");
        assertThat(event.type()).isEqualTo(GatewayEvent.Type.PAYMENT_CONFIRMED);
        assertThat(webhookService.process(event)).isTrue();
        assertThat(webhookService.process(event)).isFalse();

        assertThat(jdbc.queryForMap("SELECT status::text AS status, method::text AS method FROM invoices WHERE id = ?", invoiceId))
                .containsEntry("status", "paid")
                .containsEntry("method", "pix");
        assertThat(jdbc.queryForObject("SELECT status::text FROM accounts WHERE id = ?", String.class, accountId))
                .isEqualTo("active");
        assertThat(jdbc.queryForMap("SELECT status::text AS status, next_due_date FROM subscriptions WHERE account_id = ?", accountId))
                .containsEntry("status", "active")
                .containsEntry("next_due_date", java.sql.Date.valueOf(trialEnd.plusMonths(1)));
        assertThat(jdbc.queryForObject("SELECT status FROM abacatepay_pix_charges WHERE id = 'pix_char_1'", String.class))
                .isEqualTo("PAID");
    }

    @Test
    void pixIdentificaOClienteEOVencimento() {
        UUID accountId = accountIdOf(registered());
        checkout(accountId);
        UUID invoiceId = onlyInvoice(accountId);
        var invoice = jdbc.queryForMap("SELECT number, due_date FROM invoices WHERE id = ?", invoiceId);
        String due = ((java.sql.Date) invoice.get("due_date")).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String document = jdbc.queryForObject("SELECT document FROM accounts WHERE id = ?", String.class, accountId);

        paymentLink(invoiceId);

        String body = created.getFirst();
        assertThat(body).contains(
                "\"description\":\"Carteirama - Fulano de Tal - venc. " + due + " - fatura " + invoice.get("number") + "\"",
                "\"externalId\":\"fulano-de-tal-" + due.replace('/', '-') + "-",
                "\"customer\":{",
                "\"name\":\"Fulano de Tal\"",
                "\"taxId\":\"" + document + "\"",
                "\"cellphone\":\"11988887777\"",
                "\"account_id\":\"" + accountId + "\"");
    }

    @Test
    void pixSaiSemPagadorQuandoAbacatePayRecusaOsDados() {
        UUID accountId = accountIdOf(registered());
        checkout(accountId);
        rejectCustomer = true;

        var link = paymentLink(onlyInvoice(accountId));

        assertThat(link.pixCode()).isEqualTo("000201pix1");
        assertThat(created).hasSize(1);
        assertThat(created.getFirst()).doesNotContain("\"customer\"").contains("Fulano de Tal");
    }

    @Test
    void rotulosDoPixRespeitamOsLimites() {
        String longName = "Construtora & Incorporadora São João dos Campos Reunidos Ltda 🚀 ".repeat(4);
        String description = PixLabels.description(longName, LocalDate.of(2026, 10, 20), "CRT-000123");

        assertThat(description).hasSizeLessThanOrEqualTo(PixLabels.DESCRIPTION_MAX)
                .startsWith("Carteirama - Construtora & Incorporadora São João")
                .endsWith(" - venc. 20/10/2026 - fatura CRT-000123")
                .doesNotContain("🚀");
        assertThat(PixLabels.externalId("João da Silva", LocalDate.of(2026, 10, 20), Instant.ofEpochMilli(36)))
                .isEqualTo("joao-da-silva-20-10-2026-10");
        assertThat(PixLabels.slug("🚀")).isEqualTo("cliente");
        assertThat(PixLabels.slug(longName)).hasSizeLessThanOrEqualTo(40).doesNotEndWith("-");
    }

    @Test
    void webhookIgnoraOutrosEventosEPixDesconhecido() {
        var headers = Map.of("x-webhook-secret", SECRET);
        assertThat(gateway.parseWebhook(headers, paidEvent("log_2", "transparent.refunded", "pix_char_1")).type())
                .isEqualTo(GatewayEvent.Type.IGNORED);
        assertThat(gateway.parseWebhook(headers, paidEvent("log_3", AbacatePayPaymentGateway.PAID_EVENT, "pix_char_x")).type())
                .isEqualTo(GatewayEvent.Type.IGNORED);
        assertThat(checked).isEmpty();
        assertThatThrownBy(() -> gateway.parseWebhook(headers, "não é json"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("invalid_payload");
    }

    @Test
    void assinaturaEmDiaNaoGeraFaturaAntecipada() {
        UUID accountId = accountIdOf(registered());
        setAccountStatus(accountId, "active");
        jdbc.update("UPDATE subscriptions SET status = 'active', next_due_date = ? WHERE account_id = ?",
                clock.today().plusDays(20), accountId);

        assertThatThrownBy(() -> checkout(accountId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("nothing_to_pay");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices", Integer.class)).isZero();
    }

    @Test
    void jobsGeramFaturasDoMesEEncerramCancelamentos() {
        UUID accountId = accountIdOf(registered());
        checkout(accountId);
        jdbc.update("UPDATE invoices SET status = 'paid', paid_at = now() WHERE account_id = ?", accountId);
        setAccountStatus(accountId, "active");
        LocalDate soon = clock.today().plusDays(3);
        jdbc.update("UPDATE subscriptions SET status = 'active', next_due_date = ? WHERE account_id = ?",
                clock.today().plusDays(10), accountId);

        jobs.issueUpcomingInvoices();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices WHERE status = 'pending'", Integer.class)).isZero();

        jdbc.update("UPDATE subscriptions SET next_due_date = ? WHERE account_id = ?", soon, accountId);
        jobs.issueUpcomingInvoices();
        jobs.issueUpcomingInvoices();
        assertThat(jdbc.queryForList("SELECT due_date FROM invoices WHERE status = 'pending'", LocalDate.class))
                .containsExactly(soon);

        jdbc.update("UPDATE subscriptions SET cancel_at_period_end = true, next_due_date = ? WHERE account_id = ?",
                clock.today().plusDays(1), accountId);
        jobs.endCancelledSubscriptions();
        assertThat(jdbc.queryForObject("SELECT status::text FROM subscriptions WHERE account_id = ?", String.class, accountId))
                .isEqualTo("active");

        jdbc.update("UPDATE subscriptions SET next_due_date = ? WHERE account_id = ?", clock.today(), accountId);
        jobs.endCancelledSubscriptions();
        assertThat(jdbc.queryForObject("SELECT status::text FROM subscriptions WHERE account_id = ?", String.class, accountId))
                .isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT status::text FROM accounts WHERE id = ?", String.class, accountId))
                .isEqualTo("cancelled");
    }
}
