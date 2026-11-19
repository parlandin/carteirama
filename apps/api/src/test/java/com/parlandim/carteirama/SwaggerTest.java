package com.parlandim.carteirama;

import com.parlandim.carteirama.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerTest extends IntegrationTest {

    @Autowired
    org.springframework.core.env.Environment env;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + env.getProperty("local.server.port") + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void documentacaoPublicaSobApi() throws Exception {
        var ui = get("/api/swagger");
        assertThat(ui.uri().getPath()).isEqualTo("/api/swagger-ui/index.html");
        assertThat(ui.statusCode()).isEqualTo(200);
        assertThat(ui.body()).contains("swagger-ui");

        var docs = get("/api/docs");
        assertThat(docs.statusCode()).isEqualTo(200);
        assertThat(docs.body()).contains("person_type").doesNotContain("personType");
        assertThat(docs.body()).contains("/api/v1/auth/register");
    }
}
