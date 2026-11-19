package com.parlandim.carteirama.auth.dto;

import com.parlandim.carteirama.common.validation.Patterns;
import com.parlandim.carteirama.common.validation.Text;
import com.parlandim.carteirama.model.enums.PersonType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 254) @Pattern(regexp = Patterns.EMAIL) String email,
        @NotNull @Size(min = 8, max = 72) @Pattern(regexp = Patterns.PASSWORD_STRENGTH, message = "weak_password") String password,
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotNull PersonType personType,
        @NotBlank String document,
        @Size(min = 2, max = 120) String legalName,
        String companyName,
        @NotBlank @Pattern(regexp = Patterns.PHONE) String phone,
        @Pattern(regexp = Patterns.ZIP) String zip,
        @Size(max = 200) String address,
        @Size(max = 120) String city,
        @Pattern(regexp = Patterns.UF) String state,
        @NotNull @AssertTrue Boolean termsAccepted
) {
    public RegisterRequest {
        email = Text.email(email);
        name = Text.trim(name);
        document = Text.digits(document);
        legalName = Text.trimToNull(legalName);
        companyName = Text.trimToNull(companyName);
        phone = Text.digits(phone);
        zip = Text.digits(zip);
        address = Text.trimToNull(address);
        city = Text.trimToNull(city);
        state = Text.upper(state);
    }
}
