package com.parlandim.carteirama.client;

import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientExtraTest extends IntegrationTest {

    private static final YearMonth CURRENT = YearMonth.now(BusinessClock.ZONE);
    private static final String MONTH = CURRENT.toString();
    private static final String NEXT = CURRENT.plusMonths(1).toString();

    private String client(Api api, int amount) {
        String g = api.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        return api.post("/api/v1/clients", Api.body("name", "Ana", "group_id", g, "amount_cents", amount,
                "due_day", 28)).json().path("id").asString();
    }

    private String extra(Api api, String client, String kind, String description, int amount, String month) {
        var res = api.post("/api/v1/clients/" + client + "/extras", Api.body("kind", kind,
                "description", description, "amount_cents", amount, "month", month));
        assertThat(res.status()).as(res.raw()).isEqualTo(201);
        return res.json().path("id").asString();
    }

    private JsonNode charge(Api api, String month) {
        return api.get("/api/v1/charges?month=" + month).json().path("data").get(0);
    }

    @Test
    void bonusSomaNoCobradoECustoSaiDoLiquido() {
        Api api = registered();
        String ana = client(api, 20000);
        api.post("/api/v1/charges/generate", Map.of("month", MONTH));

        extra(api, ana, "cost", "Hospedagem", 3000, null);
        extra(api, ana, "bonus", "Fio 2,5 mm", 4500, MONTH);
        extra(api, ana, "bonus", "Ferramenta", 1000, MONTH);
        extra(api, ana, "bonus", "Material do mês que vem", 700, NEXT);

        var atual = charge(api, MONTH);
        assertThat(atual.path("amount_cents").asInt()).isEqualTo(20000);
        assertThat(atual.path("bonus_cents").asLong()).isEqualTo(5500);
        assertThat(atual.path("total_cents").asLong()).isEqualTo(25500);
        assertThat(atual.path("cost_cents").asLong()).isEqualTo(3000);
        assertThat(atual.path("net_cents").asLong()).isEqualTo(22500);

        var resumo = api.get("/api/v1/summary?month=" + MONTH).json();
        assertThat(resumo.path("total_cents").asLong()).isEqualTo(25500);
        assertThat(resumo.path("cost_cents").asLong()).isEqualTo(3000);
        assertThat(resumo.path("net_cents").asLong()).isEqualTo(22500);
        assertThat(api.get("/api/v1/groups?month=" + MONTH).json().path("data").get(0)
                .path("month_total_cents").asLong()).isEqualTo(25500);

        // No mês seguinte: o custo recorrente continua, os bônus de um mês não, e entra o daquele mês.
        api.post("/api/v1/charges/generate", Map.of("month", NEXT));
        var proximo = charge(api, NEXT);
        assertThat(proximo.path("bonus_cents").asLong()).isEqualTo(700);
        assertThat(proximo.path("cost_cents").asLong()).isEqualTo(3000);
        assertThat(proximo.path("net_cents").asLong()).isEqualTo(17700);

        var detalhe = api.get("/api/v1/clients/" + ana).json();
        assertThat(detalhe.path("extras")).hasSize(4);
        assertThat(detalhe.path("extras").get(0).path("recurring").asBoolean()).isTrue();
        assertThat(detalhe.path("extras").get(0).path("start_month").asString()).isEqualTo(MONTH + "-01");
        assertThat(detalhe.path("charges").get(0).path("total_cents").asLong()).isEqualTo(20700);

        // Cobrança avulsa não recebe bônus nem custos.
        var avulsa = api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "one_off", "description", "Visita",
                "amount_cents", 5000, "due_date", CURRENT.atDay(15).toString())).json();
        assertThat(avulsa.path("bonus_cents").asLong()).isZero();
        assertThat(avulsa.path("total_cents").asLong()).isEqualTo(5000);
    }

    @Test
    void removerRecorrentePreservaMesesAnteriores() {
        Api api = registered();
        String ana = client(api, 10000);
        String prev = CURRENT.minusMonths(1).toString();
        api.post("/api/v1/charges/generate", Map.of("month", prev));
        api.post("/api/v1/charges/generate", Map.of("month", MONTH));

        String hospedagem = extra(api, ana, "cost", "Hospedagem", 2000, null);
        String novo = extra(api, ana, "cost", "Backup", 500, null);
        jdbc.update("UPDATE client_extras SET start_month = ? WHERE id = ?::uuid",
                CURRENT.minusMonths(1).atDay(1), hospedagem);
        assertThat(charge(api, prev).path("cost_cents").asLong()).isEqualTo(2000);
        assertThat(charge(api, MONTH).path("cost_cents").asLong()).isEqualTo(2500);

        assertThat(api.delete("/api/v1/clients/" + ana + "/extras/" + hospedagem).status()).isEqualTo(204);
        assertThat(api.delete("/api/v1/clients/" + ana + "/extras/" + novo).status()).isEqualTo(204);

        assertThat(charge(api, prev).path("cost_cents").asLong()).isEqualTo(2000);
        assertThat(charge(api, MONTH).path("cost_cents").asLong()).isZero();
        assertThat(api.get("/api/v1/clients/" + ana).json().path("extras")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM client_extras", Long.class)).isEqualTo(1);
        assertThat(api.delete("/api/v1/clients/" + ana + "/extras/" + novo).status()).isEqualTo(404);
    }

    @Test
    void bonusNaoMudaMensalidadeJaPaga() {
        Api api = registered();
        String ana = client(api, 10000);
        api.post("/api/v1/charges/generate", Map.of("month", MONTH));
        String bonusDoMes = extra(api, ana, "bonus", "Fio", 500, MONTH);
        String id = charge(api, MONTH).path("id").asString();
        api.post("/api/v1/charges/" + id + "/pay", Map.of());

        var bloqueado = api.post("/api/v1/clients/" + ana + "/extras", Api.body("kind", "bonus",
                "description", "Outro", "amount_cents", 100, "month", MONTH));
        assertThat(bloqueado.code()).isEqualTo("charge_already_paid");
        assertThat(api.delete("/api/v1/clients/" + ana + "/extras/" + bonusDoMes).code())
                .isEqualTo("charge_already_paid");

        // Bônus recorrente começa no mês seguinte; custo pode entrar no mês pago.
        var recorrente = api.post("/api/v1/clients/" + ana + "/extras", Api.body("kind", "bonus",
                "description", "Suporte", "amount_cents", 1000)).json();
        assertThat(recorrente.path("start_month").asString()).isEqualTo(NEXT + "-01");
        extra(api, ana, "cost", "Material", 300, MONTH);

        var pago = charge(api, MONTH);
        assertThat(pago.path("total_cents").asLong()).isEqualTo(10500);
        assertThat(pago.path("cost_cents").asLong()).isEqualTo(300);
    }

    @Test
    void validacaoEIsolamento() {
        Api a = registered();
        Api b = registered();
        String ana = client(a, 10000);
        String path = "/api/v1/clients/" + ana + "/extras";

        assertThat(a.post(path, Api.body("kind", "brinde", "description", "x", "amount_cents", 1)).details())
                .containsExactly("kind:invalid_value");
        assertThat(a.post(path, Api.body("kind", "bonus", "description", "  ", "amount_cents", 0)).details())
                .containsExactlyInAnyOrder("description:required", "amount_cents:out_of_range");
        assertThat(a.post(path, Api.body("kind", "bonus", "description", "x", "amount_cents", 1,
                "month", CURRENT.plusMonths(13).toString())).details()).containsExactly("month:out_of_range");

        String id = extra(a, ana, "bonus", "Fio", 100, null);
        assertThat(b.post(path, Api.body("kind", "bonus", "description", "x", "amount_cents", 1)).status()).isEqualTo(404);
        assertThat(b.delete(path + "/" + id).status()).isEqualTo(404);

        setAccountStatus(accountIdOf(a), "suspended");
        assertThat(a.post(path, Api.body("kind", "bonus", "description", "x", "amount_cents", 1)).code())
                .isEqualTo("account_suspended");
        assertThat(a.delete(path + "/" + id).code()).isEqualTo("account_suspended");

        setAccountStatus(accountIdOf(a), "active");
        a.delete("/api/v1/clients/" + ana);
        assertThat(a.post(path, Api.body("kind", "bonus", "description", "x", "amount_cents", 1)).code())
                .isEqualTo("client_not_active");
    }
}
