package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.model.Account;
import com.parlandim.carteirama.model.Invoice;
import com.parlandim.carteirama.model.Subscription;

import java.util.Map;

public interface PaymentGateway {

    String createCheckout(Account account, Subscription subscription);

    PaymentLink paymentLink(Invoice invoice);

    void cancelAtPeriodEnd(Subscription subscription);

    void resume(Subscription subscription);

    GatewayEvent parseWebhook(Map<String, String> headers, String body);

    record PaymentLink(String pixCode, String boletoUrl, String cardUrl) {
    }
}
