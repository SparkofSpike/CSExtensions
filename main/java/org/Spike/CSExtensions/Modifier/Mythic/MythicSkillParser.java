package org.Spike.CSExtensions.Modifier.Mythic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MythicSkillParser {
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("~on(\\w+)(?::(\\d+))?");
    private static final Pattern HEALTH_CONDITION_PATTERN = Pattern.compile("([><]=?|=|!=)?\\d+(\\.\\d+)?%?");

    public static MythicEffect parseEffect(String configLine, String targetSelector) {
        try {
            String[] parts = configLine.split("\\s+");

            String skillCommand = null;
            double chance = 1.0;
            TriggerType trigger = null;
            int timerTicks = 0;
            HealthCondition healthCondition = null;
            String finalTargetSelector = targetSelector;

                        skillCommand = parts[0];

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];

                if (part.startsWith("~")) {
                    Matcher triggerMatcher = TRIGGER_PATTERN.matcher(part);
                    if (triggerMatcher.find()) {
                        String triggerName = triggerMatcher.group(1).toUpperCase();
                        if (triggerName.equals("TIMER") && triggerMatcher.group(2) != null) {
                            timerTicks = Integer.parseInt(triggerMatcher.group(2));
                            trigger = TriggerType.TIMER;
                        } else {
                            try {
                                trigger = TriggerType.valueOf(triggerName);
                            } catch (IllegalArgumentException e) {
                                continue;
                            }
                        }
                    }
                }
                else if (part.equals("@Self") || part.equals("@victim") || part.equals("@hitlocation")) {
                    finalTargetSelector = part;
                }
                else if (isHealthCondition(part)) {
                    healthCondition = HealthCondition.parse(part);
                }
                else if (isNumeric(part)) {
                                        try {
                        double num = Double.parseDouble(part);
                                                if (!part.contains("=") && !part.contains(">") && !part.contains("<") && !part.contains("!")) {
                            chance = num;
                            if (chance > 1) chance = 1.0;
                            if (chance < 0) chance = 0.0;
                        }
                    } catch (NumberFormatException e) {
                                                skillCommand += " " + part;
                    }
                }
                else {
                                        skillCommand += " " + part;
                }
            }

            if (skillCommand == null || trigger == null) {
                return null;
            }

                        String fullSkillCommand = skillCommand;
            if (!skillCommand.contains("@Self") && !skillCommand.contains("@victim") && !skillCommand.contains("@hitlocation")) {
                fullSkillCommand += " " + finalTargetSelector;
            }

            return new MythicEffect(fullSkillCommand, chance, trigger, timerTicks, healthCondition, finalTargetSelector);

        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isHealthCondition(String str) {
        if (str == null) return false;
        str = str.trim();

        return str.startsWith(">") || str.startsWith("<") ||
                str.startsWith("=") || str.startsWith("!=") ||
                str.startsWith(">=") || str.startsWith("<=") ||
                (str.contains("%") && (str.contains(">") || str.contains("<") ||
                        str.contains("=") || str.contains("!=")));
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}