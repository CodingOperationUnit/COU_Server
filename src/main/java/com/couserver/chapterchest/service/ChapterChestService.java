package com.couserver.chapterchest.service;

import com.couserver.account.entity.StageProgress;
import com.couserver.account.repository.CurrencyRepository;
import com.couserver.account.repository.StageProgressRepository;
import com.couserver.chapterchest.dto.ChapterChestClaimResponse;
import com.couserver.chapterchest.entity.ChapterChestClaim;
import com.couserver.chapterchest.exception.ChapterChestErrorCode;
import com.couserver.chapterchest.repository.ChapterChestClaimRepository;
import com.couserver.common.exception.BusinessException;
import com.couserver.inventory.dto.EquipmentResponse;
import com.couserver.shop.service.ShopService;
import com.couserver.staticdata.dto.ShopProductData;
import com.couserver.staticdata.dto.StageData;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterChestService {
    private static final int CHEST_PRODUCT_ID = 50002;      // 상점 '지구 방위 보급품' (Super~Rare)
    private static final int CHEST_BASE_BOXES = 3;          // 클리어 경험치 CHEST_BASE_EXP일 때 상자 수
    private static final double CHEST_BASE_EXP = 500;       // 1스테이지 클리어 경험치

    private final StaticDataService staticDataService;
    private final CurrencyRepository currencyRepository;
    private final StageProgressRepository stageProgressRepository;
    private final ChapterChestClaimRepository chapterChestClaimRepository;
    private final ShopService shopService;
    private final Clock clock;

    // 클리어한 스테이지의 보물상자를 한 번 지급한다. 상자 수는 클리어 경험치에 비례한다 (1~5스테이지: 3, 4, 5, 6, 7개)
    @Transactional
    public ChapterChestClaimResponse claim(Long playerId, int stageId) {
        StageData stage = staticDataService.getStages().get(stageId);
        if (stage == null) {
            throw new BusinessException(ChapterChestErrorCode.STAGE_NOT_FOUND);
        }

        // 재화 행을 먼저 잠근다. 같은 상자를 동시에 두 번 요청해도 차례대로 처리되어 중복 지급되지 않는다
        currencyRepository.findForUpdate(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "재화 데이터가 없습니다."));

        StageProgress progress = stageProgressRepository.findById(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스테이지 데이터가 없습니다."));
        Integer maxCleared = progress.getMaxClearedStageId();
        if (maxCleared == null || stageId > maxCleared) {
            throw new BusinessException(ChapterChestErrorCode.STAGE_NOT_CLEARED);
        }
        if (chapterChestClaimRepository.existsByPlayerIdAndStageId(playerId, stageId)) {
            throw new BusinessException(ChapterChestErrorCode.ALREADY_CLAIMED);
        }
        chapterChestClaimRepository.save(new ChapterChestClaim(playerId, stageId, Instant.now(clock)));

        ShopProductData product = staticDataService.getShopProducts().get(CHEST_PRODUCT_ID);
        int boxes = (int) Math.round(CHEST_BASE_BOXES * stage.clearAccountExp() / CHEST_BASE_EXP);
        List<EquipmentResponse> rewardedItems = new ArrayList<>();
        for (int i = 0; i < boxes; i++) {
            rewardedItems.add(shopService.drawEquipment(playerId, product));
        }
        return new ChapterChestClaimResponse(stageId, rewardedItems);
    }
}
