package com.couserver.config;

import com.couserver.account.dto.AuthAccount;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import org.springframework.http.HttpHeaders;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 토큰이 없으면 그냥 통과 (회원가입/로그인처럼 토큰이 필요 없는 요청)
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthAccount authAccount;
        try {
            String token = authorization.substring(7);
            authAccount = jwtProvider.parseToken(token);
        } catch (ResponseStatusException exception) {
            // 토큰이 있는데 잘못됐으면 바로 401
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(authAccount));
        SecurityContextHolder.setContext(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
