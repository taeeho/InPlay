package com.inplay.api.security;

import com.inplay.ingest.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 두 경로 군:
 *  - {@code /api/**}: API key 필수 (다른 사용자 multi-tenant 진입점, W7~W8)
 *  - 나머지 ({@code /}, {@code /dashboard/**}): 기존 Spring Boot 기본 basic auth(dev/dev) 유지
 *
 * <p>API key 인증 실패 시 401. SessionCreationPolicy=STATELESS — 토큰 기반 인증이라 세션 X.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public ApiKeyService apiKeyService() {
        return new ApiKeyService();
    }

    @Bean
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http,
                                                 ApiKeyService apiKeyService,
                                                 UserRepository userRepository) throws Exception {
        var filter = new ApiKeyAuthenticationFilter(apiKeyService, userRepository);
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    public SecurityFilterChain dashboardFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(httpBasic -> {});
        return http.build();
    }
}
