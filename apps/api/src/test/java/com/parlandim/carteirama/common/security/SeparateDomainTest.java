package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.support.Fixtures;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "CORS_ALLOWED_ORIGINS=https://app.example.com/",
        "CSRF_COOKIE_DOMAIN=example.com"
})
class SeparateDomainTest extends IntegrationTest {

    @Test
    void preflightDoFrontLiberaCredenciaisETokenCsrf() {
        var res = api().preflight("/api/v1/clients", "https://app.example.com", "POST");

        assertThat(res.status()).isEqualTo(200);
        assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).hasValue("https://app.example.com");
        assertThat(res.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");
        assertThat(res.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase())
                .contains("x-xsrf-token");
    }

    @Test
    void preflightDeOutroSiteEhRecusado() {
        var res = api().preflight("/api/v1/clients", "https://site-malicioso.example", "POST");

        assertThat(res.status()).isEqualTo(403);
        assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test
    void cookieCsrfFicaNoDominioCompartilhado() {
        var res = api().get("/api/v1/auth/csrf");

        assertThat(res.headers().allValues("Set-Cookie"))
                .anySatisfy(c -> assertThat(c).startsWith("XSRF-TOKEN=").contains("Domain=example.com"));
    }

    @Test
    void origemDoFrontPassaPeloFiltroEAindaExigeCsrf() {
        var res = api().postWithoutCsrf("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), Fixtures.cpf()),
                "https://app.example.com");

        assertThat(res.status()).isEqualTo(403);
        assertThat(res.code()).isEqualTo("csrf_invalid");
    }
}
