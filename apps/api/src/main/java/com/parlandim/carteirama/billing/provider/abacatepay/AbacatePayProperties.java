package com.parlandim.carteirama.billing.provider.abacatepay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("carteirama.payment.abacatepay")
public record AbacatePayProperties(String apiKey,
                                   String webhookSecret,
                                   @DefaultValue("https://api.abacatepay.com/v2") String baseUrl,
                                   @DefaultValue("1h") Duration pixExpiresIn,
                                   @DefaultValue("5") int invoiceDaysBefore) {
}
