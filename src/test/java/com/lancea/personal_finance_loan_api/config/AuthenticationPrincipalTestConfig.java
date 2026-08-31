package com.lancea.personal_finance_loan_api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@TestConfiguration
public class AuthenticationPrincipalTestConfig {

    @Bean
    WebMvcConfigurer securityArgumentResolverConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers){
                resolvers.add(new AuthenticationPrincipalArgumentResolver());
            }
        };
    }

}
