package com.parlandim.carteirama.auth.dto;

import com.parlandim.carteirama.common.validation.Text;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password, Boolean remember) {
    public LoginRequest {
        email = Text.email(email);
    }
}
