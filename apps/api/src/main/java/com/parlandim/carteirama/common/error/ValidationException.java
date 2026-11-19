package com.parlandim.carteirama.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationException extends ApiException {

    private final List<ErrorResponse.Detail> details;

    public ValidationException(List<ErrorResponse.Detail> details) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "validation_error", "Dados inválidos.");
        this.details = List.copyOf(details);
    }

    public static ValidationException of(String field, String code) {
        return new ValidationException(List.of(ErrorResponse.Detail.of(field, code)));
    }

    public static ValidationException of(String field, String code, String message) {
        return new ValidationException(List.of(new ErrorResponse.Detail(field, code, message)));
    }

    public static final class Collector {
        private final List<ErrorResponse.Detail> details = new ArrayList<>();

        public Collector add(String field, String code) {
            details.add(ErrorResponse.Detail.of(field, code));
            return this;
        }

        public Collector add(String field, String code, String message) {
            details.add(new ErrorResponse.Detail(field, code, message));
            return this;
        }

        public boolean has(String field) {
            return details.stream().anyMatch(d -> d.field().equals(field));
        }

        public void throwIfAny() {
            if (!details.isEmpty()) {
                throw new ValidationException(details);
            }
        }
    }
}
