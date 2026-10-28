package com.couserver.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.couserver.common.ratelimit.AuthRateLimitFilter;
import com.couserver.common.ratelimit.IpRateLimiter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   IpRateLimiter ipRateLimiter, ObjectMapper objectMapper) throws Exception {
        http
                .cors(Customizer.withDefaults()) // corsConfigurationSource 빈 사용
                .csrf(AbstractHttpConfigurer::disable) // 쿠키 인증 안 씀 -> CSRF 보호 끄기
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안 씀
                .formLogin(AbstractHttpConfigurer::disable) // 기본 로그인 폼 끄기
                .httpBasic(AbstractHttpConfigurer::disable) // Http Basic 인증 끄기
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/signup", "/api/accounts/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/static-data").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new AuthRateLimitFilter(ipRateLimiter, objectMapper), AuthorizationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), AuthorizationFilter.class);

        return http.build();
    }

    // WebGL 빌드는 다른 출처의 페이지에서 API 를 부른다. 쿠키 없이 Bearer 토큰만 쓰므로 모든 출처 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
