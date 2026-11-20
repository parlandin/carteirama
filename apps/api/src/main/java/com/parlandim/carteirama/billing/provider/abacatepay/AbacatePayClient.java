package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.common.error.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class AbacatePayClient {

    public record Pix(String id, String status, String brCode, Instant expiresAt) {
    }

    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public AbacatePayClient(String baseUrl, String apiKey) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.USER_AGENT, "carteirama-api")
                .build();
    }

    public Pix createPix(int amountCents, Duration expiresIn, String description, String externalId,
                         Map<String, String> metadata) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", amountCents);
        data.put("expiresIn", expiresIn.toSeconds());
        data.put("description", description);
        data.put("externalId", externalId);
        data.put("metadata", metadata);
        String body = json.writeValueAsString(Map.of("method", "PIX", "data", data));
        JsonNode pix = call("criar o Pix", () -> http.post()
                .uri("/transparents/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class));
        return new Pix(pix.path("id").asString(), pix.path("status").asString(), pix.path("brCode").asString(),
                Instant.parse(pix.path("expiresAt").asString()));
    }

    public String pixStatus(String pixId) {
        JsonNode pix = call("consultar o Pix", () -> http.get()
                .uri(b -> b.path("/transparents/check").queryParam("id", pixId).build())
                .retrieve()
                .body(String.class));
        return pix.path("status").asString();
    }

    private JsonNode call(String what, Supplier<String> request) {
        try {
            JsonNode data = json.readTree(request.get()).path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw failure();
            }
            return data;
        } catch (RestClientResponseException e) {
            log.error("AbacatePay recusou {}: HTTP {} {}", what, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw failure();
        } catch (ResourceAccessException | JacksonException e) {
            log.error("Falha ao {} na AbacatePay", what, e);
            throw failure();
        }
    }

    private static ApiException failure() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "gateway_error",
                "Não foi possível falar com a AbacatePay agora. Tente novamente em instantes.");
    }
}
