package com.couserver.config;

import com.couserver.account.dto.AuthAccount;
import com.couserver.account.entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long accessTtlSeconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ttl-seconds}") long accessTtlSeconds
        ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public String createAccessToken(Account account, Long playerId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTtlSeconds);

        return Jwts.builder()
                .subject(account.getAccountId().toString())  // 토큰 주인 = accountId
                .claim("playerId", playerId.toString())     // 게임 API에서 쓸 playerId
                .issuedAt(Date.from(now))                    // 발급 시각
                .expiration(Date.from(expiresAt))            // 만료 시각
                .signWith(secretKey, Jwts.SIG.HS256)         // 비밀키로 서명
                .compact();
    }

    public AuthAccount parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)      // 같은 비밀키로 서명 검증
                    .build()
                    .parseSignedClaims(token)   // 서명이 다르거나 만료면 여기서 예외
                    .getPayload();
            int accountId = Integer.parseInt(claims.getSubject());
            Long playerId = Long.parseLong(claims.get("playerId", String.class));   // 없으면 예외 → 401
            return new AuthAccount(accountId, playerId);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
