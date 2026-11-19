package com.parlandim.carteirama.auth;

import com.parlandim.carteirama.support.Api;
import com.parlandim.carteirama.support.Fixtures;
import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTest extends IntegrationTest {

    @Test
    void cadastroPfAbreSessaoEIniciaTrial() {
        Api api = api();
        var res = api.post("/api/v1/auth/register", Fixtures.registerPf("Novo@Example.com ", "529.982.247-25"));

        assertThat(res.status()).isEqualTo(201);
        assertThat(res.json().path("user").path("email").asString()).isEqualTo("novo@example.com");
        assertThat(res.json().path("user").path("is_admin").asBoolean()).isFalse();
        assertThat(res.json().path("account").path("document").asString()).isEqualTo("52998224725");
        assertThat(res.json().path("account").path("phone").asString()).isEqualTo("11988887777");
        assertThat(res.json().path("account").path("status").asString()).isEqualTo("trial");
        assertThat(res.json().path("account").path("legal_name").asString()).isEqualTo("Fulano de Tal");
        assertThat(res.raw()).doesNotContain("password");

        var me = api.get("/api/v1/me");
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.json().path("user").path("name").asString()).isEqualTo("Fulano de Tal");

        Map<String, Object> sub = jdbc.queryForMap("SELECT status::text AS status, price_cents, plan_code FROM subscriptions");
        assertThat(sub).containsEntry("status", "trialing").containsEntry("price_cents", 2499).containsEntry("plan_code", "cloud");
        assertThat(jdbc.queryForObject(
                "SELECT trial_ends_at::date - created_at::date FROM accounts", Integer.class)).isEqualTo(7);
    }

    @Test
    void cadastroPjExigeCnpjERazaoSocial() {
        Map<String, Object> body = Fixtures.registerPf(Fixtures.email(), Fixtures.cpf());
        body.put("person_type", "pj");
        var res = api().post("/api/v1/auth/register", body);
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.details()).contains("document:invalid_document", "company_name:required");

        body.put("document", Fixtures.cnpj());
        body.put("company_name", "ACME Ltda");
        var ok = api().post("/api/v1/auth/register", body);
        assertThat(ok.status()).isEqualTo(201);
        assertThat(ok.json().path("account").path("company_name").asString()).isEqualTo("ACME Ltda");
    }

    @Test
    void cadastroValidaCampos() {
        Map<String, Object> body = Api.body(
                "email", "invalido",
                "password", "abcdefgh",
                "name", "F",
                "person_type", "pf",
                "document", "111.111.111-11",
                "phone", "123",
                "state", "XX",
                "terms_accepted", false);
        var res = api().post("/api/v1/auth/register", body);

        assertThat(res.status()).isEqualTo(422);
        assertThat(res.code()).isEqualTo("validation_error");
        assertThat(res.json().path("error").path("request_id").asString()).isNotBlank();
        assertThat(res.details()).contains("email:invalid_format", "password:weak_password", "name:too_short",
                "phone:invalid_format", "state:invalid_format", "terms_accepted:required");
    }

    @Test
    void cpfComDigitosRepetidosEInvalido() {
        var res = api().post("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), "111.111.111-11"));
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.details()).containsExactly("document:invalid_document");
    }

    @Test
    void campoNaoPrevistoEJsonMalformado() {
        Map<String, Object> body = Fixtures.registerPf(Fixtures.email(), Fixtures.cpf());
        body.put("is_admin", true);
        var res = api().post("/api/v1/auth/register", body);
        assertThat(res.status()).isEqualTo(422);
        assertThat(res.details()).containsExactly("is_admin:unknown_field");

        var bad = api().postRaw("/api/v1/auth/login", "{nope");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.code()).isEqualTo("bad_request");

        var enumInvalido = api().post("/api/v1/auth/register", Map.of("person_type", "xx"));
        assertThat(enumInvalido.status()).isEqualTo(422);
        assertThat(enumInvalido.details()).containsExactly("person_type:invalid_value");
    }

    @Test
    void emailEDocumentoUnicos() {
        String email = Fixtures.email();
        String cpf = Fixtures.cpf();
        registered(email, cpf);

        var dupEmail = api().post("/api/v1/auth/register", Fixtures.registerPf(email.toUpperCase(), Fixtures.cpf()));
        assertThat(dupEmail.status()).isEqualTo(409);
        assertThat(dupEmail.code()).isEqualTo("email_taken");

        var dupDoc = api().post("/api/v1/auth/register", Fixtures.registerPf(Fixtures.email(), cpf));
        assertThat(dupDoc.status()).isEqualTo(409);
        assertThat(dupDoc.code()).isEqualTo("document_taken");
    }

    @Test
    void loginLogoutEBloqueioPorTentativas() {
        String email = Fixtures.email();
        registered(email, Fixtures.cpf());

        Api api = api();
        assertThat(api.get("/api/v1/me").code()).isEqualTo("not_authenticated");

        var ok = api.post("/api/v1/auth/login", Map.of("email", email, "password", Fixtures.PASSWORD, "remember", true));
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.headers().allValues("set-cookie")).anyMatch(c -> c.contains("HttpOnly") && c.contains("SameSite=Lax"));
        assertThat(api.get("/api/v1/me").status()).isEqualTo(200);

        assertThat(api.post("/api/v1/auth/logout").status()).isEqualTo(204);
        var after = api.get("/api/v1/me");
        assertThat(after.status()).isEqualTo(401);
        assertThat(after.code()).isEqualTo("not_authenticated");

        for (int i = 0; i < 5; i++) {
            var wrong = api.post("/api/v1/auth/login", Map.of("email", email, "password", "errada123"));
            assertThat(wrong.status()).isEqualTo(401);
            assertThat(wrong.code()).isEqualTo("invalid_credentials");
        }
        var blocked = api.post("/api/v1/auth/login", Map.of("email", email, "password", Fixtures.PASSWORD));
        assertThat(blocked.status()).isEqualTo(429);
        assertThat(blocked.code()).isEqualTo("too_many_requests");
        assertThat(blocked.headers().firstValue("retry-after")).isPresent();
    }

    @Test
    void emailInexistenteTemMesmaRespostaNoLogin() {
        var res = api().post("/api/v1/auth/login", Map.of("email", "ninguem@example.com", "password", "x1234567"));
        assertThat(res.status()).isEqualTo(401);
        assertThat(res.code()).isEqualTo("invalid_credentials");
    }

    @Test
    void recuperarSenha() {
        String email = Fixtures.email();
        Api antiga = registered(email, Fixtures.cpf());

        assertThat(api().post("/api/v1/auth/forgot-password", Map.of("email", "ninguem@example.com")).status()).isEqualTo(204);
        assertThat(emails.sent()).isEmpty();

        assertThat(api().post("/api/v1/auth/forgot-password", Map.of("email", email)).status()).isEqualTo(204);
        assertThat(emails.sent()).hasSize(1);
        Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(emails.sent().get(0).body());
        assertThat(m.find()).isTrue();
        String token = m.group(1);

        var reset = api().post("/api/v1/auth/reset-password", Map.of("token", token, "password", "novaSenha99"));
        assertThat(reset.status()).isEqualTo(204);
        assertThat(antiga.get("/api/v1/me").status()).isEqualTo(401);

        var reuse = api().post("/api/v1/auth/reset-password", Map.of("token", token, "password", "outraSenha99"));
        assertThat(reuse.status()).isEqualTo(422);
        assertThat(reuse.details()).containsExactly("token:invalid_token");

        assertThat(api().post("/api/v1/auth/login", Map.of("email", email, "password", "novaSenha99")).status()).isEqualTo(200);
    }

    @Test
    void editarPerfilETrocarSenha() {
        String email = Fixtures.email();
        Api api = registered(email, Fixtures.cpf());
        Api outraSessao = api();
        outraSessao.post("/api/v1/auth/login", Map.of("email", email, "password", Fixtures.PASSWORD));

        var patch = api.patch("/api/v1/me", Api.body("name", "  Beltrano  ", "zip", "01310-100", "state", "sp", "city", "São Paulo"));
        assertThat(patch.status()).isEqualTo(200);
        assertThat(patch.json().path("user").path("name").asString()).isEqualTo("Beltrano");
        assertThat(patch.json().path("account").path("zip").asString()).isEqualTo("01310100");
        assertThat(patch.json().path("account").path("state").asString()).isEqualTo("SP");

        var clear = api.patch("/api/v1/me", Api.body("city", null));
        assertThat(clear.json().path("account").path("city").isNull()).isTrue();
        assertThat(clear.json().path("account").path("state").asString()).isEqualTo("SP");

        var wrong = api.patch("/api/v1/me/password", Map.of("current_password", "errada123", "new_password", "outra1234"));
        assertThat(wrong.status()).isEqualTo(422);
        assertThat(wrong.details()).containsExactly("current_password:incorrect_password");

        var ok = api.patch("/api/v1/me/password", Map.of("current_password", Fixtures.PASSWORD, "new_password", "outra1234"));
        assertThat(ok.status()).isEqualTo(204);
        assertThat(api.get("/api/v1/me").status()).isEqualTo(200);
        assertThat(outraSessao.get("/api/v1/me").status()).isEqualTo(401);
    }

    @Test
    void rotaInexistenteERotaAdminSemPermissao() {
        Api api = registered();
        var notFound = api.get("/api/v1/nao-existe");
        assertThat(notFound.status()).isEqualTo(404);
        assertThat(notFound.code()).isEqualTo("not_found");

        var admin = api.get("/api/v1/admin/summary");
        assertThat(admin.status()).isEqualTo(403);
        assertThat(admin.code()).isEqualTo("forbidden");
        assertThat(admin.headers().firstValue("x-request-id")).isPresent();
    }

    @Test
    void rotaOuMetodoInexistenteNaoPareceFaltaDeLogin() {
        Api anon = api();
        assertThat(anon.get("/api/v1/auth/register").status()).isEqualTo(405);
        assertThat(anon.get("/api/v1/auth/register").code()).isEqualTo("method_not_allowed");
        assertThat(anon.get("/api/v1/nao-existe").code()).isEqualTo("not_found");
        assertThat(anon.get("/api/api/v1/me").status()).isEqualTo(404);
        assertThat(anon.get("/api/v1/me").code()).isEqualTo("not_authenticated");
        assertThat(anon.get("/api/v1/clients/" + java.util.UUID.randomUUID()).code()).isEqualTo("not_authenticated");
    }
}
