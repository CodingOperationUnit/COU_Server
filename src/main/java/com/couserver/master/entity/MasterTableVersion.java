package com.couserver.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Instant updatedAt;

    public static MasterTableVersion create(String tableName, String contentHash, Instant now){
        MasterTableVersion v = new MasterTableVersion();
        v.tableName = tableName;
        v.contentHash = contentHash;
        v.updatedAt = now;
        return v;
    }

    public boolean isSameContent(String contentHash){
        return this.contentHash.equals(contentHash);
    }

    public void increaseVersion(String newContentHash, Instant now){
       this.version++;
       this.contentHash = newContentHash;
       this.updatedAt = now;
    }
}
