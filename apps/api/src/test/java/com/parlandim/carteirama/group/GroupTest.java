package com.parlandim.carteirama.group;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GroupTest extends IntegrationTest {

    @Test
    void criarEditarListarEArquivar() {
        Api api = registered();
        var created = api.post("/api/v1/groups", Map.of("name", "  Projeto X ", "color", "#FF00aa"));
        assertThat(created.status()).isEqualTo(201);
        String id = created.json().path("id").asString();
        assertThat(created.json().path("name").asString()).isEqualTo("Projeto X");

        var dup = api.post("/api/v1/groups", Map.of("name", "projeto x"));
        assertThat(dup.status()).isEqualTo(409);
        assertThat(dup.code()).isEqualTo("group_name_taken");

        var patch = api.patch("/api/v1/groups/" + id, Api.body("notes", "anotação", "color", null));
        assertThat(patch.status()).isEqualTo(200);
        assertThat(patch.json().path("notes").asString()).isEqualTo("anotação");
        assertThat(patch.json().path("color").isNull()).isTrue();
        assertThat(patch.json().path("name").asString()).isEqualTo("Projeto X");

        api.post("/api/v1/groups", Map.of("name", "Assinatura SaaS"));
        var list = api.get("/api/v1/groups?month=2026-11");
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.json().path("data")).hasSize(2);
        assertThat(list.json().path("data").get(0).path("name").asString()).isEqualTo("Assinatura SaaS");
        assertThat(list.json().path("data").get(0).path("client_count").asLong()).isZero();
        assertThat(list.json().path("data").get(0).path("month_total_cents").asLong()).isZero();

        var detail = api.get("/api/v1/groups/" + id);
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.json().path("clients").isArray()).isTrue();

        assertThat(api.delete("/api/v1/groups/" + id).status()).isEqualTo(204);
        assertThat(api.get("/api/v1/groups").json().path("data")).hasSize(1);
        assertThat(api.post("/api/v1/groups", Map.of("name", "Projeto X")).status()).isEqualTo(201);
    }

    @Test
    void validacoes() {
        Api api = registered();
        var res = api.post("/api/v1/groups", Map.of("name", " ", "color", "vermelho"));
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.details()).containsExactlyInAnyOrder("name:required", "color:invalid_format");

        var longo = api.post("/api/v1/groups", Map.of("name", "x".repeat(61)));
        assertThat(longo.details()).containsExactly("name:too_long");

        String id = api.post("/api/v1/groups", Map.of("name", "A")).json().path("id").asString();
        assertThat(api.patch("/api/v1/groups/" + id, Map.of("name", "")).details()).containsExactly("name:required");
        assertThat(api.patch("/api/v1/groups/" + id, Api.body("name", null)).details()).containsExactly("name:required");

        var month = api.get("/api/v1/groups?month=2026-13");
        assertThat(month.details()).containsExactly("month:invalid_format");
        var farMonth = api.get("/api/v1/groups?month=2020-01");
        assertThat(farMonth.details()).containsExactly("month:out_of_range");
    }

    @Test
    void naoArquivaGrupoComClientesAtivos() {
        Api api = registered();
        String groupId = api.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        var client = api.post("/api/v1/clients", Map.of("name", "Cliente", "group_id", groupId,
                "amount_cents", 1000, "due_day", 10));
        assertThat(client.status()).isEqualTo(201);

        var res = api.delete("/api/v1/groups/" + groupId);
        assertThat(res.status()).isEqualTo(409);
        assertThat(res.code()).isEqualTo("group_not_empty");
    }

    @Test
    void isolamentoEntreContas() {
        Api a = registered();
        Api b = registered();
        String id = a.post("/api/v1/groups", Map.of("name", "Da conta A")).json().path("id").asString();

        assertThat(b.get("/api/v1/groups/" + id).status()).isEqualTo(404);
        assertThat(b.patch("/api/v1/groups/" + id, Map.of("name", "invadido")).status()).isEqualTo(404);
        assertThat(b.delete("/api/v1/groups/" + id).status()).isEqualTo(404);
        assertThat(b.get("/api/v1/groups").json().path("data")).isEmpty();
        assertThat(a.get("/api/v1/groups/" + id).json().path("name").asString()).isEqualTo("Da conta A");
    }

    @Test
    void contaSuspensaLeMasNaoEscreve() {
        Api api = registered();
        String id = api.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        setAccountStatus(accountIdOf(api), "suspended");

        assertThat(api.get("/api/v1/groups").status()).isEqualTo(200);
        assertThat(api.get("/api/v1/groups/" + id).status()).isEqualTo(200);
        for (var res : new Api.Res[]{
                api.post("/api/v1/groups", Map.of("name", "Outro")),
                api.patch("/api/v1/groups/" + id, Map.of("name", "X")),
                api.delete("/api/v1/groups/" + id)}) {
            assertThat(res.status()).isEqualTo(403);
            assertThat(res.code()).isEqualTo("account_suspended");
        }
    }
}
