package com.couserver.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId;

    @Column(nullable = false, unique = true, length = 20)
    private String accountLoginId;

    @Column(nullable = false)
    private String accountPasswordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime accountCreatedAt;

    private LocalDateTime accountLastLoginAt;

    // 값이 늘어날 수 있어 varchar로 만든다 (EnumColumnTypeTest). 컬럼이 추가될 때 기존 행은 NORMAL로 채워진다
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NORMAL'")
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private AccountType accountType = AccountType.NORMAL;

    public Account(String accountLoginId, String accountPasswordHash) {
        this.accountLoginId = accountLoginId;
        this.accountPasswordHash = accountPasswordHash;
        this.accountCreatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.accountLastLoginAt = LocalDateTime.now();
    }
}
