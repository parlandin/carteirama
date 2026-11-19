package com.parlandim.carteirama.support;

import com.parlandim.carteirama.billing.GatewayEvent;
import com.parlandim.carteirama.billing.InvalidWebhookSignatureException;
import com.parlandim.carteirama.billing.PaymentGateway;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;
import com.parlandim.carteirama.model.enums.PaymentMethod;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Primary
@Component
public class FakePaymentGateway implements PaymentGateway {

    public static final String SIGNATURE = "segredo";

    private final ObjectMapper mapper;

    public FakePaymentGateway(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String createCheckout(Account account, Subscription subscription) {
        subscription.setGatewaySubscriptionId("sub_" + account.getId());
        return "https://pay.example/checkout/" + account.getId();
    }

    @Override
    public PaymentLink paymentLink(Invoice invoice) {
        return new PaymentLink("00020126pix" + invoice.getNumber(), "https://pay.example/boleto/" + invoice.getId(), null);
    }

    @Override
    public void cancelAtPeriodEnd(Subscription subscription) {
    }

    @Override
    public GatewayEvent parseWebhook(Map<String, String> headers, String body) {
        if (!SIGNATURE.equals(headers.get("x-test-signature"))) {
            throw new InvalidWebhookSignatureException();
        }
        JsonNode n = mapper.readTree(body);
        String type = n.path("type").asString();
        GatewayEvent.Type t = switch (type) {
            case "invoice.created" -> GatewayEvent.Type.INVOICE_CREATED;
            case "payment.confirmed" -> GatewayEvent.Type.PAYMENT_CONFIRMED;
            case "payment.overdue" -> GatewayEvent.Type.PAYMENT_OVERDUE;
            case "subscription.cancelled" -> GatewayEvent.Type.SUBSCRIPTION_CANCELLED;
            default -> GatewayEvent.Type.IGNORED;
        };
        return new GatewayEvent(n.path("id").asString(), t, type, null, n.path("subscription_id").asString(null),
                n.path("charge_id").asString(null),
                n.has("amount_cents") ? n.path("amount_cents").asInt() : null,
                n.has("due_date") ? LocalDate.parse(n.path("due_date").asString()) : null,
                t == GatewayEvent.Type.PAYMENT_CONFIRMED ? Instant.now() : null,
                n.has("method") ? PaymentMethod.valueOf(n.path("method").asString()) : null,
                null, body);
    }
}
