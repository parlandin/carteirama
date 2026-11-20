package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminBootstrapTest extends IntegrationTest {

    @Autowired
    private AdminBootstrap bootstrap;

    @Test
    void criaOPrimeiroAdminPelasVariaveisDeAmbiente() {
        assertThat(bootstrap.ensureAdmin(" Dona@Example.com ", "senhaForte123", "Dona Admin")).isTrue();

        Api api = api();
        var login = api.post("/api/v1/auth/login", Map.of("email", "dona@example.com", "password", "senhaForte123"));
        assertThat(login.status()).isEqualTo(200);
        assertThat(login.json().path("user").path("is_admin").asBoolean()).isTrue();
        assertThat(login.json().path("user").path("name").asString()).isEqualTo("Dona Admin");
    }

    @Test
    void naoAlteraAdminQueJaExiste() {
        bootstrap.ensureAdmin("dona@example.com", "senhaForte123", null);

        assertThat(bootstrap.ensureAdmin("dona@example.com", "outraSenha456", null)).isFalse();

        var antiga = api().post("/api/v1/auth/login", Map.of("email", "dona@example.com", "password", "senhaForte123"));
        assertThat(antiga.status()).isEqualTo(200);
        var nova = api().post("/api/v1/auth/login", Map.of("email", "dona@example.com", "password", "outraSenha456"));
        assertThat(nova.status()).isEqualTo(401);
    }

    @Test
    void semVariaveisNaoFazNada() {
        assertThat(bootstrap.ensureAdmin("", "", "")).isFalse();
        assertThat(bootstrap.ensureAdmin(null, null, null)).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void recusaConfiguracaoInvalida() {
        assertThatThrownBy(() -> bootstrap.ensureAdmin("nao-e-email", "senhaForte123", null))
                .hasMessageContaining("ADMIN_EMAIL");
        assertThatThrownBy(() -> bootstrap.ensureAdmin("", "senhaForte123", null))
                .hasMessageContaining("ADMIN_EMAIL");
        assertThatThrownBy(() -> bootstrap.ensureAdmin("dona@example.com", "curta1", null))
                .hasMessageContaining("ADMIN_PASSWORD");
        assertThatThrownBy(() -> bootstrap.ensureAdmin("dona@example.com", null, null))
                .hasMessageContaining("ADMIN_PASSWORD");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }
}
