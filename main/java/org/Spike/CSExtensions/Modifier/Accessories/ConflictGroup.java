package org.Spike.CSExtensions.Modifier.Accessories;

import java.util.HashSet;
import java.util.Set;

public class ConflictGroup {
    private final String name;
    private final int limit;
    private final String exceededMessage;
    private final Set<String> includedAccessories;

    public ConflictGroup(String name, int limit, String exceededMessage, Set<String> includedAccessories) {
        this.name = name;
        this.limit = limit;
        this.exceededMessage = exceededMessage;
        this.includedAccessories = includedAccessories;
    }

    public String getName() { return name; }
    public int getLimit() { return limit; }
    public String getExceededMessage() { return exceededMessage; }
    public Set<String> getIncludedAccessories() { return includedAccessories; }

    public boolean containsAccessory(String accessoryId) {
        return includedAccessories.contains(accessoryId);
    }

    public Set<String> checkConflict(Set<String> equippedIds) {
        Set<String> conflictIds = new HashSet<>();

        int count = 0;
        for (String accessoryId : equippedIds) {
            if (containsAccessory(accessoryId)) {
                conflictIds.add(accessoryId);
                count++;
            }
        }

        if (count > limit) {
            return conflictIds;
        }

        return new HashSet<>();
    }
}