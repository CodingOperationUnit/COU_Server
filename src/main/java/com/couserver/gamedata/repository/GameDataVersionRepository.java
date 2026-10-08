package com.couserver.gamedata.repository;

import com.couserver.gamedata.entity.GameDataVersion;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface GameDataVersionRepository extends JpaRepository<GameDataVersion, String> {

    // 서버 여러 대가 동시에 시작해도 버전이 두 번 오르지 않도록 행을 잠그고 읽는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameDataVersion> findWithLockByDataType(String dataType);
}
