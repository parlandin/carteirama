package com.parlandim.carteirama.common.web;

import com.parlandim.carteirama.common.CarteiramaProperties;
import com.parlandim.carteirama.common.security.AccountAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CarteiramaProperties.class)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AccountAccessInterceptor accountAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountAccessInterceptor).addPathPatterns("/api/**");
    }
}
