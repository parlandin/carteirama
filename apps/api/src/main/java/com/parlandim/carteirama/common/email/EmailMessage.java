package com.parlandim.carteirama.common.email;

public record EmailMessage(String to, String subject, String text, String html) {
}
