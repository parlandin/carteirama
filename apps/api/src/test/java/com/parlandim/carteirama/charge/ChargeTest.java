package com.parlandim.carteirama.charge;

import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeTest extends IntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(BusinessClock.ZONE);
    private static final String MONTH = YearMonth.from(TODAY).toString();

    private String group(Api api, String name) {
        return api.post("/api/v1/groups", Map.of("name", name)).json().path("id").asString();
    }

    private String client(Api api, String groupId, String name, int amount, int dueDay) {
        return api.post("/api/v1/clients", Api.body("name", name, "group_id", groupId, "amount_cents", amount,
                "due_day", dueDay)).json().path("id").asString();
    }

    @Test
    void gerarMesNaoDuplicaECopiaValorDoCliente() {
        Api api = registered();
        String g = group(api, "G");
        String ana = client(api, g, "Ana", 5000, 10);
        client(api, g, "Bia", 3000, 28);
        String pausado = client(api, g, "Caio", 1000, 5);
        api.patch("/api/v1/clients/" + pausado, Map.of("status", "paused"));

        var gen = api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));
        assertThat(gen.status()).isEqualTo(200);
        assertThat(gen.json().path("created").asInt()).isEqualTo(2);
        assertThat(api.post("/api/v1/charges/generate", Map.of("month", "2026-12")).json().path("created").asInt()).isZero();

        api.patch("/api/v1/clients/" + ana, Map.of("amount_cents", 9999));

        var list = api.get("/api/v1/charges?month=2026-12");
        assertThat(list.json().path("meta").path("total").asLong()).isEqualTo(2);
        var first = list.json().path("data").get(0);
        assertThat(first.path("client_name").asString()).isEqualTo("Ana");
        assertThat(first.path("amount_cents").asInt()).isEqualTo(5000);
        assertThat(first.path("due_date").asString()).isEqualTo("2026-12-10");
        assertThat(first.path("reference_month").asString()).isEqualTo("2026-12-01");
        assertThat(list.json().path("data").get(1).path("due_date").asString()).isEqualTo("2026-12-28");

        var groups = api.get("/api/v1/groups?month=2026-12").json().path("data").get(0);
        assertThat(groups.path("client_count").asLong()).isEqualTo(3);
        assertThat(groups.path("month_total_cents").asLong()).isEqualTo(8000);

        assertThat(api.post("/api/v1/charges/generate", Map.of("month", "2026/12")).details())
                .containsExactly("month:invalid_format");
    }

    @Test
    void atrasoCalculadoFiltrosEResumo() {
        Api api = registered();
        String g = group(api, "G");
        String ana = client(api, g, "Ana", 1000, 10);
        String bia = client(api, g, "Bia", 2000, 10);
        String caio = client(api, g, "Caio", 4000, 10);

        LocalDate past = TODAY.withDayOfMonth(1).equals(TODAY) ? TODAY : TODAY.minusDays(1);
        LocalDate future = TODAY.plusDays(1).getMonth() == TODAY.getMonth() ? TODAY.plusDays(1) : TODAY;
        String lateId = api.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 1000,
                "due_date", past.toString())).json().path("id").asString();
        api.post("/api/v1/charges", Map.of("client_id", bia, "amount_cents", 2000, "due_date", future.toString()));
        String paidId = api.post("/api/v1/charges", Map.of("client_id", caio, "amount_cents", 4000,
                "due_date", future.toString())).json().path("id").asString();
        api.post("/api/v1/charges/" + paidId + "/pay", Map.of());

        if (past.isBefore(TODAY)) {
            var late = api.get("/api/v1/charges?status=late");
            assertThat(late.json().path("data")).hasSize(1);
            assertThat(late.json().path("data").get(0).path("id").asString()).isEqualTo(lateId);
            assertThat(late.json().path("data").get(0).path("status").asString()).isEqualTo("pending");
            assertThat(late.json().path("data").get(0).path("display_status").asString()).isEqualTo("late");

            var summary = api.get("/api/v1/summary?month=" + MONTH).json();
            assertThat(summary.path("total_cents").asLong()).isEqualTo(7000);
            assertThat(summary.path("paid_cents").asLong()).isEqualTo(4000);
            assertThat(summary.path("pending_cents").asLong()).isEqualTo(2000);
            assertThat(summary.path("late_cents").asLong()).isEqualTo(1000);
        }
        assertThat(api.get("/api/v1/charges?status=paid").json().path("data")).hasSize(1);
        assertThat(api.get("/api/v1/charges?client_id=" + bia).json().path("data")).hasSize(1);
        assertThat(api.get("/api/v1/charges?status=foo").details()).containsExactly("status:invalid_value");
    }

    @Test
    void pagarDesfazerEditarECancelar() {
        Api api = registered();
        String ana = client(api, group(api, "G"), "Ana", 1000, 10);
        String id = api.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 1500,
                "due_date", TODAY.toString(), "note", "avulsa")).json().path("id").asString();

        var dup = api.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 1500, "due_date", TODAY.toString()));
        assertThat(dup.code()).isEqualTo("charge_already_exists");

        var edit = api.patch("/api/v1/charges/" + id, Api.body("amount_cents", 2500, "note", null));
        assertThat(edit.json().path("amount_cents").asInt()).isEqualTo(2500);
        assertThat(edit.json().path("note").isNull()).isTrue();

        var futuro = api.post("/api/v1/charges/" + id + "/pay", Map.of("paid_at", TODAY.plusDays(1).toString()));
        assertThat(futuro.details()).containsExactly("paid_at:out_of_range");

        var paid = api.post("/api/v1/charges/" + id + "/pay", Map.of("paid_at", TODAY.minusDays(2).toString()));
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.json().path("status").asString()).isEqualTo("paid");
        assertThat(paid.json().path("paid_at").asString()).isEqualTo(TODAY.minusDays(2).toString());

        assertThat(api.post("/api/v1/charges/" + id + "/pay").code()).isEqualTo("charge_already_paid");
        assertThat(api.patch("/api/v1/charges/" + id, Map.of("amount_cents", 1)).code()).isEqualTo("charge_not_editable");
        assertThat(api.delete("/api/v1/charges/" + id).code()).isEqualTo("charge_not_editable");

        var unpaid = api.post("/api/v1/charges/" + id + "/unpay");
        assertThat(unpaid.json().path("status").asString()).isEqualTo("pending");
        assertThat(unpaid.json().path("paid_at").isNull()).isTrue();
        assertThat(api.post("/api/v1/charges/" + id + "/unpay").code()).isEqualTo("charge_not_editable");

        assertThat(api.delete("/api/v1/charges/" + id).status()).isEqualTo(204);
        assertThat(api.post("/api/v1/charges/" + id + "/pay").code()).isEqualTo("charge_not_editable");
    }

    @Test
    void cobrancaAvulsaExigeClienteAtivoEArquivarCancelaPendentes() {
        Api api = registered();
        String ana = client(api, group(api, "G"), "Ana", 1000, 10);
        api.post("/api/v1/charges/generate", Map.of("month", MONTH));

        api.patch("/api/v1/clients/" + ana, Map.of("status", "paused"));
        var res = api.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 100,
                "due_date", TODAY.plusMonths(1).toString()));
        assertThat(res.status()).isEqualTo(409);
        assertThat(res.code()).isEqualTo("client_not_active");

        api.delete("/api/v1/clients/" + ana);
        var charges = api.get("/api/v1/charges?month=" + MONTH).json().path("data");
        assertThat(charges.get(0).path("status").asString()).isEqualTo("cancelled");
    }

    @Test
    void cobrancaAvulsaConviveComAMensalidadeDoMes() {
        Api api = registered();
        String g = group(api, "G");
        String ana = client(api, g, "Ana", 5000, 10);
        api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));

        var semDescricao = api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "one_off",
                "amount_cents", 100, "due_date", "2026-12-15"));
        assertThat(semDescricao.details()).containsExactly("description:required");
        assertThat(api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "outro", "amount_cents", 100,
                "due_date", "2026-12-15")).details()).containsExactly("kind:invalid_value");

        var avulsa = api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "one_off",
                "description", "  Instalação ", "amount_cents", 15000, "due_date", "2026-12-15"));
        assertThat(avulsa.status()).isEqualTo(201);
        assertThat(avulsa.json().path("kind").asString()).isEqualTo("one_off");
        assertThat(avulsa.json().path("description").asString()).isEqualTo("Instalação");
        assertThat(avulsa.json().path("reference_month").asString()).isEqualTo("2026-12-01");
        String id = avulsa.json().path("id").asString();

        // Mais de uma avulsa no mesmo mês é permitido; a mensalidade continua única.
        assertThat(api.post("/api/v1/charges", Api.body("client_id", ana, "kind", "one_off",
                "description", "Visita técnica", "amount_cents", 8000, "due_date", "2026-12-20")).status()).isEqualTo(201);
        assertThat(api.post("/api/v1/charges", Api.body("client_id", ana, "amount_cents", 5000,
                "due_date", "2026-12-10")).code()).isEqualTo("charge_already_exists");
        assertThat(api.post("/api/v1/charges/generate", Map.of("month", "2026-12")).json().path("created").asInt()).isZero();

        var list = api.get("/api/v1/charges?month=2026-12").json().path("data");
        assertThat(list).hasSize(3);
        assertThat(list.get(0).path("kind").asString()).isEqualTo("recurring");
        assertThat(list.get(0).path("description").isNull()).isTrue();

        var resumo = api.get("/api/v1/summary?month=2026-12").json();
        assertThat(resumo.path("total_cents").asLong()).isEqualTo(28000);
        assertThat(api.get("/api/v1/groups?month=2026-12").json().path("data").get(0)
                .path("month_total_cents").asLong()).isEqualTo(28000);

        var edit = api.patch("/api/v1/charges/" + id, Map.of("description", "Instalação e treinamento"));
        assertThat(edit.json().path("description").asString()).isEqualTo("Instalação e treinamento");
        assertThat(api.patch("/api/v1/charges/" + id, Map.of("description", "  ")).details())
                .containsExactly("description:required");
        String mensalidade = list.get(0).path("id").asString();
        assertThat(api.patch("/api/v1/charges/" + mensalidade, Map.of("description", "x")).details())
                .containsExactly("description:not_allowed");

        var detalhe = api.get("/api/v1/clients/" + ana).json().path("charges");
        assertThat(detalhe).hasSize(3);
    }

    @Test
    void isolamentoEContaSuspensa() {
        Api a = registered();
        Api b = registered();
        String ana = client(a, group(a, "G"), "Ana", 1000, 10);
        String id = a.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 100,
                "due_date", TODAY.toString())).json().path("id").asString();

        assertThat(b.post("/api/v1/charges", Map.of("client_id", ana, "amount_cents", 100,
                "due_date", TODAY.toString())).status()).isEqualTo(404);
        assertThat(b.patch("/api/v1/charges/" + id, Map.of("amount_cents", 1)).status()).isEqualTo(404);
        assertThat(b.post("/api/v1/charges/" + id + "/pay").status()).isEqualTo(404);
        assertThat(b.post("/api/v1/charges/" + id + "/unpay").status()).isEqualTo(404);
        assertThat(b.delete("/api/v1/charges/" + id).status()).isEqualTo(404);
        assertThat(b.get("/api/v1/charges?month=" + MONTH).json().path("data")).isEmpty();
        assertThat(b.post("/api/v1/charges/generate", Map.of("month", MONTH)).json().path("created").asInt()).isZero();
        assertThat(b.get("/api/v1/summary").json().path("total_cents").asLong()).isZero();

        setAccountStatus(accountIdOf(a), "suspended");
        assertThat(a.get("/api/v1/charges?month=" + MONTH).status()).isEqualTo(200);
        assertThat(a.get("/api/v1/summary").status()).isEqualTo(200);
        assertThat(a.post("/api/v1/charges/generate", Map.of("month", MONTH)).code()).isEqualTo("account_suspended");
        assertThat(a.post("/api/v1/charges/" + id + "/pay").code()).isEqualTo("account_suspended");
        assertThat(a.delete("/api/v1/charges/" + id).code()).isEqualTo("account_suspended");
    }
}
