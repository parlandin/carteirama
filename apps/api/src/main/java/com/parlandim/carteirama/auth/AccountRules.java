package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.common.error.ValidationException;
import com.parlandim.carteirama.common.validation.Documents;
import com.parlandim.carteirama.model.enums.PersonType;

final class AccountRules {

    private AccountRules() {
    }

    static void checkDocument(ValidationException.Collector errors, PersonType type, String document) {
        if (type == null || document == null || document.isEmpty()) {
            return;
        }
        boolean valid = type == PersonType.pf ? Documents.isValidCpf(document) : Documents.isValidCnpj(document);
        if (!valid) {
            errors.add("document", "invalid_document", type == PersonType.pf ? "CPF inválido." : "CNPJ inválido.");
        }
    }

    static void checkCompanyName(ValidationException.Collector errors, PersonType type, String companyName) {
        if (type == PersonType.pj && companyName == null) {
            errors.add("company_name", "required");
        } else if (type == PersonType.pj && (companyName.length() < 2 || companyName.length() > 120)) {
            errors.add("company_name", companyName.length() < 2 ? "too_short" : "too_long");
        }
    }
}
