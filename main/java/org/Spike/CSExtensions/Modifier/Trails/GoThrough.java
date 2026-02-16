package org.Spike.CSExtensions.Modifier.Trails;

public enum GoThrough {
    NONE("none"),
    WALLS("walls"),
    PLAYERS("players"),
    ALL("all");

    private final String configName;

    GoThrough(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public static GoThrough fromString(String name) {
        for (GoThrough type : values()) {
            if (type.configName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return NONE;
    }

    public boolean canGoThroughWalls() {
        return this == WALLS || this == ALL;
    }

    public boolean canGoThroughPlayers() {
        return this == PLAYERS || this == ALL;
    }

    public boolean canGoThroughAny() {
        return this == ALL;
    }
}