package com.parlandim.carteirama.export;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportTest extends IntegrationTest {

    @Test
    void exportaCsvEXlsxMesmoComContaSuspensa() {
        Api api = registered();
        String g = api.post("/api/v1/groups", Map.of("name", "Projeto; X")).json().path("id").asString();
        api.post("/api/v1/clients", Api.body("name", "=Ana", "group_id", g, "amount_cents", 5900, "due_day", 10));
        api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));
        setAccountStatus(accountIdOf(api), "suspended");

        var csv = api.get("/api/v1/export?type=charges&format=csv&month=2026-12");
        assertThat(csv.status()).isEqualTo(200);
        assertThat(csv.headers().firstValue("content-type").orElseThrow()).startsWith("text/csv");
        assertThat(csv.headers().firstValue("content-disposition").orElseThrow())
                .contains("carteirama-cobrancas-2026-12.csv");
        assertThat(csv.raw()).contains("Cliente;Grupo;Mês;Vencimento;Valor (R$);Status;Pago em;Nota");
        assertThat(csv.raw()).contains("'=Ana;\"Projeto; X\";2026-12;10/12/2026;59,00;Pendente;;");

        var xlsx = api.get("/api/v1/export?type=clients&format=xlsx");
        assertThat(xlsx.status()).isEqualTo(200);
        assertThat(xlsx.headers().firstValue("content-disposition").orElseThrow()).contains("carteirama-clientes.xlsx");
        assertThat(xlsx.raw()).startsWith("PK");

        assertThat(api.get("/api/v1/export?type=groups&format=csv").status()).isEqualTo(200);
    }

    @Test
    void validaParametros() {
        Api api = registered();
        assertThat(api.get("/api/v1/export?type=clients&format=pdf").details()).containsExactly("format:invalid_value");
        assertThat(api.get("/api/v1/export?type=x&format=csv").details()).containsExactly("type:invalid_value");
        assertThat(api.get("/api/v1/export?type=clients").details()).containsExactly("format:required");
    }

    @Test
    void exportaSoDadosDaPropriaConta() {
        Api a = registered();
        Api b = registered();
        String g = a.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        a.post("/api/v1/clients", Api.body("name", "Cliente da A", "group_id", g, "amount_cents", 100, "due_day", 1));

        assertThat(b.get("/api/v1/export?type=clients&format=csv").raw()).doesNotContain("Cliente da A");
    }
}
