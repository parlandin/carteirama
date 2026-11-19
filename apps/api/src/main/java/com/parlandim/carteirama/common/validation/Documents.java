package com.parlandim.carteirama.common.validation;

import java.util.Set;

public final class Documents {

    public static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private Documents() {
    }

    public static boolean isValidCpf(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}") || allSame(cpf)) {
            return false;
        }
        return checkDigit(cpf, 9, 10) == cpf.charAt(9) - '0'
                && checkDigit(cpf, 10, 11) == cpf.charAt(10) - '0';
    }

    public static boolean isValidCnpj(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}") || allSame(cnpj)) {
            return false;
        }
        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return cnpjDigit(cnpj, w1) == cnpj.charAt(12) - '0'
                && cnpjDigit(cnpj, w2) == cnpj.charAt(13) - '0';
    }

    public static boolean isValidCpfOrCnpj(String doc) {
        return isValidCpf(doc) || isValidCnpj(doc);
    }

    private static int checkDigit(String digits, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (startWeight - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static int cnpjDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean allSame(String s) {
        return s.chars().allMatch(c -> c == s.charAt(0));
    }
}
