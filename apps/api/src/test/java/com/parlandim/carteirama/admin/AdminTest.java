package com.parlandim.carteirama.admin;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.Fixtures;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTest extends IntegrationTest {

    private Api admin() {
        String email = Fixtures.email();
        registered(email, Fixtures.cpf());
        jdbc.update("UPDATE users SET is_admin = true WHERE email = ?", email);
        Api api = api();
        api.post("/api/v1/auth/login", Map.of("email", email, "password", Fixtures.PASSWORD));
        return api;
    }

    private UUID invoice(UUID accountId, LocalDate due, String status) {
        return jdbc.queryForObject("INSERT INTO invoices (account_id, amount_cents, due_date, status, paid_at) "
                        + "VALUES (?, 5900, ?, ?::invoice_status, " + ("paid".equals(status) ? "now()" : "NULL") + ") RETURNING id",
                UUID.class, accountId, due, status);
    }

    @Test
    void usuarioComumNaoAcessa() {
        Api user = registered();
        assertThat(user.get("/api/v1/admin/accounts").code()).isEqualTo("forbidden");
        assertThat(user.post("/api/v1/admin/accounts/" + UUID.randomUUID() + "/suspend", Map.of("reason", "abc")).status())
                .isEqualTo(403);
        assertThat(api().get("/api/v1/admin/summary").code()).isEqualTo("not_authenticated");
        assertThat(admin().get("/api/v1/me").json().path("user").path("is_admin").asBoolean()).isTrue();
    }

    @Test
    void suspenderEReativarGravamAuditLog() {
        Api admin = admin();
        Api user = registered();
        UUID accountId = accountIdOf(user);

        var invalid = admin.post("/api/v1/admin/accounts/" + accountId + "/suspend", Map.of("reason", "x"));
        assertThat(invalid.details()).containsExactly("reason:too_short");

        var suspended = admin.post("/api/v1/admin/accounts/" + accountId + "/suspend", Map.of("reason", "Fraude no cadastro"));
        assertThat(suspended.status()).isEqualTo(200);
        assertThat(suspended.json().path("account").path("status").asString()).isEqualTo("suspended");
        assertThat(user.post("/api/v1/groups", Map.of("name", "G")).code()).isEqualTo("account_suspended");

        var again = admin.post("/api/v1/admin/accounts/" + accountId + "/suspend", Map.of("reason", "de novo"));
        assertThat(again.code()).isEqualTo("invalid_account_state");

        var reactivated = admin.post("/api/v1/admin/accounts/" + accountId + "/reactivate");
        assertThat(reactivated.json().path("account").path("status").asString()).isEqualTo("active");
        assertThat(admin.post("/api/v1/admin/accounts/" + accountId + "/reactivate").code()).isEqualTo("invalid_account_state");

        var log = reactivated.json().path("audit_log");
        assertThat(log).hasSize(2);
        assertThat(log.get(1).path("action").asString()).isEqualTo("account.suspend");
        assertThat(log.get(1).path("meta").path("reason").asString()).isEqualTo("Fraude no cadastro");
        assertThat(log.get(1).path("actor_name").asString()).isEqualTo("Fulano de Tal");
        assertThat(user.post("/api/v1/groups", Map.of("name", "G")).status()).isEqualTo(201);
    }

    @Test
    void adminNaoSuspendeAPropriaConta() {
        Api admin = admin();
        var res = admin.post("/api/v1/admin/accounts/" + accountIdOf(admin) + "/suspend", Map.of("reason", "teste"));
        assertThat(res.code()).isEqualTo("invalid_account_state");
    }

    @Test
    void listaContasComAtrasoCalculadoEBusca() {
        Api admin = admin();
        UUID atrasada = accountIdOf(registered("atrasada@example.com", Fixtures.cpf()));
        UUID emDia = accountIdOf(registered());
        setAccountStatus(atrasada, "active");
        setAccountStatus(emDia, "active");
        invoice(atrasada, LocalDate.now().minusDays(2), "pending");
        invoice(emDia, LocalDate.now().plusDays(5), "pending");

        var late = admin.get("/api/v1/admin/accounts?status=late");
        assertThat(late.json().path("data")).hasSize(1);
        assertThat(late.json().path("data").get(0).path("id").asString()).isEqualTo(atrasada.toString());
        assertThat(late.json().path("data").get(0).path("status").asString()).isEqualTo("active");
        assertThat(late.json().path("data").get(0).path("display_status").asString()).isEqualTo("late");

        assertThat(admin.get("/api/v1/admin/accounts?q=atrasada@").json().path("data")).hasSize(1);
        assertThat(admin.get("/api/v1/admin/accounts").json().path("meta").path("total").asLong()).isEqualTo(3);

        var summary = admin.get("/api/v1/admin/summary").json();
        assertThat(summary.path("accounts_by_status").path("active").asLong()).isEqualTo(2);
        assertThat(summary.path("accounts_by_status").path("trial").asLong()).isEqualTo(1);
        assertThat(summary.path("accounts_by_status").path("late").asLong()).isEqualTo(1);
        assertThat(summary.path("mrr_by_month")).hasSize(12);

        var detail = admin.get("/api/v1/admin/accounts/" + atrasada);
        assertThat(detail.json().path("invoices")).hasSize(1);
        assertThat(detail.json().path("subscription").path("price_cents").asInt()).isEqualTo(5900);
        assertThat(detail.raw()).doesNotContain("password").doesNotContain("gateway");
        assertThat(admin.get("/api/v1/admin/accounts/" + UUID.randomUUID()).status()).isEqualTo(404);
    }

    @Test
    void baixaManualDeFatura() {
        Api admin = admin();
        UUID accountId = accountIdOf(registered());
        UUID invoiceId = invoice(accountId, LocalDate.now(), "pending");

        var futuro = admin.post("/api/v1/admin/invoices/" + invoiceId + "/mark-paid",
                Map.of("paid_at", LocalDate.now().plusDays(2).toString(), "method", "pix"));
        assertThat(futuro.details()).containsExactly("paid_at:out_of_range");
        var metodo = admin.post("/api/v1/admin/invoices/" + invoiceId + "/mark-paid",
                Map.of("paid_at", LocalDate.now().toString(), "method", "cheque"));
        assertThat(metodo.details()).containsExactly("method:invalid_value");

        var paid = admin.post("/api/v1/admin/invoices/" + invoiceId + "/mark-paid",
                Map.of("paid_at", LocalDate.now().minusDays(1).toString(), "method", "boleto"));
        assertThat(paid.status()).isEqualTo(200);
        assertThat(paid.json().path("status").asString()).isEqualTo("paid");
        assertThat(paid.json().path("method").asString()).isEqualTo("boleto");

        var again = admin.post("/api/v1/admin/invoices/" + invoiceId + "/mark-paid",
                Map.of("paid_at", LocalDate.now().toString(), "method", "pix"));
        assertThat(again.code()).isEqualTo("invoice_not_payable");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'invoice.mark_paid'", Integer.class))
                .isEqualTo(1);

        assertThat(admin.get("/api/v1/admin/invoices?status=paid").json().path("data")).hasSize(1);
        assertThat(admin.get("/api/v1/admin/invoices?status=pending").json().path("data")).isEmpty();
    }

    @Test
    void baixaManualTemOsMesmosEfeitosDoWebhook() {
        Api admin = admin();
        LocalDate today = LocalDate.now(com.parlandim.carteirama.common.time.BusinessClock.ZONE);

        Api trial = registered();
        UUID trialId = accountIdOf(trial);
        admin.post("/api/v1/admin/invoices/" + invoice(trialId, today, "pending") + "/mark-paid",
                Map.of("paid_at", today.toString(), "method", "pix"));
        assertThat(trial.get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("active");
        assertThat(trial.get("/api/v1/subscription").json().path("status").asString()).isEqualTo("active");

        Api auto = registered();
        UUID autoId = accountIdOf(auto);
        setAccountStatus(autoId, "suspended");
        jdbc.update("INSERT INTO audit_log (account_id, action) VALUES (?, 'account.suspend')", autoId);
        admin.post("/api/v1/admin/invoices/" + invoice(autoId, today.minusDays(10), "pending") + "/mark-paid",
                Map.of("paid_at", today.toString(), "method", "boleto"));
        assertThat(auto.get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("active");

        Api manual = registered();
        UUID manualId = accountIdOf(manual);
        setAccountStatus(manualId, "active");
        admin.post("/api/v1/admin/accounts/" + manualId + "/suspend", Map.of("reason", "Chargeback"));
        admin.post("/api/v1/admin/invoices/" + invoice(manualId, today, "pending") + "/mark-paid",
                Map.of("paid_at", today.toString(), "method", "card"));
        assertThat(manual.get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("suspended");
    }

    @Test
    void mrrPorMesSomaFaturasNaoCanceladasPeloVencimento() {
        Api admin = admin();
        UUID a = accountIdOf(registered());
        java.time.YearMonth current = java.time.YearMonth.now(com.parlandim.carteirama.common.time.BusinessClock.ZONE);
        invoice(a, current.atDay(1), "paid");
        invoice(a, current.atDay(2), "pending");
        jdbc.update("INSERT INTO invoices (account_id, amount_cents, due_date, status) VALUES (?, 1000, ?, 'cancelled')",
                a, current.atDay(3));
        invoice(a, current.minusMonths(2).atDay(15), "pending");
        invoice(a, current.minusMonths(13).atDay(15), "paid");

        var months = admin.get("/api/v1/admin/summary").json().path("mrr_by_month");
        assertThat(months).hasSize(12);
        assertThat(months.get(11).path("month").asString()).isEqualTo(current.toString());
        assertThat(months.get(11).path("cents").asLong()).isEqualTo(11800);
        assertThat(months.get(9).path("month").asString()).isEqualTo(current.minusMonths(2).toString());
        assertThat(months.get(9).path("cents").asLong()).isEqualTo(5900);
        assertThat(months.get(10).path("cents").asLong()).isZero();
    }

    @Test
    void mudarPrecoValeSoParaNovasAssinaturas() {
        Api admin = admin();
        Api antigo = registered();

        assertThat(admin.patch("/api/v1/admin/plans/cloud", Map.of("price_cents", 50)).details())
                .containsExactly("price_cents:out_of_range");
        var updated = admin.patch("/api/v1/admin/plans/cloud", Map.of("price_cents", 7900));
        assertThat(updated.json().path("price_cents").asInt()).isEqualTo(7900);
        assertThat(admin.get("/api/v1/admin/plans").json().path("data").get(0).path("price_cents").asInt()).isEqualTo(7900);
        assertThat(admin.patch("/api/v1/admin/plans/anual", Map.of("price_cents", 7900)).status()).isEqualTo(404);

        Api novo = registered();
        assertThat(antigo.get("/api/v1/subscription").json().path("price_cents").asInt()).isEqualTo(5900);
        assertThat(novo.get("/api/v1/subscription").json().path("price_cents").asInt()).isEqualTo(7900);
        assertThat(jdbc.queryForObject("SELECT meta->>'from' FROM audit_log WHERE action = 'plan.price_change'", String.class))
                .isEqualTo("5900");
    }
}
