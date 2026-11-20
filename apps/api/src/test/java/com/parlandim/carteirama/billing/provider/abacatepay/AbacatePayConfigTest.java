package com.parlandim.carteirama.billing.provider.abacatepay;

import com.parlandim.carteirama.billing.PaymentGateway;
import com.parlandim.carteirama.billing.UnconfiguredPaymentGateway;
import com.parlandim.carteirama.billing.WebhookService;
import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.time.BusinessClock;
import com.parlandim.carteirama.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbacatePayConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AbacatePayConfig.class)
            .withBean(UnconfiguredPaymentGateway.class)
            .withBean(WebhookService.class, () -> mock(WebhookService.class))
            .withBean(InvoiceRepository.class, () -> mock(InvoiceRepository.class))
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withBean(BusinessClock.class, () -> new BusinessClock(Clock.systemUTC()))
            .withBean(CarteiramaProperties.class, () -> new CarteiramaProperties("http://localhost:8080", 5, 90));

    @Test
    void semProviderFicaOGatewayNaoConfigurado() {
        runner.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(AbacatePayPaymentGateway.class);
            assertThat(ctx.getBean(PaymentGateway.class)).isInstanceOf(UnconfiguredPaymentGateway.class);
        });
        runner.withPropertyValues("carteirama.payment.provider=none").run(ctx ->
                assertThat(ctx.getBean(PaymentGateway.class)).isInstanceOf(UnconfiguredPaymentGateway.class));
    }

    @Test
    void comProviderAbacatePayElaViraOGatewayPrincipal() {
        runner.withPropertyValues("carteirama.payment.provider=abacatepay",
                        "carteirama.payment.abacatepay.api-key=abc_dev_123",
                        "carteirama.payment.abacatepay.webhook-secret=segredo-do-webhook-com-mais-de-32-caracteres")
                .run(ctx -> {
                    assertThat(ctx.getBean(PaymentGateway.class)).isInstanceOf(AbacatePayPaymentGateway.class);
                    assertThat(ctx).hasSingleBean(AbacatePayBillingJobs.class);
                    var properties = ctx.getBean(AbacatePayProperties.class);
                    assertThat(properties.baseUrl()).isEqualTo("https://api.abacatepay.com/v2");
                    assertThat(properties.invoiceDaysBefore()).isEqualTo(5);
                });
    }

    @Test
    void semChaveOuSegredoAAplicacaoNaoSobe() {
        runner.withPropertyValues("carteirama.payment.provider=abacatepay",
                        "carteirama.payment.abacatepay.webhook-secret=segredo-do-webhook-com-mais-de-32-caracteres")
                .run(ctx -> assertThat(ctx).hasFailed());
        runner.withPropertyValues("carteirama.payment.provider=abacatepay",
                        "carteirama.payment.abacatepay.api-key=abc_dev_123",
                        "carteirama.payment.abacatepay.webhook-secret=curto")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
