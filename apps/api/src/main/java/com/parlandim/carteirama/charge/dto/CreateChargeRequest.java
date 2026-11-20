package com.parlandim.carteirama.charge.dto;

import com.parlandim.carteirama.common.validation.Text;
import com.parlandim.carteirama.model.enums.ChargeKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateChargeRequest(
        UUID clientId,
        @Size(max = 120) String payerName,
        UUID groupId,
        @NotNull @Min(1) @Max(99_999_999) Integer amountCents,
        @NotNull LocalDate dueDate,
        @Size(max = 500) String note,
        ChargeKind kind,
        @Size(max = 120) String description
) {
    public CreateChargeRequest {
        note = Text.trimToNull(note);
        payerName = Text.trimToNull(payerName);
        kind = kind == null ? ChargeKind.recurring : kind;
        description = Text.trimToNull(description);
    }
}
