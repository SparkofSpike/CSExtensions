package org.Spike.CSExtensions.Modifier.Mythic;

import org.bukkit.entity.LivingEntity;

public class MythicEffect {
    private final String skillCommand;
    private final double chance;
    private final TriggerType trigger;
    private final int timerTicks;
    private final HealthCondition healthCondition;
    private final String targetSelector;

    public MythicEffect(String skillCommand, double chance, TriggerType trigger,
                        int timerTicks, HealthCondition healthCondition, String targetSelector) {
        this.skillCommand = skillCommand;
        this.chance = chance;
        this.trigger = trigger;
        this.timerTicks = timerTicks;
        this.healthCondition = healthCondition;
        this.targetSelector = targetSelector;
    }

    public String getSkillCommand() { return skillCommand; }
    public double getChance() { return chance; }
    public TriggerType getTrigger() { return trigger; }
    public int getTimerTicks() { return timerTicks; }
    public HealthCondition getHealthCondition() { return healthCondition; }
    public String getTargetSelector() { return targetSelector; }

    public boolean shouldTrigger(double random) {
        return random < chance;
    }

    public boolean checkHealthCondition(LivingEntity entity) {
        if (healthCondition == null) return true;
        return healthCondition.check(entity);
    }
}