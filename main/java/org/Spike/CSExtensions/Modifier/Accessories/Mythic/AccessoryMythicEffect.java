package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import org.Spike.CSExtensions.CSExtensions;

import java.util.HashSet;
import java.util.Set;

public class AccessoryMythicEffect {
    private String skillName;
    private String targetSelector;
    private AccessoryMythicTrigger trigger;
    private int timerTicks;
    private String condition;
    private Set<String> requiredTags;
    private boolean conditionAnd;
    private ConditionParser parser;
    private CSExtensions plugin;

    public AccessoryMythicEffect(CSExtensions plugin) {
        this.requiredTags = new HashSet<>();
        this.timerTicks = 20;
        this.plugin = plugin;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getTargetSelector() {
        return targetSelector;
    }

    public void setTargetSelector(String targetSelector) {
        this.targetSelector = targetSelector;
    }

    public AccessoryMythicTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(AccessoryMythicTrigger trigger) {
        this.trigger = trigger;
    }

    public int getTimerTicks() {
        return timerTicks;
    }

    public void setTimerTicks(int timerTicks) {
        this.timerTicks = timerTicks;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
        if (condition != null && !condition.trim().isEmpty()) {
            this.parser = new ConditionParser(condition);
        } else {
            this.parser = null;
        }
    }

    public void setParser(ConditionParser Parser) {
        this.parser = Parser;
    }

    public Set<String> getRequiredTags() {
        if (parser == null) {
            return new HashSet<>();
        }
        return parser.getAllTags();
    }

    public void setRequiredTags(Set<String> requiredTags) {
        this.requiredTags = requiredTags;
    }

    public boolean isConditionAnd() {
        return conditionAnd;
    }

    public void setConditionAnd(boolean conditionAnd) {
        this.conditionAnd = conditionAnd;
    }

    private void parseCondition() {
        if (condition == null || condition.trim().isEmpty()) {
            this.requiredTags.clear();
            this.conditionAnd = true;
            return;
        }

        String cond = condition.trim().toLowerCase();


        if (cond.contains("&&")) {
            this.conditionAnd = true;
            String[] parts = cond.split("&&");
            for (String part : parts) {
                this.requiredTags.add(part.trim());
            }
        } else if (cond.contains("||")) {
            this.conditionAnd = false;
            String[] parts = cond.split("\\|\\|");
            for (String part : parts) {
                this.requiredTags.add(part.trim());
            }
        } else {

            this.conditionAnd = true;
            this.requiredTags.add(cond.trim());
        }
    }

    public boolean checkCondition(Set<String> weaponTags) {
        if (parser == null) {
            return true;
        }
        try {
            return parser.evaluate(weaponTags);
        } catch (Exception e) {

            if (plugin != null && plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().warning("[饰品Mythic] 条件解析错误: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean isTimerEffect() {
        return trigger == AccessoryMythicTrigger.ON_TIMER;
    }

    public boolean isAttackEffect() {
        return trigger == AccessoryMythicTrigger.ON_ATTACK;
    }

    public boolean isDamagedEffect() {
        return trigger == AccessoryMythicTrigger.ON_DAMAGED;
    }

    public SelectorType getSelectorType() {
        if ("@self".equals(targetSelector)) {
            return SelectorType.SELF;
        } else if ("@shooter".equals(targetSelector)) {
            return SelectorType.SELF;
        } else if ("@victim".equals(targetSelector)) {
            return SelectorType.VICTIM;
        } else if ("@trigger".equals(targetSelector)) {
            return SelectorType.TRIGGER;
        }
        return SelectorType.SELF;
    }

    public enum SelectorType {
        SELF,
        VICTIM,
        TRIGGER
    }

    @Override
    public String toString() {
        return String.format("AccessoryMythicEffect{skill='%s', selector='%s', trigger=%s, condition='%s', timer=%d}",
                skillName, targetSelector, trigger, condition, timerTicks);
    }
}