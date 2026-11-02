package com.couserver.social.service;

import com.couserver.account.entity.Account;
import com.couserver.account.repository.AccountRepository;
import com.couserver.account.service.AccountService;
import com.couserver.common.exception.BusinessException;
import com.couserver.social.dto.SocialLinkStatusResponse;
import com.couserver.social.dto.SocialLoginResponse;
import com.couserver.social.entity.SocialLink;
import com.couserver.social.entity.SocialProvider;
import com.couserver.social.exception.SocialErrorCode;
import com.couserver.social.repository.SocialLinkRepository;
import com.couserver.social.verifier.SocialIdentity;
import com.couserver.social.verifier.SocialIdentityVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

// 소셜 로그인·연동
// - 토큰 검증은 트랜잭션 밖에서 한다 (공개키 조회로 기다리는 동안 DB 트랜잭션을 잡지 않도록)
// - 동시 요청은 social_link의 유니크 제약이 막고, 제약에 걸리면 새 트랜잭션으로 한 번 다시 판단한다
@Service
public class SocialAuthService {

    private final Map<SocialProvider, SocialIdentityVerifier> verifiers = new EnumMap<>(SocialProvider.class);
    private final SocialLinkRepository socialLinkRepository;
    private final AccountRepository accountRepository;   // 계정 담당 (연동할 계정 참조만)
    private final AccountService accountService;         // 계정 담당 (계정 생성·토큰 발급)
    private final TransactionTemplate transaction;
    private final Clock clock;

    public SocialAuthService(List<SocialIdentityVerifier> verifierList,
                             SocialLinkRepository socialLinkRepository,
                             AccountRepository accountRepository,
                             AccountService accountService,
                             PlatformTransactionManager transactionManager,
                             Clock clock) {
        verifierList.forEach(v -> verifiers.put(v.provider(), v));
        this.socialLinkRepository = socialLinkRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    // 소셜 로그인: 연동된 계정이 있으면 그 계정으로, 없으면 새 계정을 만들어 로그인한다
    public SocialLoginResponse login(SocialProvider provider, String token) {
        SocialIdentity identity = verify(provider, token);
        return retryOnConflict(() -> loginInTransaction(identity));
    }

    // 로그인한 계정에 소셜 신원을 연동한다
    public SocialLinkStatusResponse link(int accountId, SocialProvider provider, String token) {
        SocialIdentity identity = verify(provider, token);
        return retryOnConflict(() -> linkInTransaction(accountId, identity));
    }

    // 연동 상태 조회
    public SocialLinkStatusResponse status(int accountId, SocialProvider provider) {
        return socialLinkRepository.findByAccount_AccountIdAndSocialLinkProvider(accountId, provider)
                .map(link -> SocialLinkStatusResponse.linked(link.getSocialLinkCreatedAt()))
                .orElseGet(SocialLinkStatusResponse::notLinked);
    }

    private SocialIdentity verify(SocialProvider provider, String token) {
        SocialIdentityVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new IllegalStateException("검증기가 없는 로그인 방식: " + provider);
        }
        return verifier.verify(token);
    }

    private SocialLoginResponse loginInTransaction(SocialIdentity identity) {
        return transaction.execute(status -> {
            Optional<SocialLink> existing = findLink(identity);
            if (existing.isPresent()) {
                return SocialLoginResponse.of(accountService.issueLoginResponse(existing.get().getAccount()), false);
            }

            Account account = accountService.createSocialAccount();
            // 즉시 INSERT해서, 같은 신원으로 동시에 들어온 요청과의 충돌을 이 트랜잭션 안에서 드러낸다
            socialLinkRepository.saveAndFlush(SocialLink.create(
                    account, identity.provider(), identity.providerUserId(), now()));
            return SocialLoginResponse.of(accountService.issueLoginResponse(account), true);
        });
    }

    private SocialLinkStatusResponse linkInTransaction(int accountId, SocialIdentity identity) {
        return transaction.execute(status -> {
            Optional<SocialLink> byIdentity = findLink(identity);
            if (byIdentity.isPresent()) {
                if (byIdentity.get().getAccount().getAccountId() == accountId) {
                    return SocialLinkStatusResponse.linked(byIdentity.get().getSocialLinkCreatedAt());   // 이미 이 계정에 연동됨
                }
                throw new BusinessException(SocialErrorCode.SOCIAL_IDENTITY_ALREADY_LINKED);
            }
            if (socialLinkRepository.findByAccount_AccountIdAndSocialLinkProvider(accountId, identity.provider()).isPresent()) {
                throw new BusinessException(SocialErrorCode.SOCIAL_PROVIDER_ALREADY_LINKED);
            }

            Account account = accountRepository.getReferenceById(accountId);
            SocialLink link = socialLinkRepository.saveAndFlush(SocialLink.create(
                    account, identity.provider(), identity.providerUserId(), now()));
            return SocialLinkStatusResponse.linked(link.getSocialLinkCreatedAt());
        });
    }

    private Optional<SocialLink> findLink(SocialIdentity identity) {
        return socialLinkRepository.findBySocialLinkProviderAndSocialLinkProviderUserId(
                identity.provider(), identity.providerUserId());
    }

    // 유니크 제약에 걸리면(동시 요청) 그 트랜잭션은 통째로 롤백된다. 새 트랜잭션으로 한 번만 다시 판단한다
    private static <T> T retryOnConflict(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException first) {
            return action.get();
        }
    }

    private Instant now() {
        return clock.instant();
    }
}