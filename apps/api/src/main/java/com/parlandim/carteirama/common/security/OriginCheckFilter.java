package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.error.ErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> allowedOrigins;
    private final ErrorWriter errors;

    public OriginCheckFilter(Collection<String> origins, ErrorWriter errors) {
        this.allowedOrigins = origins.stream()
                .map(OriginCheckFilter::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || allowed(origin, request)) {
            chain.doFilter(request, response);
            return;
        }
        errors.write(response, HttpStatus.FORBIDDEN, "forbidden_origin", "Origem da requisição não permitida.");
    }

    private boolean allowed(String origin, HttpServletRequest request) {
        String normalized = normalize(origin);
        if (normalized == null) {
            return false;
        }
        if (allowedOrigins.contains(normalized)) {
            return true;
        }
        String host = request.getHeader("Host");
        return host != null && normalized.endsWith("://" + host.toLowerCase(Locale.ROOT));
    }

    private static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || uri.getRawAuthority() == null) {
                return null;
            }
            return (uri.getScheme() + "://" + uri.getRawAuthority()).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
