package com.parlandim.carteirama.client;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.Fixtures;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTest extends IntegrationTest {

    private String group(Api api, String name) {
        return api.post("/api/v1/groups", Map.of("name", name)).json().path("id").asString();
    }

    private Map<String, Object> client(String groupId, String name) {
        return Api.body("name", name, "group_id", groupId, "amount_cents", 5000, "due_day", 10);
    }

    @Test
    void criarBuscarEditarEArquivar() {
        Api api = registered();
        String g1 = group(api, "G1");
        String g2 = group(api, "G2");

        Map<String, Object> body = client(g1, "  Maria Souza ");
        body.put("email", " MARIA@Example.com ");
        body.put("document", Fixtures.cnpj());
        body.put("phone", "(21) 3333-4444");
        var created = api.post("/api/v1/clients", body);
        assertThat(created.status()).isEqualTo(201);
        String id = created.json().path("id").asString();
        assertThat(created.json().path("name").asString()).isEqualTo("Maria Souza");
        assertThat(created.json().path("email").asString()).isEqualTo("maria@example.com");
        assertThat(created.json().path("phone").asString()).isEqualTo("2133334444");
        assertThat(created.json().path("status").asString()).isEqualTo("active");
        assertThat(created.json().path("group_name").asString()).isEqualTo("G1");

        api.post("/api/v1/clients", client(g2, "João"));

        var all = api.get("/api/v1/clients");
        assertThat(all.json().path("meta").path("total").asLong()).isEqualTo(2);
        assertThat(all.json().path("data").get(0).path("name").asString()).isEqualTo("João");
        assertThat(api.get("/api/v1/clients?q=mari").json().path("data")).hasSize(1);
        assertThat(api.get("/api/v1/clients?group_id=" + g2).json().path("data")).hasSize(1);
        var paged = api.get("/api/v1/clients?page=2&limit=1");
        assertThat(paged.json().path("data")).hasSize(1);
        assertThat(paged.json().path("meta").path("page").asInt()).isEqualTo(2);

        var patch = api.patch("/api/v1/clients/" + id,
                Api.body("group_id", g2, "status", "paused", "email", null, "amount_cents", 7000));
        assertThat(patch.status()).isEqualTo(200);
        assertThat(patch.json().path("group_id").asString()).isEqualTo(g2);
        assertThat(patch.json().path("status").asString()).isEqualTo("paused");
        assertThat(patch.json().path("email").isNull()).isTrue();
        assertThat(patch.json().path("phone").asString()).isEqualTo("2133334444");
        assertThat(patch.json().path("amount_cents").asInt()).isEqualTo(7000);

        assertThat(api.get("/api/v1/clients/" + id).json().path("client").path("name").asString()).isEqualTo("Maria Souza");

        assertThat(api.delete("/api/v1/clients/" + id).status()).isEqualTo(204);
        assertThat(api.get("/api/v1/clients").json().path("meta").path("total").asLong()).isEqualTo(1);
        assertThat(api.get("/api/v1/clients?status=archived").json().path("data")).hasSize(1);
        var editArchived = api.patch("/api/v1/clients/" + id, Map.of("name", "Outro"));
        assertThat(editArchived.code()).isEqualTo("client_not_active");
    }

    @Test
    void validacoes() {
        Api api = registered();
        String g = group(api, "G");
        var res = api.post("/api/v1/clients", Api.body("name", "A", "group_id", g, "amount_cents", 0, "due_day", 29,
                "email", "x@", "document", "123"));
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.details()).contains("name:too_short", "amount_cents:out_of_range", "due_day:out_of_range",
                "email:invalid_format");

        var doc = api.post("/api/v1/clients", Api.body("name", "Ana", "group_id", g, "amount_cents", 100, "due_day", 1,
                "document", "111.111.111-11"));
        assertThat(doc.details()).containsExactly("document:invalid_document");

        var semGrupo = api.post("/api/v1/clients", client("00000000-0000-0000-0000-000000000000", "Ana"));
        assertThat(semGrupo.status()).isEqualTo(404);

        var statusInvalido = api.get("/api/v1/clients?status=late");
        assertThat(statusInvalido.details()).containsExactly("status:invalid_value");
        assertThat(api.get("/api/v1/clients?limit=101").details()).containsExactly("limit:out_of_range");
        assertThat(api.get("/api/v1/clients?group_id=abc").details()).containsExactly("group_id:invalid_format");

        String id = api.post("/api/v1/clients", client(g, "Ana")).json().path("id").asString();
        assertThat(api.patch("/api/v1/clients/" + id, Api.body("amount_cents", null)).details())
                .containsExactly("amount_cents:required");
        assertThat(api.patch("/api/v1/clients/" + id, Api.body("name", null)).details())
                .containsExactly("name:required");
    }

    @Test
    void grupoArquivadoOuDeOutraContaNaoAceitaClientes() {
        Api a = registered();
        Api b = registered();
        String groupB = group(b, "Da B");
        assertThat(a.post("/api/v1/clients", client(groupB, "Ana")).status()).isEqualTo(404);

        String archived = group(a, "Velho");
        a.delete("/api/v1/groups/" + archived);
        assertThat(a.post("/api/v1/clients", client(archived, "Ana")).status()).isEqualTo(404);
    }

    @Test
    void isolamentoEntreContas() {
        Api a = registered();
        Api b = registered();
        String id = a.post("/api/v1/clients", client(group(a, "G"), "Ana")).json().path("id").asString();

        assertThat(b.get("/api/v1/clients/" + id).status()).isEqualTo(404);
        assertThat(b.patch("/api/v1/clients/" + id, Map.of("name", "Invasor")).status()).isEqualTo(404);
        assertThat(b.delete("/api/v1/clients/" + id).status()).isEqualTo(404);
        assertThat(b.get("/api/v1/clients").json().path("meta").path("total").asLong()).isZero();
    }

    @Test
    void contaSuspensaLeMasNaoEscreve() {
        Api api = registered();
        String g = group(api, "G");
        String id = api.post("/api/v1/clients", client(g, "Ana")).json().path("id").asString();
        setAccountStatus(accountIdOf(api), "cancelled");

        assertThat(api.get("/api/v1/clients").status()).isEqualTo(200);
        assertThat(api.get("/api/v1/clients/" + id).status()).isEqualTo(200);
        assertThat(api.post("/api/v1/clients", client(g, "Bia")).code()).isEqualTo("account_suspended");
        assertThat(api.patch("/api/v1/clients/" + id, Map.of("name", "X")).code()).isEqualTo("account_suspended");
        assertThat(api.delete("/api/v1/clients/" + id).code()).isEqualTo("account_suspended");
    }
}
