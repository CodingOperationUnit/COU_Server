package com.couserver.social.entity;

import com.couserver.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 계정과 소셜 로그인 신원의 연결
// - (방식, 신원 값) 유니크: 한 소셜 신원이 여러 계정에 붙지 않는다
// - (계정, 방식) 유니크: 한 계정에 같은 방식이 두 번 붙지 않는다
// 동시에 같은 연동 요청이 와도 이 두 제약이 최종적으로 막는다
@Getter
@Entity
@Table(name = "social_link",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_link_provider_user",
                        columnNames = {"socialLinkProvider", "socialLinkProviderUserId"}),
                @UniqueConstraint(name = "uk_social_link_account_provider",
                        columnNames = {"account_id", "socialLinkProvider"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long socialLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // 방식이 늘어날 수 있어서 MySQL ENUM이 아닌 varchar로 만든다 (팀 관례, EnumColumnTypeTest)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private SocialProvider socialLinkProvider;

    // 방식별 신원 값. 구글은 검증된 Firebase ID 토큰의 subject(UID, 최대 128자)
    @Column(nullable = false, length = 128)
    private String socialLinkProviderUserId;

    @Column(nullable = false, updatable = false)
    private Instant socialLinkCreatedAt;

    public static SocialLink create(Account account, SocialProvider provider, String providerUserId, Instant now) {
        SocialLink link = new SocialLink();
        link.account = account;
        link.socialLinkProvider = provider;
        link.socialLinkProviderUserId = providerUserId;
        link.socialLinkCreatedAt = now;
        return link;
    }
}