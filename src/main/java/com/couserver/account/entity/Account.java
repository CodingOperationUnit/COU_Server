package com.couserver.account.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public Account(String accountLoginId, String accountPasswordHash) {
        this.accountLoginId = accountLoginId;
        this.accountPasswordHash = accountPasswordHash;
        this.accountCreatedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.accountLastLoginAt = LocalDateTime.now();
    }
}
