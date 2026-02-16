package org.Spike.CSExtensions.Modifier.Trails;

public enum TrailType {
    STRAIGHT,
    CIRCLE;

    public static TrailType fromString(String name) {
        try {
            return TrailType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STRAIGHT;
        }
    }
}