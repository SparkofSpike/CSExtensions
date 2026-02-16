package org.Spike.CSExtensions.Modifier.SpikeElements;

public enum CalculationType {
    ADD,
    MUL,
    ULTIADD,
    ULTIMUL,
    FIX;

    public static CalculationType fromString(String type) {
        if (type == null) return null;

        switch (type.toUpperCase()) {
            case "ADD": return ADD;
            case "MUL": return MUL;
            case "ULTIADD": return ULTIADD;
            case "ULTIMUL": return ULTIMUL;
            case "FIX": return FIX;
            default: return null;
        }
    }
}