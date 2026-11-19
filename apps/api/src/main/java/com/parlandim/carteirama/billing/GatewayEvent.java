package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.model.enums.PaymentMethod;

import java.time.Instant;
import java.time.LocalDate;

public record GatewayEvent(String eventId, Type type, String rawType, String customerId, String subscriptionId,
                           String chargeId, Integer amountCents, LocalDate dueDate, Instant paidAt,
                           PaymentMethod method, LocalDate nextDueDate, String payload) {
    public enum Type {
        INVOICE_CREATED,
        PAYMENT_CONFIRMED,
        PAYMENT_OVERDUE,
        SUBSCRIPTION_CANCELLED,
        IGNORED
    }
}
