package com.couserver.gamedata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 기획 데이터 종류별 버전. 반영한 내용의 해시를 함께 저장해 내용이 바뀌었을 때만 버전을 올린다.
@Getter
@Entity
@Table(name = "game_data_version")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameDataVersion {

    // 데이터 종류 (예: "skill")
    @Id
    @Column(length = 50)
    private String dataType;

    @Column(nullable = false)
    private int version;

    // 반영한 내용의 SHA-256 (hex)
    @Column(nullable = false, length = 64)
    private String contentHash;

    // 처음 반영할 때는 버전 1로 시작한다
    public static GameDataVersion create(String dataType, String contentHash) {
        GameDataVersion v = new GameDataVersion();
        v.dataType = dataType;
        v.version = 1;
        v.contentHash = contentHash;
        return v;
    }

    public boolean isSameContent(String contentHash) {
        return this.contentHash.equals(contentHash);
    }

    public void increaseVersion(String newContentHash) {
        this.version++;
        this.contentHash = newContentHash;
    }
}
