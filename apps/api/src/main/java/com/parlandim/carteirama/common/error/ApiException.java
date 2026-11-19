package com.parlandim.carteirama.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "not_found", "Recurso não encontrado.");
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.");
    }

    public static ApiException accountSuspended() {
        return new ApiException(HttpStatus.FORBIDDEN, "account_suspended",
                "Sua conta está suspensa. Regularize a fatura para voltar a editar.");
    }

    public static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "E-mail ou senha inválidos.");
    }
}
