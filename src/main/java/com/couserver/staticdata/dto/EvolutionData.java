package com.couserver.staticdata.dto;

import com.couserver.player.entity.PlayerStatType;

import java.util.List;

public record EvolutionData (
        List<PlayerStatType> evolutionStatOrder,
        List<Integer> evolutionIncreasePerLevel,
        int evolutionBaseCost,
        int evolutionCostPerStep,
        int evolutionMaxStep,
        int evolutionStepsPerLevel
){
    public PlayerStatType statAt(int step){
        return evolutionStatOrder.get(step % evolutionStatOrder.size());
    }

    public int increasePerLevel (PlayerStatType type){
        return evolutionIncreasePerLevel.get(evolutionStatOrder.indexOf(type));
    }

    public int costAt(int step){
        return evolutionBaseCost + evolutionCostPerStep * step;
    }

    public boolean isMaxStep(int step) {
        return evolutionMaxStep > 0 && step >= evolutionMaxStep;
    }

    // 계정 레벨로 진화할 수 있는 단계 상한 (계정 레벨 × evolutionStepsPerLevel)
    public int levelStepLimit(int accountLevel) {
        return accountLevel * evolutionStepsPerLevel;
    }

    // 계정 레벨이 부족해서 더 진화할 수 없는 단계인지
    public boolean isLevelLocked(int step, int accountLevel) {
        return step >= levelStepLimit(accountLevel);
    }
}
