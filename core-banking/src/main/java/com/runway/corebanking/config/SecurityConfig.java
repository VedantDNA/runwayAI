package com.runway.corebanking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary permissive security config — allows all requests so the app starts
 * cleanly and Flyway can run migrations without auth interference.
 *
 * NEXT STEP (Granule 2-C): Replace permitAll() with JWT filter chain:
 *   - Only /api/v1/auth/** stays public
 *   - All other endpoints require a valid Bearer token
 *   - Sessions remain STATELESS (no server-side HttpSession)
 *
 * WHY STATELESS: In a microservice architecture, any service instance must be
 * able to validate a request independently — without calling a central session
 * store. A self-contained signed JWT achieves this.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                         // REST APIs don't use CSRF cookies
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll());                        // TODO: lock down in Granule 2-C

        return http.build();
    }

    /**
     * BCrypt with default work factor 10.
     * Registered as a Spring bean so it can be injected into AuthService.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
