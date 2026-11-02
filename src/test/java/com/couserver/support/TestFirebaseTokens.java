package com.couserver.support;

import com.couserver.social.verifier.FirebaseKeySource;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

// 통합 테스트용 Firebase ID 토큰. 테스트 키로 진짜 서명해서, 서버 검증기를 그대로 통과(또는 실패)하게 만든다
// 공개키 조회만 테스트 키로 바꾸고, 검증 규칙은 운영과 같다
public final class TestFirebaseTokens {

    public static final String PROJECT_ID = "cou-test-project";   // application-test.properties와 같아야 한다
    public static final String KID = "test-kid";
    private static final String PLAY_GAMES = "playgames.google.com";
    private static final KeyPair KEY_PAIR = generate();

    private TestFirebaseTokens() {
    }

    // 정상 토큰
    public static String idToken(String uid) {
        return idToken(uid, PROJECT_ID);
    }

    // aud·iss를 다른 프로젝트로 만든 토큰 (거부되어야 한다)
    public static String idToken(String uid, String projectId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(KID).and()
                .issuer("https://securetoken.google.com/" + projectId)
                .audience().add(projectId).and()
                .subject(uid)
                .issuedAt(Date.from(now.minusSeconds(10)))
                .expiration(Date.from(now.plusSeconds(3600)))
                .claim("auth_time", now.minusSeconds(10).getEpochSecond())
                .claim("firebase", Map.of("sign_in_provider", PLAY_GAMES))
                .signWith(KEY_PAIR.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // 실제 Google 공개키 조회 대신 테스트 공개키를 돌려준다 (모든 통합 테스트가 같은 설정을 쓰도록 공용으로 등록)
    @TestConfiguration
    public static class KeyConfiguration {
        @Bean
        @Primary
        public FirebaseKeySource testFirebaseKeySource() {
            return kid -> KID.equals(kid) ? KEY_PAIR.getPublic() : null;
        }
    }
}