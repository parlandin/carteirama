package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.enums.PersonType;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class PixLabels {

    static final int DESCRIPTION_MAX = 140;
    private static final int SLUG_MAX = 40;
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ID_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private PixLabels() {
    }

    static String payerName(Account account) {
        if (account.getPersonType() == PersonType.pj && hasText(account.getCompanyName())) {
            return account.getCompanyName().strip();
        }
        if (hasText(account.getLegalName())) {
            return account.getLegalName().strip();
        }
        if (account.getUser() != null && hasText(account.getUser().getName())) {
            return account.getUser().getName().strip();
        }
        return "Cliente";
    }

    static String description(String payer, LocalDate dueDate, String invoiceNumber) {
        String suffix = " - venc. " + dueDate.format(DISPLAY_DATE) + " - fatura " + clean(invoiceNumber);
        String prefix = "Carteirama - ";
        int room = DESCRIPTION_MAX - prefix.length() - suffix.length();
        String name = clean(payer);
        if (name.length() > room) {
            name = name.substring(0, Math.max(0, room)).strip();
        }
        return prefix + name + suffix;
    }

    static String externalId(String payer, LocalDate dueDate, Instant now) {
        return slug(payer) + "-" + dueDate.format(ID_DATE) + "-" + Long.toString(now.toEpochMilli(), 36);
    }

    static AbacatePayClient.Customer customer(Account account, String payer) {
        String email = account.getUser() != null ? account.getUser().getEmail() : null;
        if (!hasText(email) || !hasText(account.getDocument()) || !hasText(account.getPhone())) {
            return null;
        }
        return new AbacatePayClient.Customer(clean(payer), email, account.getDocument(), account.getPhone());
    }

    static String slug(String text) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (ascii.length() > SLUG_MAX) {
            ascii = ascii.substring(0, SLUG_MAX).replaceAll("-+$", "");
        }
        return ascii.isEmpty() ? "cliente" : ascii;
    }

    private static String clean(String text) {
        return (text == null ? "" : text)
                .replaceAll("[^\\p{L}\\p{N} .,&()/-]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
