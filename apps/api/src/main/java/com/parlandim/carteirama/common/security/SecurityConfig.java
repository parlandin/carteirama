package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.error.ErrorWriter;
import com.parlandim.carteirama.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {"/api/docs/**", "/api/swagger/**", "/api/swagger", "/api/swagger-ui/**"};

    public static final String CSRF_PATH = "/api/v1/auth/csrf";

    private static final String WEBHOOKS = "/api/v1/webhooks/**";

    private static final String[] PUBLIC_POSTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/webhooks/**"
    };

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ErrorWriter errors,
                                                   SecurityContextRepository contextRepository,
                                                   CarteiramaProperties properties,
                                                   @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookies,
                                                   @Value("${carteirama.cors.allowed-origins:}") String corsOrigins,
                                                   @Value("${carteirama.csrf.cookie-domain:}") String csrfCookieDomain,
                                                   @Qualifier("requestMappingHandlerMapping")
                                                   ObjectProvider<HandlerMapping> routes) throws Exception {
        List<String> crossOrigins = splitOrigins(corsOrigins);
        if (!crossOrigins.isEmpty()) {
            http.cors(cors -> cors.configurationSource(corsSource(crossOrigins)));
        }
        List<String> allowedOrigins = new ArrayList<>(crossOrigins);
        allowedOrigins.add(properties.frontendUrl());
        http
                .csrf(csrf -> csrf.spa()
                        .csrfTokenRepository(csrfTokenRepository(secureCookies, csrfCookieDomain))
                        .ignoringRequestMatchers(WEBHOOKS))
                .addFilterBefore(new OriginCheckFilter(allowedOrigins, errors), CsrfFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(contextRepository))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, CSRF_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POSTS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            switch (match(routes.getObject(), req)) {
                                case NONE -> errors.write(res, HttpStatus.NOT_FOUND, "not_found",
                                        "Recurso não encontrado.");
                                case WRONG_METHOD -> errors.write(res, HttpStatus.METHOD_NOT_ALLOWED,
                                        "method_not_allowed", "Método não permitido para esta rota.");
                                case FOUND -> errors.write(res, HttpStatus.UNAUTHORIZED,
                                        "not_authenticated", "Faça login para continuar.");
                            }
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            if (e instanceof CsrfException) {
                                errors.write(res, HttpStatus.FORBIDDEN, "csrf_invalid",
                                        "Não foi possível validar a requisição. Recarregue a página e tente de novo.");
                            } else {
                                errors.write(res, HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.");
                            }
                        }));
        return http.build();
    }

    private static CookieCsrfTokenRepository csrfTokenRepository(boolean secure, String domain) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        String cookieDomain = domain == null || domain.isBlank() ? null : domain.strip();
        repository.setCookieCustomizer(cookie -> {
            cookie.secure(secure).sameSite("Lax");
            if (cookieDomain != null) {
                cookie.domain(cookieDomain);
            }
        });
        return repository;
    }

    private static List<String> splitOrigins(String origins) {
        if (origins == null || origins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(origins.split(","))
                .map(String::strip)
                .map(o -> o.replaceAll("/+$", ""))
                .filter(o -> !o.isEmpty())
                .toList();
    }

    private static CorsConfigurationSource corsSource(List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN", RequestIdFilter.HEADER));
        config.setExposedHeaders(List.of("Content-Disposition", RequestIdFilter.HEADER));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private enum RouteMatch { FOUND, NONE, WRONG_METHOD }

    private static RouteMatch match(HandlerMapping routes, HttpServletRequest request) {
        if (!ServletRequestPathUtils.hasParsedRequestPath(request)) {
            ServletRequestPathUtils.parseAndCache(request);
        }
        try {
            return routes.getHandler(request) != null ? RouteMatch.FOUND : RouteMatch.NONE;
        } catch (HttpRequestMethodNotSupportedException e) {
            return RouteMatch.WRONG_METHOD;
        } catch (Exception e) {
            return RouteMatch.FOUND;
        }
    }
}
