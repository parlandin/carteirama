package com.parlandim.carteirama.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("E-mail (sem provedor configurado) para {}: {}", to, subject);
        log.debug("Corpo do e-mail para {}:\n{}", to, body);
    }
}
