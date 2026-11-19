package com.parlandim.carteirama.common.validation;

public final class Patterns {

    public static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    public static final String PHONE = "^\\d{10,11}$";
    public static final String ZIP = "^\\d{8}$";
    public static final String UF = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$";
    public static final String HEX_COLOR = "^#[0-9A-Fa-f]{6}$";
    public static final String PASSWORD_STRENGTH = "^(?=.*[A-Za-z])(?=.*\\d).*$";

    private Patterns() {
    }
}
