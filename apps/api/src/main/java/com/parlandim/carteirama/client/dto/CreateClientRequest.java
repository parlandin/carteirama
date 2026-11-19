package com.parlandim.carteirama.client.dto;

import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateClientRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull UUID groupId,
        String document,
        @Size(max = 254) @Pattern(regexp = Patterns.EMAIL) String email,
        @Pattern(regexp = Patterns.PHONE) String phone,
        @NotNull @Min(1) @Max(99_999_999) Integer amountCents,
        @NotNull @Min(1) @Max(28) Integer dueDay,
        @Size(max = 2000) String notes
) {
    public CreateClientRequest {
        name = Text.trim(name);
        document = Text.digits(document);
        email = Text.trimToNull(Text.email(email));
        phone = Text.digits(phone);
        notes = Text.trimToNull(notes);
    }
}
