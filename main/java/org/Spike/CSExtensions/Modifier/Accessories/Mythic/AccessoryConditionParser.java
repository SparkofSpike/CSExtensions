package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import java.util.HashSet;
import java.util.Set;

public class AccessoryConditionParser {

    public static AccessoryCondition parseCondition(String conditionStr) {
        if (conditionStr == null || conditionStr.trim().isEmpty()) {
            return new AccessoryCondition(true, new HashSet<>(), true);
        }

        conditionStr = conditionStr.trim().toLowerCase();

        boolean isAnd = conditionStr.contains("&&");
        boolean isOr = conditionStr.contains("||");

        if (!isAnd && !isOr) {
            Set<String> tags = new HashSet<>();
            tags.add(conditionStr);
            return new AccessoryCondition(true, tags, true);
        }

        String[] parts;
        if (isAnd) {
            parts = conditionStr.split("&&");
        } else {
            parts = conditionStr.split("\\|\\|");
        }

        Set<String> tags = new HashSet<>();
        for (String part : parts) {
            tags.add(part.trim());
        }

        return new AccessoryCondition(!tags.isEmpty(), tags, isAnd);
    }

    public static class AccessoryCondition {
        private final boolean hasCondition;
        private final Set<String> requiredTags;
        private final boolean isAndLogic;

        public AccessoryCondition(boolean hasCondition, Set<String> requiredTags, boolean isAndLogic) {
            this.hasCondition = hasCondition;
            this.requiredTags = requiredTags;
            this.isAndLogic = isAndLogic;
        }

        public boolean check(Set<String> weaponTags) {
            if (!hasCondition) return true;

            if (isAndLogic) {
                return weaponTags.containsAll(requiredTags);
            } else {
                for (String tag : requiredTags) {
                    if (weaponTags.contains(tag)) return true;
                }
                return false;
            }
        }
        public Set<String> getRequiredTags() {
            return requiredTags;
        }

        public boolean isAndLogic() {
            return isAndLogic;
        }
    }
}