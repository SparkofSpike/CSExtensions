package org.Spike.CSExtensions.Modifier.HealthAdjust;

import java.util.ArrayList;
import java.util.List;

public class HealthAdjustConfig {
    private boolean enabled = false;

    private List<HealthAdjustEffect> healSelfInstant = new ArrayList<>();
    private List<HealthAdjustEffect> damageSelfInstant = new ArrayList<>();
    private List<HealthAdjustEffect> healTargetInstant = new ArrayList<>();
    private List<HealthAdjustEffect> damageTargetInstant = new ArrayList<>();
    private List<HealthAdjustEffect> healSelfConstant = new ArrayList<>();
    private List<HealthAdjustEffect> damageSelfConstant = new ArrayList<>();
    private List<HealthAdjustEffect> healTargetConstant = new ArrayList<>();
    private List<HealthAdjustEffect> damageTargetConstant = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<HealthAdjustEffect> getHealSelfInstant() {
        return healSelfInstant;
    }

    public void setHealSelfInstant(List<HealthAdjustEffect> healSelfInstant) {
        this.healSelfInstant = healSelfInstant;
    }

    public List<HealthAdjustEffect> getDamageSelfInstant() {
        return damageSelfInstant;
    }

    public void setDamageSelfInstant(List<HealthAdjustEffect> damageSelfInstant) {
        this.damageSelfInstant = damageSelfInstant;
    }

    public List<HealthAdjustEffect> getHealTargetInstant() {
        return healTargetInstant;
    }

    public void setHealTargetInstant(List<HealthAdjustEffect> healTargetInstant) {
        this.healTargetInstant = healTargetInstant;
    }

    public List<HealthAdjustEffect> getDamageTargetInstant() {
        return damageTargetInstant;
    }

    public void setDamageTargetInstant(List<HealthAdjustEffect> damageTargetInstant) {
        this.damageTargetInstant = damageTargetInstant;
    }

    public List<HealthAdjustEffect> getHealTargetConstant() {
        return healTargetConstant;
    }

    public void setHealTargetConstant(List<HealthAdjustEffect> healTargetConstant) {
        this.healTargetConstant = healTargetConstant;
    }

    public List<HealthAdjustEffect> getDamageTargetConstant() {
        return damageTargetConstant;
    }

    public void setDamageTargetConstant(List<HealthAdjustEffect> damageTargetConstant) {
        this.damageTargetConstant = damageTargetConstant;
    }

    public List<HealthAdjustEffect> getHealSelfConstant() {
        return healSelfConstant;
    }

    public void setHealSelfConstant(List<HealthAdjustEffect> healSelfConstant) {
        this.healSelfConstant = healSelfConstant;
    }

    public List<HealthAdjustEffect> getDamageSelfConstant() {
        return damageSelfConstant;
    }

    public void setDamageSelfConstant(List<HealthAdjustEffect> damageSelfConstant) {
        this.damageSelfConstant = damageSelfConstant;
    }

    public boolean hasAnyEffect() {
        return !healSelfInstant.isEmpty() || !damageSelfInstant.isEmpty() || !healSelfConstant.isEmpty() || !damageSelfConstant.isEmpty() || !healTargetInstant.isEmpty() || !damageTargetInstant.isEmpty() || !healTargetConstant.isEmpty() || !damageTargetConstant.isEmpty();
    }

    public double getTargetInstantValue(Trigger trigger, boolean isHealing) {
        List<HealthAdjustEffect> effects = isHealing ? healTargetInstant : damageTargetInstant;
        for (HealthAdjustEffect effect : effects) {
            if (effect.getTrigger() == trigger && effect.isInstant()) {
                return effect.getAmountPerTick();
            }
        }
        return 0;
    }

    public List<HealthAdjustEffect> getEffectsForTrigger(Trigger trigger) {
        List<HealthAdjustEffect> results = new ArrayList<>();

        results.addAll(getEffectsByTrigger(healSelfInstant, trigger));
        results.addAll(getEffectsByTrigger(damageSelfInstant, trigger));
        results.addAll(getEffectsByTrigger(healSelfConstant, trigger));
        results.addAll(getEffectsByTrigger(damageSelfConstant, trigger));
        results.addAll(getEffectsByTrigger(healTargetInstant, trigger));
        results.addAll(getEffectsByTrigger(damageTargetInstant, trigger));
        results.addAll(getEffectsByTrigger(healTargetConstant, trigger));
        results.addAll(getEffectsByTrigger(damageTargetConstant, trigger));

        return results;
    }

    private List<HealthAdjustEffect> getEffectsByTrigger(List<HealthAdjustEffect> effects, Trigger trigger) {
        List<HealthAdjustEffect> result = new ArrayList<>();
        for (HealthAdjustEffect effect : effects) {
            if (effect.getTrigger() == trigger) {
                result.add(effect);
            }
        }
        return result;
    }

    public enum Trigger {
        SHOOT("shoot"), HIT("hit"), KILL("kill"), CRIT("crit"), HEADSHOT("headshot"), RELOAD("reload");

        private final String configName;

        Trigger(String configName) {
            this.configName = configName;
        }

        public static Trigger fromString(String name) {
            if (name == null) return null;
            for (Trigger trigger : values()) {
                if (trigger.configName.equalsIgnoreCase(name)) {
                    return trigger;
                }
            }
            return null;
        }

        public String getConfigName() {
            return configName;
        }
    }

}