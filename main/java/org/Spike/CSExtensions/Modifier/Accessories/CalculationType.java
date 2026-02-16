package org.Spike.CSExtensions.Modifier.Accessories;

public enum CalculationType {
    ADD,
    MUL,
    FLAT,
    FIX;

    public static CalculationType fromString(String type) {
        if (type == null) return null;

        switch (type.toLowerCase()) {
            case "add": return ADD;
            case "mul": return MUL;
            case "flat": return FLAT;
            case "fix": return FIX;
            default: return null;
        }
    }
}