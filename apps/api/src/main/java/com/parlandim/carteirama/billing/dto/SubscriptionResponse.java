package com.parlandim.carteirama.billing.dto;

import com.parlandim.carteirama.model.enums.AccountStatus;
import com.parlandim.carteirama.model.enums.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record SubscriptionResponse(PlanView plan, SubscriptionStatus status, int priceCents, LocalDate nextDueDate,
                                   boolean cancelAtPeriodEnd, AccountStatus accountStatus, Instant trialEndsAt,
                                   Usage usage) {
    public record PlanView(String code, String name) {
    }

    public record Usage(long clients, int users) {
    }
}
