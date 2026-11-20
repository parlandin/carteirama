package com.parlandim.carteirama.charge.dto;

import com.parlandim.carteirama.model.Charge;
import com.parlandim.carteirama.model.enums.ChargeKind;
import com.parlandim.carteirama.model.enums.ChargeStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ChargeResponse(UUID id, UUID clientId, String clientName, UUID groupId, String groupName,
                             String groupColor, LocalDate referenceMonth,
                             int amountCents, LocalDate dueDate, ChargeStatus status, String displayStatus,
                             LocalDate paidAt, String note, ChargeKind kind, String description,
                             long bonusCents, long costCents, long totalCents, long netCents) {
    public static ChargeResponse of(Charge c, LocalDate today) {
        boolean late = c.getStatus() == ChargeStatus.pending && c.getDueDate().isBefore(today);
        var client = c.getClient();
        var group = c.getEffectiveGroup();
        return new ChargeResponse(c.getId(), client == null ? null : client.getId(), c.getPayer(),
                group == null ? null : group.getId(), group == null ? null : group.getName(),
                group == null ? null : group.getColor(), c.getReferenceMonth(), c.getAmountCents(), c.getDueDate(),
                c.getStatus(), late ? "late" : c.getStatus().name(), c.getPaidAt(), c.getNote(), c.getKind(),
                c.getDescription(), c.getBonusCents(), c.getCostCents(), c.getTotalCents(), c.getNetCents());
    }
}
