package com.parlandim.carteirama.support;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class Fixtures {

    public static final String PASSWORD = "senha1234";
    private static final AtomicInteger SEQ = new AtomicInteger();

    private Fixtures() {
    }

    public static String email() {
        return "user" + SEQ.incrementAndGet() + "@example.com";
    }

    public static Map<String, Object> registerPf(String email, String cpf) {
        return Api.body(
                "email", email,
                "password", PASSWORD,
                "name", "Fulano de Tal",
                "person_type", "pf",
                "document", cpf,
                "phone", "(11) 98888-7777",
                "terms_accepted", true);
    }

    public static String cpf() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (allSame(d, 9));
        d[9] = cpfDigit(d, 9);
        d[10] = cpfDigit(d, 10);
        return join(d);
    }

    public static String cnpj() {
        int[] d = new int[14];
        for (int i = 0; i < 8; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(10);
        }
        d[8] = 0;
        d[9] = 0;
        d[10] = 0;
        d[11] = 1;
        d[12] = cnpjDigit(d, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        d[13] = cnpjDigit(d, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return join(d);
    }

    private static int cpfDigit(int[] d, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += d[i] * (len + 1 - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static int cnpjDigit(int[] d, int[] w) {
        int sum = 0;
        for (int i = 0; i < w.length; i++) {
            sum += d[i] * w[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean allSame(int[] d, int len) {
        for (int i = 1; i < len; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }

    private static String join(int[] d) {
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }
}
