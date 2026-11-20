package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.billing.WebhookService;
import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "carteirama.payment.provider", havingValue = "abacatepay")
@EnableConfigurationProperties(AbacatePayProperties.class)
public class AbacatePayConfig {

    static final int MIN_WEBHOOK_SECRET_LENGTH = 32;

    @Bean
    AbacatePayClient abacatePayClient(AbacatePayProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("PAYMENT_PROVIDER=abacatepay exige ABACATEPAY_API_KEY.");
        }
        if (properties.webhookSecret() == null || properties.webhookSecret().strip().length() < MIN_WEBHOOK_SECRET_LENGTH) {
            throw new IllegalStateException("PAYMENT_PROVIDER=abacatepay exige ABACATEPAY_WEBHOOK_SECRET com "
                    + MIN_WEBHOOK_SECRET_LENGTH + " caracteres ou mais.");
        }
        log.info("Pagamentos pela AbacatePay ({})", properties.baseUrl());
        return new AbacatePayClient(properties.baseUrl(), properties.apiKey().strip());
    }

    @Bean
    AbacatePayPixStore abacatePayPixStore(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        return new AbacatePayPixStore(jdbc, transactionManager);
    }

    @Bean
    AbacatePayInvoices abacatePayInvoices(WebhookService webhookService, InvoiceRepository invoices) {
        return new AbacatePayInvoices(webhookService, invoices);
    }

    @Bean
    @Primary
    AbacatePayPaymentGateway abacatePayPaymentGateway(AbacatePayClient client, AbacatePayPixStore pixStore,
                                                      AbacatePayInvoices abacatePayInvoices, InvoiceRepository invoices,
                                                      AbacatePayProperties properties, BusinessClock clock,
                                                      CarteiramaProperties carteirama) {
        return new AbacatePayPaymentGateway(client, pixStore, abacatePayInvoices, invoices, properties, clock,
                carteirama.frontendUrl());
    }

    @Bean
    AbacatePayBillingJobs abacatePayBillingJobs(JdbcTemplate jdbc, AbacatePayInvoices abacatePayInvoices,
                                                AbacatePayProperties properties, BusinessClock clock) {
        return new AbacatePayBillingJobs(jdbc, abacatePayInvoices, properties, clock);
    }
}
