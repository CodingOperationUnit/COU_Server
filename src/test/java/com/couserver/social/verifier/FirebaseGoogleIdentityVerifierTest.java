package com.couserver.social.verifier;

import com.couserver.common.exception.BusinessException;
import com.couserver.social.entity.SocialProvider;
import com.couserver.social.exception.SocialErrorCode;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseGoogleIdentityVerifierTest {

    private static final String PROJECT = "cou-test-project";
    private static final String KID = "test-kid";
    private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static KeyPair keyPair;
    private static KeyPair otherKeyPair;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();
    }

    private static FirebaseGoogleIdentityVerifier verifier(String projectId) {
        FirebaseKeySource keys = kid -> KID.equals(kid) ? keyPair.getPublic() : null;
        return new FirebaseGoogleIdentityVerifier(keys, projectId, CLOCK);
    }

    // 정상 토큰을 기준으로, 테스트마다 한 항목씩 바꿔서 만든다
    private static String token(String kid, String audience, String issuer, Instant expiresAt,
                                String signInProvider, String subject, KeyPair signer) {
        var builder = Jwts.builder();
        if (kid != null) {
            builder.header().keyId(kid).and();
        }
        return builder
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(subject)
                .issuedAt(Date.from(NOW.minusSeconds(60)))
                .expiration(Date.from(expiresAt))
                .claim("auth_time", NOW.minusSeconds(60).getEpochSecond())
                .claim("firebase", Map.of("sign_in_provider", signInProvider))
                .signWith(signer.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static String validToken() {
        return token(KID, PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "firebase-uid-1", keyPair);
    }

    private static void assertInvalid(String token) {
        assertThatThrownBy(() -> verifier(PROJECT).verify(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SocialErrorCode.SOCIAL_TOKEN_INVALID);
    }

    @Test
    @DisplayName("정상 토큰이면 Firebase UID를 신원 값으로 돌려준다")
    void valid() {
        SocialIdentity identity = verifier(PROJECT).verify(validToken());
        assertThat(identity).isEqualTo(new SocialIdentity(SocialProvider.GOOGLE_PLAY_GAMES, "firebase-uid-1"));
    }

    @Test
    @DisplayName("프로젝트 ID가 설정되지 않으면 GOOGLE_LOGIN_NOT_CONFIGURED")
    void notConfigured() {
        assertThatThrownBy(() -> verifier("").verify(validToken()))
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(SocialErrorCode.GOOGLE_LOGIN_NOT_CONFIGURED);
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void expired() {
        assertInvalid(token(KID, PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.minusSeconds(120),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", keyPair));
    }

    @Test
    @DisplayName("다른 프로젝트(aud)의 토큰은 거부한다")
    void otherProject() {
        assertInvalid(token(KID, "other-project", "https://securetoken.google.com/other-project", NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", keyPair));
    }

    @Test
    @DisplayName("발급자(iss)가 다르면 거부한다")
    void wrongIssuer() {
        assertInvalid(token(KID, PROJECT, "https://evil.example.com/" + PROJECT, NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", keyPair));
    }

    @Test
    @DisplayName("kid가 없거나 모르는 kid면 거부한다")
    void missingOrUnknownKid() {
        assertInvalid(token(null, PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", keyPair));
        assertInvalid(token("unknown", PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", keyPair));
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 거부한다")
    void wrongSignature() {
        assertInvalid(token(KID, PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.plusSeconds(3600),
                FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER, "uid", otherKeyPair));
    }

    @Test
    @DisplayName("Play Games가 아닌 로그인 수단(예: 익명)의 토큰은 거부한다")
    void otherSignInProvider() {
        assertInvalid(token(KID, PROJECT, "https://securetoken.google.com/" + PROJECT, NOW.plusSeconds(3600),
                "anonymous", "uid", keyPair));
    }

    @Test
    @DisplayName("RS256이 아닌 알고리즘(HS256)으로 서명된 토큰은 거부한다")
    void wrongAlgorithm() {
        String hs256 = Jwts.builder()
                .header().keyId(KID).and()
                .issuer("https://securetoken.google.com/" + PROJECT)
                .audience().add(PROJECT).and()
                .subject("uid")
                .issuedAt(Date.from(NOW.minusSeconds(60)))
                .expiration(Date.from(NOW.plusSeconds(3600)))
                .claim("auth_time", NOW.minusSeconds(60).getEpochSecond())
                .claim("firebase", Map.of("sign_in_provider", FirebaseGoogleIdentityVerifier.PLAY_GAMES_SIGN_IN_PROVIDER))
                .signWith(Keys.hmacShaKeyFor(new byte[32]), Jwts.SIG.HS256)
                .compact();
        assertInvalid(hs256);
    }

    @Test
    @DisplayName("빈 토큰이나 형식이 깨진 토큰은 거부한다")
    void malformed() {
        assertInvalid("");
        assertInvalid("not-a-jwt");
    }
}