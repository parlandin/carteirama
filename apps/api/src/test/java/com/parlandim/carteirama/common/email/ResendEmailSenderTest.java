package com.parlandim.carteirama.common.email;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResendEmailSenderTest {

    private record Request(String auth, String idempotencyKey, String body) {
    }

    private record Reply(int status, String body, String retryAfter) {
    }

    private HttpServer server;
    private final List<Request> requests = new ArrayList<>();
    private final Deque<Reply> replies = new ArrayDeque<>();
    private final List<Duration> waits = new ArrayList<>();
    private ResendEmailSender sender;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", ex -> {
            requests.add(new Request(ex.getRequestHeaders().getFirst("Authorization"),
                    ex.getRequestHeaders().getFirst("Idempotency-Key"),
                    new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Reply r = replies.isEmpty() ? new Reply(200, "{\"id\":\"ok\"}", null) : replies.poll();
            if (r.retryAfter() != null) {
                ex.getResponseHeaders().add("Retry-After", r.retryAfter());
            }
            ex.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = r.body().getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(r.status(), bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
        sender = new ResendEmailSender("http://127.0.0.1:" + server.getAddress().getPort(), "re_test",
                "Carteirama <onboarding@resend.dev>", waits::add);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void enviaComChaveEMesmoIdempotencyKeyNasNovasTentativas() {
        replies.add(new Reply(500, "{\"message\":\"erro\"}", null));
        replies.add(new Reply(429, "{\"message\":\"devagar\"}", "2"));

        sender.send("ana@exemplo.com", "Redefinir sua senha", "Olá, Ana.");

        assertThat(requests).hasSize(3);
        assertThat(requests).allSatisfy(r -> assertThat(r.auth()).isEqualTo("Bearer re_test"));
        assertThat(requests.stream().map(Request::idempotencyKey).distinct()).hasSize(1).doesNotContainNull();
        assertThat(requests.get(0).body())
                .contains("\"from\":\"Carteirama <onboarding@resend.dev>\"")
                .contains("\"to\":[\"ana@exemplo.com\"]")
                .contains("\"subject\":\"Redefinir sua senha\"")
                .contains("\"text\":\"Olá, Ana.\"");
        assertThat(waits).containsExactly(Duration.ofMillis(500), Duration.ofSeconds(2));
    }

    @Test
    void erro4xxNaoRepeteENaoLancaExcecao() {
        replies.add(new Reply(422, "{\"message\":\"from inválido\"}", null));

        sender.send("ana@exemplo.com", "Assunto", "Corpo");

        assertThat(requests).hasSize(1);
        assertThat(waits).isEmpty();
    }

    @Test
    void desisteDepoisDoLimiteDeTentativas() {
        for (int i = 0; i < 5; i++) {
            replies.add(new Reply(503, "{}", null));
        }
        sender.send("ana@exemplo.com", "Assunto", "Corpo");

        assertThat(requests).hasSize(ResendEmailSender.MAX_ATTEMPTS);
        assertThat(waits).containsExactly(Duration.ofMillis(500), Duration.ofSeconds(1));
    }

    @Test
    void novoEmailGanhaNovaIdempotencyKey() {
        sender.send("ana@exemplo.com", "Um", "Corpo");
        sender.send("ana@exemplo.com", "Dois", "Corpo");

        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).idempotencyKey()).isNotEqualTo(requests.get(1).idempotencyKey());
    }
}
