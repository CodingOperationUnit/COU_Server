package com.couserver.social.verifier;

import com.couserver.common.exception.BusinessException;
import com.couserver.social.exception.SocialErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Google이 공개하는 Firebase ID 토큰 서명 인증서(kid → X.509)를 받아 캐시한다
// - 캐시 기간은 응답의 Cache-Control max-age (없으면 1시간)
// - 모르는 kid가 오면 키가 교체됐을 수 있어서 다시 받는다. 단, 1분에 한 번까지만 (잘못된 토큰으로 Google을 계속 부르지 않도록)
@Slf4j
@Component
public class FirebasePublicKeySource implements FirebaseKeySource {

    private static final String CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);
    private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");

    private final RestClient restClient;
    private final Clock clock;

    private Map<String, PublicKey> keys = Map.of();
    private Instant expiresAt = Instant.EPOCH;
    private Instant lastFetchedAt = Instant.EPOCH;

    public FirebasePublicKeySource(Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.clock = clock;
    }

    @Override
    public synchronized PublicKey find(String kid) {
        Instant now = clock.instant();
        boolean expired = !now.isBefore(expiresAt);
        boolean unknownKid = !keys.containsKey(kid);
        boolean canRefresh = !now.isBefore(lastFetchedAt.plus(MIN_REFRESH_INTERVAL));

        if (expired || (unknownKid && canRefresh)) {
            refresh(now);
        }
        return keys.get(kid);
    }

    private void refresh(Instant now) {
        lastFetchedAt = now;
        try {
            ResponseEntity<Map<String, String>> response = restClient.get()
                    .uri(CERTS_URL)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() { });
            Map<String, String> certs = response.getBody();
            if (certs == null || certs.isEmpty()) {
                throw new IllegalStateException("인증서 목록이 비어 있습니다.");
            }

            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Map<String, PublicKey> parsed = new HashMap<>();
            for (Map.Entry<String, String> entry : certs.entrySet()) {
                byte[] pem = entry.getValue().getBytes(StandardCharsets.US_ASCII);
                parsed.put(entry.getKey(), factory.generateCertificate(new ByteArrayInputStream(pem)).getPublicKey());
            }

            keys = Map.copyOf(parsed);
            expiresAt = now.plus(maxAge(response.getHeaders().getCacheControl()));
        } catch (RestClientException | CertificateException | IllegalStateException e) {
            // 키를 못 받으면 검증할 수 없다. 이전 키가 아직 유효하면 그대로 쓰고, 없으면 일시 장애로 응답한다
            log.warn("Firebase 공개키 조회 실패: {}", e.getClass().getSimpleName());
            if (keys.isEmpty() || !now.isBefore(expiresAt)) {
                throw new BusinessException(SocialErrorCode.SOCIAL_PROVIDER_UNAVAILABLE);
            }
        }
    }

    private static Duration maxAge(String cacheControl) {
        if (cacheControl != null) {
            Matcher matcher = MAX_AGE.matcher(cacheControl);
            if (matcher.find()) {
                return Duration.ofSeconds(Long.parseLong(matcher.group(1)));
            }
        }
        return DEFAULT_TTL;
    }
}