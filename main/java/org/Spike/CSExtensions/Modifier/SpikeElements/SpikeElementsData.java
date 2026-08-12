package org.Spike.CSExtensions.Modifier.SpikeElements;

import org.Spike.CSExtensions.Modifier.Mythic.core.ConditionParser;

import java.util.Set;

public class SpikeElementsData {
    private final String identifier;
    private final double value;
    private final CalculationType type;
    private final String condition;
    private final ConditionParser parser;

    public SpikeElementsData(String identifier, double value, CalculationType type, String condition) {
        this.identifier = identifier.toLowerCase();
        this.value = value;
        this.type = type;
        this.condition = condition;

        if ("conditional".equals(this.identifier) && condition != null && !condition.trim().isEmpty()) {
            this.parser = new ConditionParser(condition);
        } else {
            this.parser = null;
        }
    }

    public boolean matches(Set<String> weaponTags) {
        if ("conditional".equals(identifier)) {
            return parser != null && parser.evaluate(weaponTags);
        } else if ("all".equals(identifier)) {
            return true;
        } else if ("null".equals(identifier)) {
            return weaponTags == null || weaponTags.isEmpty();
        } else if ("others".equals(identifier)) {
            return false;
        } else {
            return weaponTags != null && weaponTags.contains(identifier);
        }
    }

    public String getElement() {
        return getIdentifier();
    }

    public String getIdentifier() { return identifier; }
    public double getValue() { return value; }
    public CalculationType getType() { return type; }
    public String getCondition() { return condition; }
    public boolean isConditional() { return "conditional".equals(identifier); }

    @Override
    public String toString() {
        if (isConditional()) {
            return String.format("(%s) %.2f %s", condition, value, type);
        } else {
            return String.format("%s %.2f %s", identifier, value, type);
        }
    }
}