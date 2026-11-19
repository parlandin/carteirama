package com.parlandim.carteirama.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("carteirama")
public record CarteiramaProperties(String frontendUrl, int suspendAfterDays, int retentionDays) {
}
