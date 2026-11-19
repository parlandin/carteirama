package com.parlandim.carteirama.common.validation;

import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class Text {

    private Text() {
    }

    public static String trim(String s) {
        return s == null ? null : s.strip();
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    public static String email(String s) {
        String t = trim(s);
        return t == null ? null : t.toLowerCase(Locale.ROOT);
    }

    public static String digits(String s) {
        String t = trimToNull(s);
        return t == null ? null : t.replaceAll("[\\s.\\-/()]", "");
    }

    public static String upper(String s) {
        String t = trimToNull(s);
        return t == null ? null : t.toUpperCase(Locale.ROOT);
    }

    public static Optional<String> patch(Optional<String> value, UnaryOperator<String> normalizer) {
        if (value == null) {
            return null;
        }
        return value.map(normalizer);
    }
}
