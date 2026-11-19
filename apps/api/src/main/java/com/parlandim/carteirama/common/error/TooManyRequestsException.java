package com.parlandim.carteirama.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TooManyRequestsException extends ApiException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "too_many_requests", "Muitas tentativas. Tente novamente mais tarde.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
