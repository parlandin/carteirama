package com.parlandim.carteirama.common.email;

public interface EmailSender {

    void send(String to, String subject, String body);
}
