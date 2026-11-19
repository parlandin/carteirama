package com.parlandim.carteirama.auth.dto;

import com.parlandim.carteirama.common.validation.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotNull @Size(min = 8, max = 72) @Pattern(regexp = Patterns.PASSWORD_STRENGTH, message = "weak_password") String password
) {
}
