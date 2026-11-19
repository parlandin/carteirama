package com.parlandim.carteirama.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public record ErrorResponse(Body error) {
    public static ErrorResponse of(String code, String message, List<Detail> details, String requestId) {
        return new ErrorResponse(new Body(code, message, details == null || details.isEmpty() ? null : details, requestId));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, List<Detail> details, String requestId) {
    }

    public record Detail(String field, String code, String message) {
        public static Detail of(String field, String code) {
            return new Detail(field, code, defaultMessage(code));
        }
    }

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("required", "Campo obrigatório."),
            Map.entry("too_short", "Muito curto."),
            Map.entry("too_long", "Muito longo."),
            Map.entry("invalid_format", "Formato inválido."),
            Map.entry("invalid_value", "Valor inválido."),
            Map.entry("out_of_range", "Fora do intervalo permitido."),
            Map.entry("invalid_document", "Documento inválido."),
            Map.entry("weak_password", "A senha precisa ter ao menos 1 letra e 1 número e ser diferente do e-mail."),
            Map.entry("unknown_field", "Campo não permitido."),
            Map.entry("invalid_token", "Link inválido ou expirado."),
            Map.entry("incorrect_password", "Senha atual incorreta.")
    );

    public static String defaultMessage(String code) {
        return MESSAGES.getOrDefault(code, "Valor inválido.");
    }
}
