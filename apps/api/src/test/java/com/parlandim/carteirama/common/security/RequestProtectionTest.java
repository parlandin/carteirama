package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.Fixtures;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestProtectionTest extends IntegrationTest {

    @Test
    void escritaSemTokenCsrfEhRecusada() {
        Api api = api();
        var res = api.postWithoutCsrf("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), Fixtures.cpf()));

        assertThat(res.status()).isEqualTo(403);
        assertThat(res.code()).isEqualTo("csrf_invalid");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void tokenCsrfEhEntregueNumCookieLegivelPeloFront() {
        var res = api().get("/api/v1/auth/csrf");

        assertThat(res.status()).isEqualTo(204);
        assertThat(res.headers().allValues("Set-Cookie"))
                .anySatisfy(c -> assertThat(c).startsWith("XSRF-TOKEN=").contains("Path=/").doesNotContain("HttpOnly"));
    }

    @Test
    void escritaDeOutraOrigemEhRecusada() {
        Api api = api();
        var res = api.postWithOrigin("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), Fixtures.cpf()),
                "https://site-malicioso.example");

        assertThat(res.status()).isEqualTo(403);
        assertThat(res.code()).isEqualTo("forbidden_origin");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void escritaDaOrigemDoFrontEhAceita() {
        var res = api().postWithOrigin("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), Fixtures.cpf()),
                "http://localhost:8080");

        assertThat(res.status()).isEqualTo(201);
    }
}
