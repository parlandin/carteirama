package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.common.error.ApiException;
import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UnconfiguredPaymentGateway implements PaymentGateway {

    @Override
    public String createCheckout(Account account, Subscription subscription) {
        throw notConfigured();
    }

    @Override
    public PaymentLink paymentLink(Invoice invoice) {
        throw notConfigured();
    }

    @Override
    public void cancelAtPeriodEnd(Subscription subscription) {
    }

    @Override
    public void resume(Subscription subscription) {
    }

    @Override
    public GatewayEvent parseWebhook(Map<String, String> headers, String body) {
        throw new InvalidWebhookSignatureException();
    }

    private static ApiException notConfigured() {
        return new ApiException(HttpStatus.BAD_GATEWAY, "gateway_error", "Gateway de pagamento não configurado.");
    }
}
