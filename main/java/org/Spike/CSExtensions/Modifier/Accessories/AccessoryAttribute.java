package org.Spike.CSExtensions.Modifier.Accessories;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Mythic.core.ConditionParser;;

import java.util.Set;

public class AccessoryAttribute {
    private final String element;
    private final double value;
    private final CalculationType calcType;
    private final String condition;
    private final ConditionParser parser;
    private final CSExtensions plugin;

    public AccessoryAttribute(String element, double value, CalculationType calcType,CSExtensions plugin) {
        this(element, value, calcType, null,plugin);
    }

    public AccessoryAttribute(String element, double value, CalculationType calcType,
                              String condition,CSExtensions plugin) {
        this.element = element != null ? element.toLowerCase() : "all";
        this.value = value;
        this.calcType = calcType;
        this.condition = condition;
        this.plugin = plugin;

        if ("conditional".equals(this.element) && condition != null && !condition.trim().isEmpty()) {
            this.parser = new ConditionParser(condition);
        } else {
            this.parser = null;
        }
    }

    public String getElement() { return element; }
    public double getValue() { return value; }
    public CalculationType getCalcType() { return calcType; }
    public String getCondition() { return condition; }
    public boolean isConditional() { return "conditional".equals(element); }

    public boolean matchesElement(String weaponElement) {
        if (isConditional()) {
            return false;
        }

        if ("all".equals(element)) {
            return true;
        }
        if ("null".equals(element)) {
            return weaponElement == null || weaponElement.isEmpty() || "null".equals(weaponElement);
        }
        return element.equalsIgnoreCase(weaponElement);
    }

    public boolean matchesCondition(Set<String> weaponTags) {
        if (parser == null) {
            return matchesElement(weaponTags != null && !weaponTags.isEmpty() ?
                    weaponTags.iterator().next() : "null");
        }

        try {
            ConditionParser freshParser = new ConditionParser(condition);
            return freshParser.evaluate(weaponTags);
        } catch (Exception e) {
            plugin.getLogger().warning("条件解析失败: " + condition + " - " + e.getMessage());
            return false;
        }
    }

    @Override
    public String toString() {
        if (isConditional()) {
            return String.format("(%s) %.2f %s", condition, value, calcType);
        } else {
            return String.format("%s %.2f %s", element, value, calcType);
        }
    }
}