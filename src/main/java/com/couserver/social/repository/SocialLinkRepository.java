package com.couserver.social.repository;

import com.couserver.social.entity.SocialLink;
import com.couserver.social.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {

    // 로그인: 이 소셜 신원이 어느 계정에 연동돼 있는지
    Optional<SocialLink> findBySocialLinkProviderAndSocialLinkProviderUserId(
            SocialProvider provider, String providerUserId);

    // 연동·상태 조회: 이 계정에 이 방식이 연동돼 있는지
    Optional<SocialLink> findByAccount_AccountIdAndSocialLinkProvider(Integer accountId, SocialProvider provider);
}