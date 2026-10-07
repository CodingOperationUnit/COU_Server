package com.couserver.player.dummy;

import com.couserver.player.entity.PlayerStatType;

// 진화 더미 데이터 (임시 코드)
// TODO : 진화 시트가 확정되면 마스터 데이터(evolution 테이블)로 교체하고 이 클래스는 삭제 -> 추후 별개의 계획을 통해 변동 가능성 있음.
public class EvolutionDummyData {
    private EvolutionDummyData(){

    }

    // 진화 단계(0부터)별 골드 비용. 지금은 모든 단계 0
    public static int getGoldCost(int step){
        return 0;
    }

    // 능력치별 레벨 1당 상승량. 지금은 모두 0 (최종 스탯 계산에서 사용)
    public static int getIncreasePerLevel (PlayerStatType type){
        return switch (type){
            case ATTACK -> 0;
            case HP -> 0;
            case DEFENSE -> 0;
            case POTION_RECOVERY -> 0;
        };
    }
}
