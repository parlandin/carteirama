package com.parlandim.carteirama.charge.dto;

import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.enums.ChargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ChargeResponse(UUID id, UUID clientId, String clientName, UUID groupId, LocalDate referenceMonth,
                             int amountCents, LocalDate dueDate, ChargeStatus status, String displayStatus,
                             LocalDate paidAt, String note) {
    public static ChargeResponse of(Charge c, LocalDate today) {
        boolean late = c.getStatus() == ChargeStatus.pending && c.getDueDate().isBefore(today);
        return new ChargeResponse(c.getId(), c.getClient().getId(), c.getClient().getName(),
                c.getClient().getGroup().getId(), c.getReferenceMonth(), c.getAmountCents(), c.getDueDate(),
                c.getStatus(), late ? "late" : c.getStatus().name(), c.getPaidAt(), c.getNote());
    }
}
