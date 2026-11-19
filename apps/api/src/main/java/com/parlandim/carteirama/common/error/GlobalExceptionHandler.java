package com.parlandim.carteirama.common.error;

import com.parlandim.carteirama.common.web.RequestIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.InvalidNullException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, ApiException> UNIQUE_CONSTRAINTS = Map.of(
            "users_email_key", ApiException.conflict("email_taken", "Este e-mail já está em uso."),
            "accounts_document_key", ApiException.conflict("document_taken", "Este CPF/CNPJ já está cadastrado."),
            "groups_account_name_active_uq", ApiException.conflict("group_name_taken", "Já existe um grupo com este nome."),
            "charges_client_month_uq", ApiException.conflict("charge_already_exists", "Este cliente já tem cobrança neste mês.")
    );

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ResponseEntity<ErrorResponse> handleTooMany(TooManyRequestsException ex) {
        return ResponseEntity.status(ex.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null, RequestIdFilter.currentRequestId()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        Set<ErrorResponse.Detail> details = new LinkedHashSet<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String field = snakeCase(fe.getField());
            String code = codeOf(fe);
            details.add(ErrorResponse.Detail.of(field, code));
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> details.add(ErrorResponse.Detail.of(ge.getObjectName(), "invalid_value")));
        Set<String> requiredFields = details.stream().filter(d -> d.code().equals("required"))
                .map(ErrorResponse.Detail::field).collect(Collectors.toSet());
        details.removeIf(d -> requiredFields.contains(d.field()) && !d.code().equals("required"));
        return validation(new ArrayList<>(details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof JacksonException jex && !jex.getPath().isEmpty()) {
            String field = jex.getPath().stream()
                    .map(r -> r.getPropertyName() != null ? r.getPropertyName() : String.valueOf(r.getIndex()))
                    .collect(Collectors.joining("."));
            String code;
            if (jex instanceof UnrecognizedPropertyException) {
                code = "unknown_field";
            } else if (jex instanceof InvalidNullException) {
                code = "required";
            } else if (jex instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                code = "invalid_value";
            } else {
                code = "invalid_format";
            }
            return validation(List.of(ErrorResponse.Detail.of(field, code)));
        }
        return body(HttpStatus.BAD_REQUEST, "bad_request", "Requisição inválida.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> type = ex.getRequiredType();
        // Em parâmetros de lista (ex.: ?type=a&type=b), o que importa é o tipo do elemento
        Class<?> element = ex.getParameter().nested().getNestedParameterType();
        boolean isEnum = (type != null && type.isEnum()) || element.isEnum();
        String code = isEnum ? "invalid_value" : "invalid_format";
        return validation(List.of(ErrorResponse.Detail.of(ex.getName(), code)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return validation(List.of(ErrorResponse.Detail.of(ex.getParameterName(), "required")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return handleApi(ApiException.notFound());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "Método não permitido.", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", "Envie o corpo como application/json.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        String msg = String.valueOf(ex.getMostSpecificCause().getMessage());
        for (var entry : UNIQUE_CONSTRAINTS.entrySet()) {
            if (msg.contains(entry.getKey())) {
                return handleApi(entry.getValue());
            }
        }
        log.error("Violação de integridade não mapeada", ex);
        return body(HttpStatus.CONFLICT, "conflict", "Conflito com o estado atual dos dados.", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "Erro inesperado. Tente novamente.", null);
    }

    private static ResponseEntity<ErrorResponse> validation(List<ErrorResponse.Detail> details) {
        return body(HttpStatus.UNPROCESSABLE_CONTENT, "validation_error", "Dados inválidos.", details);
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                                      List<ErrorResponse.Detail> details) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(code, message, details, RequestIdFilter.currentRequestId()));
    }

    private static String codeOf(FieldError fe) {
        ConstraintViolation<?> cv;
        try {
            cv = fe.unwrap(ConstraintViolation.class);
        } catch (IllegalArgumentException e) {
            return fe.isBindingFailure() ? "invalid_format" : "invalid_value";
        }
        String template = cv.getMessageTemplate();
        if (template != null && !template.startsWith("{")) {
            return template;
        }
        Annotation a = cv.getConstraintDescriptor().getAnnotation();
        if (a instanceof NotNull || a instanceof NotBlank || a instanceof NotEmpty || a instanceof AssertTrue) {
            return "required";
        }
        if (a instanceof Size size) {
            Object v = cv.getInvalidValue();
            int len = v instanceof CharSequence cs ? cs.length() : v instanceof java.util.Collection<?> c ? c.size() : 0;
            return len < size.min() ? "too_short" : "too_long";
        }
        if (a instanceof Pattern || a instanceof Email) {
            return "invalid_format";
        }
        if (a instanceof Min || a instanceof Max || a instanceof DecimalMin || a instanceof DecimalMax || a instanceof Positive) {
            return "out_of_range";
        }
        return "invalid_value";
    }

    static String snakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
