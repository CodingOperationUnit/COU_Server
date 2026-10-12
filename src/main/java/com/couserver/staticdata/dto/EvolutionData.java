package com.couserver.staticdata.dto;

import com.couserver.player.entity.PlayerStatType;

import java.util.List;

public record EvolutionData (
        List<PlayerStatType> evolutionStatOrder,
        List<Integer> evolutionIncreasePerLevel,
        int evolutionBaseCost,
        int evolutionCostPerStep,
        int evolutionMaxStep
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
}
