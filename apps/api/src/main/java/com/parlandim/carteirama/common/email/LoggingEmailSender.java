package com.parlandim.carteirama.common.email;

import lombok.extern.slf4j.Slf4j;

/** Usado quando não há provedor de e-mail configurado (ver EmailConfig). */
@Slf4j
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info("E-mail (sem provedor configurado) para {}: {}", message.to(), message.subject());
        log.debug("Corpo do e-mail para {}:\n{}", message.to(), message.text());
    }
}
