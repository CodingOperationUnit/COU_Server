package com.couserver.staticdata.dto;

// 드롭 아이템 종류. DropTable·DropItem의 dropItemType. NONE은 DropTable에서만 쓰며 드롭 없음을 뜻한다
public enum DropItemType {
    EXPGEM1, EXPGEM2, EXPGEM3, EXPGEM4,
    GOLD1, GOLD2, GOLD3, GOLD4,
    LUCKYBOX, REWARDBOX, POTION, MAGNET, BOMB,
    NONE
}
