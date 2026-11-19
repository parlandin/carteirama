package com.parlandim.carteirama.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentGateway gateway;
    private final WebhookService webhookService;

    public record WebhookResponse(boolean received, boolean duplicate) {
    }

    @PostMapping("/gateway")
    public WebhookResponse receive(@RequestHeader HttpHeaders headers, @RequestBody String body) {
        Map<String, String> flat = new HashMap<>();
        headers.forEach((k, v) -> flat.put(k.toLowerCase(Locale.ROOT), v.isEmpty() ? "" : v.get(0)));
        GatewayEvent event = gateway.parseWebhook(flat, body);
        try {
            return new WebhookResponse(true, !webhookService.process(event));
        } catch (DataIntegrityViolationException e) {
            return new WebhookResponse(true, true);
        }
    }
}
