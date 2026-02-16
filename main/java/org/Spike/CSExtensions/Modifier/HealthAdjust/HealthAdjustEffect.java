package org.Spike.CSExtensions.Modifier.HealthAdjust;

public class HealthAdjustEffect {
    private final double amountPerTick;
    private final int durationTicks;
    private final HealthAdjustConfig.Trigger trigger;
    private final boolean isHealing;
    private final boolean trueDamage;

    public HealthAdjustEffect(double amountPerTick, int durationTicks,
                              HealthAdjustConfig.Trigger trigger, boolean isHealing, boolean trueDamage) {
        this.amountPerTick = amountPerTick;
        this.durationTicks = durationTicks;
        this.trigger = trigger;
        this.isHealing = isHealing;
        this.trueDamage = trueDamage;
    }

    public static HealthAdjustEffect fromString(String configString, boolean isHealing) {
        if (configString == null || configString.trim().isEmpty()) {
            return null;
        }

        String[] parts = configString.split("-");
        boolean trueDamage = false;

        try {
            if (isHealing) {
                if (parts.length == 2) {
                    // amount-trigger
                    double amount = Double.parseDouble(parts[0]);
                    HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[1]);
                    if (trigger == null) return null;
                    return new HealthAdjustEffect(amount, 0, trigger, true, false);

                } else if (parts.length == 3) {
                    // amount-duration-trigger
                    double amountPerTick = Double.parseDouble(parts[0]);
                    int duration = Integer.parseInt(parts[1]);
                    HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[2]);
                    if (trigger == null) return null;
                    return new HealthAdjustEffect(amountPerTick, duration, trigger, true, false);
                }

            } else {
                if (parts.length >= 2) {
                    int trueDamageIndex = -1;
                    if (parts.length >= 3) {
                        String lastPart = parts[parts.length - 1];
                        if (isBooleanString(lastPart)) {
                            trueDamage = parseBoolean(lastPart);
                            trueDamageIndex = parts.length - 1;
                        }
                    }

                    if (trueDamageIndex != -1) {
                        if (parts.length == 3) {
                            // amount-trigger-trueDamage
                            double amount = Double.parseDouble(parts[0]);
                            HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[1]);
                            if (trigger == null) return null;
                            return new HealthAdjustEffect(amount, 0, trigger, false, trueDamage);

                        } else if (parts.length == 4) {
                            // amount-duration-trigger-trueDamage
                            double amountPerTick = Double.parseDouble(parts[0]);
                            int duration = Integer.parseInt(parts[1]);
                            HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[2]);
                            if (trigger == null) return null;
                            return new HealthAdjustEffect(amountPerTick, duration, trigger, false, trueDamage);
                        }

                    } else {
                        if (parts.length == 2) {
                            try {
                                // amount-trigger
                                double amount = Double.parseDouble(parts[0]);
                                HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[1]);
                                if (trigger != null) {
                                    return new HealthAdjustEffect(amount, 0, trigger, false, false);
                                }
                            } catch (IllegalArgumentException e1) {
                                // amount-duration
                                try {
                                    double amountPerTick = Double.parseDouble(parts[0]);
                                    int duration = Integer.parseInt(parts[1]);
                                    return new HealthAdjustEffect(amountPerTick, duration,
                                            HealthAdjustConfig.Trigger.HIT, false, false);
                                } catch (NumberFormatException e2) {
                                    return null;
                                }
                            }

                        } else if (parts.length == 3) {
                            // amount-duration-trigger
                            double amountPerTick = Double.parseDouble(parts[0]);
                            int duration = Integer.parseInt(parts[1]);
                            HealthAdjustConfig.Trigger trigger = HealthAdjustConfig.Trigger.fromString(parts[2]);
                            if (trigger == null) return null;
                            return new HealthAdjustEffect(amountPerTick, duration, trigger, false, false);
                        }
                    }
                }
            }

            return null;

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBooleanString(String str) {
        if (str == null) return false;
        String lower = str.toLowerCase();
        return lower.equals("true") || lower.equals("false") ||
                lower.equals("yes") || lower.equals("no") ||
                lower.equals("1") || lower.equals("0");
    }

    private static boolean parseBoolean(String str) {
        if (str == null) return false;
        return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") || str.equals("1");
    }


    public static double parseInstantValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getAmountPerTick() {
        return amountPerTick;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public HealthAdjustConfig.Trigger getTrigger() {
        return trigger;
    }

    public boolean isHealing() {
        return isHealing;
    }

    public boolean isTrueDamage() {
        return trueDamage;
    }

    public boolean isInstant() {
        return durationTicks == 0;
    }

    public double getTotalAmount() {
        if (isInstant()) {
            return amountPerTick;
        } else {
            return amountPerTick * durationTicks;
        }
    }
}