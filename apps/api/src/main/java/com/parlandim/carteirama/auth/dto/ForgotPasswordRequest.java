package com.parlandim.carteirama.auth.dto;

import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(@NotBlank @Size(max = 254) @Pattern(regexp = Patterns.EMAIL) String email) {
    public ForgotPasswordRequest {
        email = Text.email(email);
    }
}
