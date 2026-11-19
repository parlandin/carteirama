package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.error.ValidationException;

final class PasswordPolicy {

    private PasswordPolicy() {
    }

    static void requireDifferentFromEmail(String field, String password, String email) {
        if (password != null && email != null && password.equalsIgnoreCase(email)) {
            throw ValidationException.of(field, "weak_password");
        }
    }
}
