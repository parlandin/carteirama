package com.parlandim.carteirama.common.web;

import com.parlandim.carteirama.common.error.ErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "request_id";
    public static final String HEADER = "X-Request-Id";
    public static final String ACCOUNT_ID_ATTR = RequestIdFilter.class.getName() + ".accountId";
    static final long MAX_BODY_BYTES = 100 * 1024;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ErrorWriter errorWriter;

    public static String currentRequestId() {
        return MDC.get(MDC_KEY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        String requestId = HexFormat.of().formatHex(bytes);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        long start = System.nanoTime();
        try {
            if (request.getContentLengthLong() > MAX_BODY_BYTES) {
                errorWriter.write(response, HttpStatus.BAD_REQUEST, "bad_request", "Corpo da requisição grande demais.");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                Object accountId = request.getAttribute(ACCOUNT_ID_ATTR);
                log.info("{} {} -> {} ({} ms) account_id={}", request.getMethod(), request.getRequestURI(),
                        response.getStatus(), ms, accountId == null ? "-" : accountId);
            }
            MDC.remove(MDC_KEY);
        }
    }
}
