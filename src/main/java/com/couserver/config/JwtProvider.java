package com.couserver.config;

import com.couserver.account.entity.Account;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public String createAccesssToken(Account account) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTtlSeconds);

        return Jwts.builder()
                .subject(account.getAccountId().toString())  // 토큰 주인 = accountId
                .issuedAt(Date.from(now))                    // 발급 시각
                .expiration(Date.from(expiresAt))            // 만료 시각
                .signWith(secretKey, Jwts.SIG.HS256)         // 비밀키로 서명
                .compact();
    }
}
