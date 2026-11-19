package com.parlandim.carteirama.common.security;

import com.parlandim.carteirama.common.error.ErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

@Configuration
public class SecurityConfig {

    private static final String[] SWAGGER_PATHS = {"/api/docs/**", "/api/swagger/**", "/api/swagger", "/api/swagger-ui/**"};

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
                                                   @Qualifier("requestMappingHandlerMapping")
                                                   ObjectProvider<HandlerMapping> routes) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .securityContext(sc -> sc.securityContextRepository(contextRepository))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_PATHS).permitAll()
                        .requestMatchers("/error").permitAll()
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
                        .accessDeniedHandler((req, res, e) -> errors.write(res, HttpStatus.FORBIDDEN,
                                "forbidden", "Acesso negado.")));
        return http.build();
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
