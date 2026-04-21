package com.example.bankcards.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestConfig {


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.disable())
                .securityContext(securityContext -> securityContext.disable())
                .requestCache(requestCache -> requestCache.disable())
                .anonymous(a -> a.disable())
                .httpBasic(h -> h.disable())
                .formLogin(f -> f.disable());

        return http.build();
    }

}
