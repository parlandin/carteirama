package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.common.validation.Text;
import com.parlandim.carteirama.model.enums.ExtraKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExtraRequest(
        @NotNull ExtraKind kind,
        @NotBlank @Size(max = 120) String description,
        @NotNull @Min(1) @Max(99_999_999) Integer amountCents,
        String month
) {
    public CreateExtraRequest {
        description = Text.trimToNull(description);
    }
}
