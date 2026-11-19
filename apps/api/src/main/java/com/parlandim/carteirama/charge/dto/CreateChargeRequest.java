package com.parlandim.carteirama.charge.dto;

import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateChargeRequest(
        @NotNull UUID clientId,
        @NotNull @Min(1) @Max(99_999_999) Integer amountCents,
        @NotNull LocalDate dueDate,
        @Size(max = 500) String note
) {
    public CreateChargeRequest {
        note = Text.trimToNull(note);
    }
}
