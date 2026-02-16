package org.Spike.CSExtensions.Modifier.Trails;

public enum TrailWeaponType {
    PROJECTILES("projectiles"),
    ENERGY("energy");

    private final String configName;

    TrailWeaponType(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public static TrailWeaponType fromString(String name) {
        for (TrailWeaponType type : values()) {
            if (type.configName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return PROJECTILES;
    }
}