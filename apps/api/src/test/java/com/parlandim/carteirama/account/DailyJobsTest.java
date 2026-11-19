package com.parlandim.carteirama.account;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DailyJobsTest extends IntegrationTest {

    @Autowired
    DailyJobs jobs;

    private String statusOf(UUID accountId) {
        return jdbc.queryForObject("SELECT status::text FROM accounts WHERE id = ?", String.class, accountId);
    }

    private void invoice(UUID accountId, String dueDateExpr, String status) {
        jdbc.update("INSERT INTO invoices (account_id, amount_cents, due_date, status, paid_at) VALUES (?, 5900, "
                + dueDateExpr + ", ?::invoice_status, " + ("paid".equals(status) ? "now()" : "NULL") + ")",
                accountId, status);
    }

    @Test
    void suspendeTrialVencidoComAuditoriaEEmail() {
        Api vencido = registered();
        UUID a = accountIdOf(vencido);
        UUID b = accountIdOf(registered());
        jdbc.update("UPDATE accounts SET trial_ends_at = now() - interval '1 hour' WHERE id = ?", a);

        jobs.suspendExpiredTrials();

        assertThat(statusOf(a)).isEqualTo("suspended");
        assertThat(statusOf(b)).isEqualTo("trial");
        Map<String, Object> audit = jdbc.queryForMap(
                "SELECT action, actor_user_id, meta->>'reason' AS reason FROM audit_log WHERE account_id = ?", a);
        assertThat(audit).containsEntry("action", "account.suspend").containsEntry("reason", "trial_expired");
        assertThat(audit.get("actor_user_id")).isNull();
        assertThat(emails.sent()).hasSize(1);
        assertThat(emails.sent().get(0).subject()).contains("suspensa");

        assertThat(vencido.get("/api/v1/groups").status()).isEqualTo(200);
        assertThat(vencido.post("/api/v1/groups", Map.of("name", "X")).code()).isEqualTo("account_suspended");
    }

    @Test
    void suspendeContaAtivaComFaturaVencidaHaCincoDias() {
        UUID atrasada = accountIdOf(registered());
        UUID recente = accountIdOf(registered());
        UUID paga = accountIdOf(registered());
        for (UUID id : new UUID[]{atrasada, recente, paga}) {
            setAccountStatus(id, "active");
        }
        invoice(atrasada, "current_date - 5", "pending");
        invoice(recente, "current_date - 4", "pending");
        invoice(paga, "current_date - 30", "paid");

        jobs.suspendOverdueAccounts();

        assertThat(statusOf(atrasada)).isEqualTo("suspended");
        assertThat(statusOf(recente)).isEqualTo("active");
        assertThat(statusOf(paga)).isEqualTo("active");
    }

    @Test
    void avisaTresDiasAntesDoVencimento() {
        UUID a = accountIdOf(registered());
        invoice(a, "(now() AT TIME ZONE 'America/Sao_Paulo')::date + 3", "pending");
        invoice(a, "(now() AT TIME ZONE 'America/Sao_Paulo')::date + 4", "pending");

        jobs.sendDueReminders();

        assertThat(emails.sent()).hasSize(1);
        assertThat(emails.sent().get(0).subject()).contains("vence em 3 dias");
    }

    @Test
    void apagaContaCanceladaAposRetencaoMantendoAuditLog() {
        Api api = registered();
        UUID velha = accountIdOf(api);
        String g = api.post("/api/v1/groups", Map.of("name", "G")).json().path("id").asString();
        api.post("/api/v1/clients", Api.body("name", "Ana", "group_id", g, "amount_cents", 100, "due_day", 1));
        api.post("/api/v1/charges/generate", Map.of("month", "2026-12"));
        invoice(velha, "current_date", "pending");
        jdbc.update("INSERT INTO audit_log (account_id, action) VALUES (?, 'account.suspend')", velha);
        jdbc.update("UPDATE accounts SET status = 'cancelled', cancelled_at = now() - interval '91 days' WHERE id = ?", velha);

        UUID recente = accountIdOf(registered());
        jdbc.update("UPDATE accounts SET status = 'cancelled', cancelled_at = now() - interval '89 days' WHERE id = ?", recente);
        jdbc.update("INSERT INTO password_resets (user_id, token_hash, expires_at) "
                + "SELECT user_id, 'h', now() - interval '1 minute' FROM accounts WHERE id = ?", recente);

        jobs.cleanup();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM accounts WHERE id = ?", Integer.class, velha)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM charges", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE account_id IS NULL", Integer.class)).isEqualTo(1);
        assertThat(statusOf(recente)).isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM password_resets", Integer.class)).isZero();
        assertThat(api.get("/api/v1/me").status()).isEqualTo(401);
    }
}
