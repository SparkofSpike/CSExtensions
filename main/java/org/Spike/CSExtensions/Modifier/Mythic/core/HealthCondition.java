package org.Spike.CSExtensions.Modifier.Mythic.core;

import org.bukkit.entity.LivingEntity;

public class HealthCondition {
    private final char operator;
    private final double value;
    private final boolean isPercentage;

    public HealthCondition(String conditionStr) {
        conditionStr = conditionStr.trim();

        if (conditionStr.contains("%")) {
            isPercentage = true;
            conditionStr = conditionStr.replace("%", "");
        } else {
            isPercentage = false;
        }

        if (conditionStr.startsWith(">=")) {
            operator = '≥';
            value = Double.parseDouble(conditionStr.substring(2));
        } else if (conditionStr.startsWith("<=")) {
            operator = '≤';
            value = Double.parseDouble(conditionStr.substring(2));
        } else if (conditionStr.startsWith("!=")) {
            operator = '≠';
            value = Double.parseDouble(conditionStr.substring(2));
        } else if (conditionStr.startsWith(">")) {
            operator = '>';
            value = Double.parseDouble(conditionStr.substring(1));
        } else if (conditionStr.startsWith("<")) {
            operator = '<';
            value = Double.parseDouble(conditionStr.substring(1));
        } else if (conditionStr.startsWith("=")) {
            operator = '=';
            value = Double.parseDouble(conditionStr.substring(1));
        } else {
            operator = '=';
            value = Double.parseDouble(conditionStr);
        }
    }

    public boolean check(LivingEntity entity) {
        double currentHealth = entity.getHealth();
        double maxHealth = entity.getMaxHealth();
        double compareValue = isPercentage ? (value / 100.0 * maxHealth) : value;

        switch (operator) {
            case '>':
                return currentHealth > compareValue;
            case '<':
                return currentHealth < compareValue;
            case '=':
                return Math.abs(currentHealth - compareValue) < 0.001;
            case '≠':
                return Math.abs(currentHealth - compareValue) >= 0.001;
            case '≥':
                return currentHealth >= compareValue;
            case '≤':
                return currentHealth <= compareValue;
            default:
                return true;
        }
    }

    public static HealthCondition parse(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        return new HealthCondition(str);
    }
}