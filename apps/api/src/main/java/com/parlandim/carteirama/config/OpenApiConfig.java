package com.parlandim.carteirama.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carteiramaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Carteirama API")
                        .description("API do Carteirama. Autenticação por cookie de sessão: faça POST /api/v1/auth/login "
                                + "nesta página e as próximas chamadas já vão autenticadas.")
                        .version("v1"));
    }

    @Bean
    public ModelResolver modelResolver() {
        return new ModelResolver(Json.mapper().copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE));
    }
}
