package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.FakePaymentGateway;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest extends IntegrationTest {

    private record Subscriber(Api api, UUID accountId, String subscriptionId) {
    }

    private Subscriber subscriber() {
        Api api = registered();
        UUID accountId = accountIdOf(api);
        api.post("/api/v1/subscription/checkout");
        return new Subscriber(api, accountId, "sub_" + accountId);
    }

    private Api.Res webhook(String signature, Map<String, Object> event) throws Exception {
        Api api = api();
        return api.postSigned("/api/v1/webhooks/gateway", event, signature);
    }

    private Map<String, Object> event(String id, String type, Subscriber s, String chargeId) {
        return Api.body("id", id, "type", type, "subscription_id", s.subscriptionId(), "charge_id", chargeId,
                "amount_cents", 5900, "due_date", "2026-11-10", "method", "pix");
    }

    @Test
    void assinaturaEmTrialEChecktout() {
        Api api = registered();
        var sub = api.get("/api/v1/subscription");
        assertThat(sub.status()).isEqualTo(200);
        assertThat(sub.json().path("plan").path("code").asString()).isEqualTo("cloud");
        assertThat(sub.json().path("status").asString()).isEqualTo("trialing");
        assertThat(sub.json().path("price_cents").asInt()).isEqualTo(2499);
        assertThat(sub.json().path("account_status").asString()).isEqualTo("trial");
        assertThat(sub.json().path("usage").path("users").asInt()).isEqualTo(1);
        assertThat(sub.raw()).doesNotContain("gateway");

        var checkout = api.post("/api/v1/subscription/checkout");
        assertThat(checkout.status()).isEqualTo(200);
        assertThat(checkout.json().path("url").asString()).startsWith("https://pay.example/checkout/");
    }

    @Test
    void webhookPagamentoConfirmadoAtivaContaEEIdempotente() throws Exception {
        Subscriber s = subscriber();

        assertThat(webhook("errada", event("evt_1", "invoice.created", s, "ch_1")).code())
                .isEqualTo("invalid_webhook_signature");

        assertThat(webhook(FakePaymentGateway.SIGNATURE, event("evt_1", "invoice.created", s, "ch_1")).status()).isEqualTo(200);
        var invoices = s.api().get("/api/v1/subscription/invoices");
        assertThat(invoices.json().path("data")).hasSize(1);
        assertThat(invoices.json().path("data").get(0).path("number").asString()).startsWith("FAT-");
        assertThat(invoices.json().path("data").get(0).path("display_status").asString()).isIn("pending", "late");

        var first = webhook(FakePaymentGateway.SIGNATURE, event("evt_2", "payment.confirmed", s, "ch_1"));
        assertThat(first.json().path("duplicate").asBoolean()).isFalse();
        var repeated = webhook(FakePaymentGateway.SIGNATURE, event("evt_2", "payment.confirmed", s, "ch_1"));
        assertThat(repeated.status()).isEqualTo(200);
        assertThat(repeated.json().path("duplicate").asBoolean()).isTrue();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoices", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM webhook_events", Integer.class)).isEqualTo(2);
        var invoice = s.api().get("/api/v1/subscription/invoices").json().path("data").get(0);
        assertThat(invoice.path("status").asString()).isEqualTo("paid");
        assertThat(invoice.path("method").asString()).isEqualTo("pix");
        assertThat(s.api().get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("active");
        assertThat(s.api().get("/api/v1/subscription").json().path("status").asString()).isEqualTo("active");

        var link = s.api().post("/api/v1/subscription/invoices/" + invoice.path("id").asString() + "/payment-link");
        assertThat(link.code()).isEqualTo("invoice_not_payable");
    }

    @Test
    void pagamentoReativaSuspensaoAutomaticaMasNaoAManual() throws Exception {
        Subscriber auto = subscriber();
        jdbc.update("UPDATE accounts SET status = 'suspended' WHERE id = ?", auto.accountId());
        jdbc.update("INSERT INTO audit_log (account_id, action, meta) VALUES (?, 'account.suspend', '{}')", auto.accountId());
        webhook(FakePaymentGateway.SIGNATURE, event("evt_a", "payment.confirmed", auto, "ch_a"));
        assertThat(auto.api().get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("active");

        Subscriber manual = subscriber();
        UUID adminId = jdbc.queryForObject("SELECT user_id FROM accounts WHERE id = ?", UUID.class, manual.accountId());
        jdbc.update("UPDATE accounts SET status = 'suspended' WHERE id = ?", manual.accountId());
        jdbc.update("INSERT INTO audit_log (account_id, actor_user_id, action, meta) VALUES (?, ?, 'account.suspend', '{}')",
                manual.accountId(), adminId);
        webhook(FakePaymentGateway.SIGNATURE, event("evt_m", "payment.confirmed", manual, "ch_m"));
        assertThat(manual.api().get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("suspended");
    }

    @Test
    void linkDePagamentoECancelamento() throws Exception {
        Subscriber s = subscriber();
        webhook(FakePaymentGateway.SIGNATURE, event("evt_1", "invoice.created", s, "ch_1"));
        String invoiceId = s.api().get("/api/v1/subscription/invoices").json().path("data").get(0).path("id").asString();

        jdbc.update("UPDATE accounts SET status = 'suspended' WHERE id = ?", s.accountId());
        var link = s.api().post("/api/v1/subscription/invoices/" + invoiceId + "/payment-link");
        assertThat(link.status()).isEqualTo(200);
        assertThat(link.json().path("pix_code").asString()).startsWith("00020126pix");

        Api other = registered();
        assertThat(other.post("/api/v1/subscription/invoices/" + invoiceId + "/payment-link").status()).isEqualTo(404);
        assertThat(other.get("/api/v1/subscription/invoices").json().path("data")).isEmpty();

        var cancel = s.api().post("/api/v1/subscription/cancel");
        assertThat(cancel.status()).isEqualTo(200);
        assertThat(cancel.json().path("cancel_at_period_end").asBoolean()).isTrue();

        webhook(FakePaymentGateway.SIGNATURE, event("evt_c", "subscription.cancelled", s, null));
        assertThat(s.api().get("/api/v1/me").json().path("account").path("status").asString()).isEqualTo("cancelled");
        assertThat(s.api().post("/api/v1/subscription/cancel").code()).isEqualTo("invalid_account_state");
    }
}
