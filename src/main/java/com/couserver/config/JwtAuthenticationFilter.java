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
    private static final String CHAT_SOCKET_PATH = "/ws/chat";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);

        // 토큰이 없으면 그냥 통과 (회원가입/로그인처럼 토큰이 필요 없는 요청)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthAccount authAccount;
        try {
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

    // Authorization: Bearer 헤더에서 토큰을 꺼낸다.
    // 브라우저(WebGL) WebSocket 은 헤더를 붙일 수 없어서 /ws/chat 연결만 쿼리(?token=)로도 받는다.
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7);
        }

        if (CHAT_SOCKET_PATH.equals(request.getRequestURI())) {
            return request.getParameter("token");
        }
        return null;
    }
}
