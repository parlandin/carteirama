package com.parlandim.carteirama.support;

import com.parlandim.carteirama.common.security.RateLimiter;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "COOKIE_SECURE=false")
public abstract class IntegrationTest {

    private static final EmbeddedPostgres PG = startPostgres();

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected ObjectMapper mapper;
    @Autowired
    protected CapturingEmailSender emails;
    @Autowired
    private RateLimiter rateLimiter;
    @Autowired
    private Environment env;

    private static EmbeddedPostgres startPostgres() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pg.close();
                } catch (IOException ignored) {
                }
            }));
            return pg;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE charges, clients, groups, audit_log, webhook_events, invoices, subscriptions,
                         password_resets, accounts, users, spring_session CASCADE
                """);
        jdbc.update("UPDATE plans SET price_cents = 2499, trial_days = 7, active = true WHERE code = 'cloud'");
        rateLimiter.clear();
        emails.clear();
    }

    protected Api api() {
        return new Api("http://localhost:" + env.getProperty("local.server.port"), mapper);
    }

    protected Api registered() {
        return registered(Fixtures.email(), Fixtures.cpf());
    }

    protected Api registered(String email, String cpf) {
        Api api = api();
        var res = api.post("/api/v1/auth/register", Fixtures.registerPf(email, cpf));
        if (res.status() != 201) {
            throw new AssertionError("Cadastro falhou: " + res.raw());
        }
        return api;
    }

    protected UUID accountIdOf(Api api) {
        return UUID.fromString(api.get("/api/v1/me").json().path("account").path("id").asString());
    }

    protected void setAccountStatus(UUID accountId, String status) {
        jdbc.update("UPDATE accounts SET status = ?::account_status WHERE id = ?", status, accountId);
    }

    protected static Map<String, Object> body(Object... keyValues) {
        return Api.body(keyValues);
    }
}
