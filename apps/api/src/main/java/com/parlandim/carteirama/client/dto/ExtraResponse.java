package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.model.ClientExtra;
import com.parlandim.carteirama.model.enums.ExtraKind;

import java.time.LocalDate;
import java.util.UUID;

public record ExtraResponse(UUID id, ExtraKind kind, String description, int amountCents, boolean recurring,
                            LocalDate startMonth, LocalDate endMonth) {
    public static ExtraResponse of(ClientExtra e) {
        return new ExtraResponse(e.getId(), e.getKind(), e.getDescription(), e.getAmountCents(), e.isRecurring(),
                e.getStartMonth(), e.getEndMonth());
    }
}
