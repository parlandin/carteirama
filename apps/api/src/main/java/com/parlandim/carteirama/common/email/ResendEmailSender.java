package com.parlandim.carteirama.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Envia pela API da Resend (POST /emails).
 * <p>
 * Cada e-mail leva uma Idempotency-Key, repetida nas novas tentativas: se a primeira chegou e só a
 * resposta se perdeu, a Resend não envia de novo. Tenta outra vez em erro de rede, 5xx e 429
 * (respeitando o retry-after); outros 4xx são erro nosso e não adianta repetir.
 * Falhas são registradas no log e não interrompem quem chamou (login, rotinas diárias).
 */
@Slf4j
public class ResendEmailSender implements EmailSender {

    static final int MAX_ATTEMPTS = 3;
    private static final Duration MAX_WAIT = Duration.ofSeconds(5);

    /** Espera entre tentativas; trocada nos testes. */
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private record Sent(String id) {
    }

    private final RestClient http;
    private final String from;
    private final Sleeper sleeper;

    public ResendEmailSender(String baseUrl, String apiKey, String from, Sleeper sleeper) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.USER_AGENT, "carteirama-api")
                .build();
        this.from = from;
        this.sleeper = sleeper;
    }

    @Override
    public void send(EmailMessage message) {
        String to = message.to();
        String subject = message.subject();
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("from", from);
        payload.put("to", List.of(to));
        payload.put("subject", subject);
        payload.put("text", message.text());
        if (message.html() != null) {
            payload.put("html", message.html());
        }
        for (int attempt = 1; ; attempt++) {
            Duration wait;
            try {
                Sent sent = http.post().uri("/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(payload)
                        .retrieve()
                        .body(Sent.class);
                log.info("E-mail enviado pela Resend para {}: {} (id {})", to, subject, sent == null ? "?" : sent.id());
                return;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    log.error("Resend recusou o e-mail para {} ({}), tentativa {}: HTTP {} {}", to, subject, attempt,
                            status, e.getResponseBodyAsString());
                    return;
                }
                wait = retryAfter(e).orElse(backoff(attempt));
            } catch (ResourceAccessException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Falha de rede ao enviar e-mail para {} ({}) pela Resend: {}", to, subject, e.getMessage());
                    return;
                }
                wait = backoff(attempt);
            }
            try {
                sleeper.sleep(wait);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Envio de e-mail para {} interrompido", to);
                return;
            }
        }
    }

    private static Duration backoff(int attempt) {
        return Duration.ofMillis(500L << (attempt - 1)); // 0,5 s, 1 s
    }

    private static java.util.Optional<Duration> retryAfter(RestClientResponseException e) {
        HttpHeaders headers = e.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return java.util.Optional.empty();
        }
        try {
            Duration d = Duration.ofSeconds(Long.parseLong(value.strip()));
            return java.util.Optional.of(d.compareTo(MAX_WAIT) > 0 ? MAX_WAIT : d);
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }
}
