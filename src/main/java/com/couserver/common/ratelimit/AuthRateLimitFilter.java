package com.couserver.common.ratelimit;

import com.couserver.common.exception.RateLimitedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

// 로그인·회원가입 요청에 IP 기준 제한을 건다. 컨트롤러에 닿기 전에 거부한다
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LIMITED_PATHS = Set.of("/api/accounts/login", "/api/accounts/signup");

    private final IpRateLimiter ipRateLimiter;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(IpRateLimiter ipRateLimiter, ObjectMapper objectMapper) {
        this.ipRateLimiter = ipRateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long retryAfter = ipRateLimiter.tryAcquire(request.getRemoteAddr());
        if (retryAfter > 0) {
            RateLimitedException limited = RateLimitedException.tooManyRequests(retryAfter);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(limited.toErrorResponse()));
            return;
        }
        filterChain.doFilter(request, response);
    }
}