package com.couserver.staticdata.dto;

public record CombatPowerData(
        int combatPowerAttackWeight,
        int combatPowerHpWeight,
        int combatPowerDefenseWeight,
        boolean combatPowerUseCritical
) {
    public int calculate(int attack, int hp, int defense){
        int value = Math.multiplyExact(attack, combatPowerAttackWeight);
        value = Math.addExact(value, Math.multiplyExact(hp, combatPowerHpWeight));
        value = Math.addExact(value, Math.multiplyExact(defense, combatPowerDefenseWeight));

        return value;
    }
}
