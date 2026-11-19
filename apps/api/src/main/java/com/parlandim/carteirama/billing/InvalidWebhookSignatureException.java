package com.parlandim.carteirama.billing;

import com.parlandim.carteirama.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidWebhookSignatureException extends ApiException {

    public InvalidWebhookSignatureException() {
        super(HttpStatus.UNAUTHORIZED, "invalid_webhook_signature", "Assinatura do webhook inválida.");
    }
}
