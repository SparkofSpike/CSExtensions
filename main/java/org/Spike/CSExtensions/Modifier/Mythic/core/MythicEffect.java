package org.Spike.CSExtensions.Modifier.Mythic.core;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Set;

public class MythicEffect {
    private final String skillName;
    private final String targetSelector;
    private final MythicTrigger trigger;  // 改为 MythicTrigger
    private final double chance;
    private final int timerTicks;
    private final HealthCondition healthCondition;
    private final ConditionParser elementCondition;

    public MythicEffect(String skillName, String targetSelector, MythicTrigger trigger,
                        double chance, int timerTicks, HealthCondition healthCondition,
                        String elementConditionStr) {
        this.skillName = skillName;
        this.targetSelector = targetSelector;
        this.trigger = trigger;
        this.chance = chance;
        this.timerTicks = timerTicks;
        this.healthCondition = healthCondition;
        this.elementCondition = elementConditionStr != null ?
                new ConditionParser(elementConditionStr) : null;
    }

    public String getSkillName() { return skillName; }
    public String getTargetSelector() { return targetSelector; }
    public MythicTrigger getTrigger() { return trigger; }
    public double getChance() { return chance; }
    public int getTimerTicks() { return timerTicks; }
    public HealthCondition getHealthCondition() { return healthCondition; }

    public boolean shouldTrigger(double random) {
        return random < chance;
    }

    public boolean checkHealthCondition(LivingEntity entity) {
        if (healthCondition == null) return true;
        return healthCondition.check(entity);
    }

    public boolean checkConditions(Player caster, LivingEntity target, Set<String> weaponTags) {
        if (healthCondition != null) {
            LivingEntity entity = "@self".equals(targetSelector) ? caster : target;
            if (entity != null && !healthCondition.check(entity)) {
                return false;
            }
        }

        if (elementCondition != null && !elementCondition.evaluate(weaponTags)) {
            return false;
        }

        return true;
    }
}