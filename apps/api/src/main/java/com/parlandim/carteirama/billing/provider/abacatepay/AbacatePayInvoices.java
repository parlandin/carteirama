package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.billing.GatewayEvent;
import com.parlandim.carteirama.billing.WebhookService;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.repository.InvoiceRepository;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AbacatePayInvoices {

    public static final String PREFIX = "abacatepay:";

    private final WebhookService webhookService;
    private final InvoiceRepository invoices;
    private final JsonMapper json = JsonMapper.builder().build();

    public AbacatePayInvoices(WebhookService webhookService, InvoiceRepository invoices) {
        this.webhookService = webhookService;
        this.invoices = invoices;
    }

    public static String subscriptionId(UUID subscriptionId) {
        return PREFIX + subscriptionId;
    }

    public Invoice issue(String subscriptionId, int amountCents, LocalDate dueDate) {
        String chargeId = subscriptionId + ":" + dueDate;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscription_id", subscriptionId);
        payload.put("charge_id", chargeId);
        payload.put("amount_cents", amountCents);
        payload.put("due_date", dueDate.toString());
        webhookService.process(new GatewayEvent("abacatepay:invoice:" + chargeId, GatewayEvent.Type.INVOICE_CREATED,
                "abacatepay.invoice.issued", null, subscriptionId, chargeId, amountCents, dueDate, null, null, null,
                json.writeValueAsString(payload)));
        return invoices.findByGatewayChargeId(chargeId).orElseThrow();
    }

    public void endSubscription(String subscriptionId) {
        webhookService.process(new GatewayEvent("abacatepay:cancel:" + subscriptionId,
                GatewayEvent.Type.SUBSCRIPTION_CANCELLED, "abacatepay.subscription.ended", null, subscriptionId,
                null, null, null, null, null, null,
                json.writeValueAsString(Map.of("subscription_id", subscriptionId))));
    }
}
