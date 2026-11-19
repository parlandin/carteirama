package com.parlandim.carteirama.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Com RESEND_API_KEY, envia pela Resend; sem ela, os e-mails só vão para o log. */
@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    EmailSender emailSender(@Value("${carteirama.email.resend-api-key:}") String apiKey,
                            @Value("${carteirama.email.resend-base-url:https://api.resend.com}") String baseUrl,
                            @Value("${carteirama.email.from}") String from) {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY não configurada: e-mails serão apenas registrados no log.");
            return new LoggingEmailSender();
        }
        log.info("E-mails serão enviados pela Resend como {}", from);
        return new ResendEmailSender(baseUrl, apiKey.strip(), from, d -> Thread.sleep(d));
    }
}
