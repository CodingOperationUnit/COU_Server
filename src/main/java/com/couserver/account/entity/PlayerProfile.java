package com.couserver.account.entity;

import com.couserver.staticdata.dto.AccountConstData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "player_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(nullable = false, unique = true, length = 20)
    private String playerNickname;

    @Column(nullable = false)
    private int accountLevel = 1;

    @Column(nullable = false)
    private int accountExp = 0;

    public PlayerProfile(Account account, String playerNickname) {
        this.account = account;
        this.playerNickname = playerNickname;
    }

    public void updateProgress(String playerNickname, int accountLevel, int accountExp) {
        this.playerNickname = playerNickname;
        this.accountLevel = accountLevel;
        this.accountExp = accountExp;
    }

    // 경험치는 현재 레벨 기준으로 저장한다. 필요 경험치를 채울 때마다 빼고 레벨을 올리며, 최대 레벨에서는 경험치만 쌓인다
    public void gainExp(int amount, AccountConstData accountConst) {
        this.accountExp += amount;
        while (accountLevel < accountConst.maxAccountLevel()
                && accountExp >= accountConst.requiredExp(accountLevel)) {
            this.accountExp -= accountConst.requiredExp(accountLevel);
            this.accountLevel++;
        }
    }
}
