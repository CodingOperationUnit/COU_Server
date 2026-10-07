package com.couserver.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "master_table_version")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MasterTableVersion {
    @Id
    @Column(length = 50)
    private String tableName;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static MasterTableVersion create(String tableName, String contentHash){
        MasterTableVersion v = new MasterTableVersion();
        v.tableName = tableName;
        v.contentHash = contentHash;
        v.updatedAt = LocalDateTime.now();
        return v;
    }

    public boolean isSameContent(String contentHash){
        return this.contentHash.equals(contentHash);
    }

    public void increaseVersion(String newContentHase){
       this.version++;
       this.contentHash = newContentHase;
       this.updatedAt = LocalDateTime.now();
    }
}
