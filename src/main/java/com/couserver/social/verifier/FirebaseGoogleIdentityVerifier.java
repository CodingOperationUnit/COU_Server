package com.couserver.social.verifier;

import com.couserver.common.exception.BusinessException;
import com.couserver.social.entity.SocialProvider;
import com.couserver.social.exception.SocialErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

// 구글 플레이 로그인(Play Games → Firebase Auth)의 Firebase ID 토큰을 검증한다
// 공식 문서의 "third-party JWT 라이브러리로 검증" 방식: 서비스 계정 키 없이 프로젝트 ID와 공개키만 쓴다
// 토큰 값은 로그·에러 메시지에 절대 남기지 않는다
@Slf4j
@Component
public class FirebaseGoogleIdentityVerifier implements SocialIdentityVerifier {

    private static final String ISSUER_PREFIX = "https://securetoken.google.com/";
    private static final String ALGORITHM = "RS256";
    private static final long CLOCK_SKEW_SECONDS = 60;
    private static final int MAX_UID_LENGTH = 128;

    // Play Games로 로그인한 Firebase 토큰의 로그인 수단 값 (가정: 클라이언트에서 실제 토큰으로 확인 필요)
    static final String PLAY_GAMES_SIGN_IN_PROVIDER = "playgames.google.com";

    private final FirebaseKeySource keySource;
    private final String projectId;
    private final Clock clock;

    public FirebaseGoogleIdentityVerifier(FirebaseKeySource keySource,
                                          @Value("${firebase.project-id:}") String projectId,
                                          Clock clock) {
        this.keySource = keySource;
        this.projectId = projectId == null ? "" : projectId.trim();
        this.clock = clock;
    }

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE_PLAY_GAMES;
    }

    @Override
    public SocialIdentity verify(String idToken) {
        if (projectId.isEmpty()) {
            throw new BusinessException(SocialErrorCode.GOOGLE_LOGIN_NOT_CONFIGURED);
        }
        if (idToken == null || idToken.isBlank()) {
            throw invalid("토큰 없음");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            if (!ALGORITHM.equals(header.getAlgorithm())) {
                                throw new JwtException("허용하지 않는 서명 알고리즘");
                            }
                            String kid = header.getKeyId();
                            if (kid == null || kid.isBlank()) {
                                throw new JwtException("kid 없음");
                            }
                            PublicKey key = keySource.find(kid);   // 조회 실패는 BusinessException(503)으로 그대로 나간다
                            if (key == null) {
                                throw new JwtException("모르는 kid");
                            }
                            return key;
                        }
                    })
                    .requireIssuer(ISSUER_PREFIX + projectId)
                    .requireAudience(projectId)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw invalid(e.getClass().getSimpleName());
        }

        Instant now = clock.instant();
        Instant latestAllowed = now.plusSeconds(CLOCK_SKEW_SECONDS);

        Date issuedAt = claims.getIssuedAt();
        if (issuedAt == null || issuedAt.toInstant().isAfter(latestAllowed)) {
            throw invalid("iat");
        }
        Object authTime = claims.get("auth_time");
        if (!(authTime instanceof Number n) || Instant.ofEpochSecond(n.longValue()).isAfter(latestAllowed)) {
            throw invalid("auth_time");
        }

        String uid = claims.getSubject();
        if (uid == null || uid.isBlank() || uid.length() > MAX_UID_LENGTH) {
            throw invalid("sub");
        }

        Object firebase = claims.get("firebase");
        Object signInProvider = firebase instanceof Map<?, ?> map ? map.get("sign_in_provider") : null;
        if (!PLAY_GAMES_SIGN_IN_PROVIDER.equals(signInProvider)) {
            throw invalid("sign_in_provider");
        }

        return new SocialIdentity(SocialProvider.GOOGLE_PLAY_GAMES, uid);
    }

    // 실패 이유는 서버 로그에만 짧게 남기고(토큰 값 제외), 클라이언트에는 같은 에러 하나로 응답한다
    private BusinessException invalid(String reason) {
        log.debug("Firebase ID 토큰 거부: {}", reason);
        return new BusinessException(SocialErrorCode.SOCIAL_TOKEN_INVALID);
    }
}