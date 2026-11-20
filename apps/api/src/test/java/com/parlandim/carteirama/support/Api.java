package com.parlandim.carteirama.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class Api {

    private final String baseUrl;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    public Api(String baseUrl, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .build();
    }

    public record Res(int status, JsonNode json, HttpHeaders headers, String raw) {
        public String code() {
            return json.path("error").path("code").asString();
        }

        public java.util.List<String> details() {
            java.util.List<String> out = new java.util.ArrayList<>();
            json.path("error").path("details").forEach(d -> out.add(d.path("field").asString() + ":" + d.path("code").asString()));
            return out;
        }
    }

    public Res get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    public Res post(String path) {
        return post(path, Map.of());
    }

    public Res post(String path, Object body) {
        return send(HttpRequest.newBuilder(uri(path)).POST(json(body)));
    }

    public Res postWithoutCsrf(String path, Object body) {
        return exchange(HttpRequest.newBuilder(uri(path)).POST(json(body)));
    }

    public Res postWithoutCsrf(String path, Object body, String origin) {
        return exchange(HttpRequest.newBuilder(uri(path)).header("Origin", origin).POST(json(body)));
    }

    public Res preflight(String path, String origin, String method) {
        return exchange(HttpRequest.newBuilder(uri(path))
                .header("Origin", origin)
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
    }

    public Res postWithOrigin(String path, Object body, String origin) {
        return send(HttpRequest.newBuilder(uri(path)).header("Origin", origin).POST(json(body)));
    }

    public Res postSigned(String path, Object body, String signature) {
        return send(HttpRequest.newBuilder(uri(path)).header("X-Test-Signature", signature).POST(json(body)));
    }

    public Res postRaw(String path, String rawBody) {
        return send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.ofString(rawBody)));
    }

    public Res patch(String path, Object body) {
        return send(HttpRequest.newBuilder(uri(path)).method("PATCH", json(body)));
    }

    public Res delete(String path) {
        return send(HttpRequest.newBuilder(uri(path)).DELETE());
    }

    public static Map<String, Object> body(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private URI uri(String path) {
        return URI.create(baseUrl + path);
    }

    private HttpRequest.BodyPublisher json(Object body) {
        return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
    }

    private Res send(HttpRequest.Builder builder) {
        if (!"GET".equals(builder.copy().build().method())) {
            builder.header("X-XSRF-TOKEN", csrfToken());
        }
        return exchange(builder);
    }

    private String csrfToken() {
        String token = csrfCookie();
        if (token == null) {
            get("/api/v1/auth/csrf");
            token = csrfCookie();
        }
        if (token == null) {
            throw new IllegalStateException("Cookie XSRF-TOKEN não recebido");
        }
        return token;
    }

    private String csrfCookie() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private Res exchange(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> res = http.send(builder.header("Content-Type", "application/json").build(),
                    HttpResponse.BodyHandlers.ofString());
            String raw = res.body();
            JsonNode node = raw == null || raw.isBlank() || !raw.trim().startsWith("{") && !raw.trim().startsWith("[")
                    ? mapper.createObjectNode() : mapper.readTree(raw);
            return new Res(res.statusCode(), node, res.headers(), raw);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
