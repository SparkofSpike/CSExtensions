package org.Spike.CSExtensions.Modifier.Mythic.accessory;

import org.bukkit.configuration.ConfigurationSection;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Mythic.core.HealthCondition;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicEffect;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicTrigger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessoryMythicConfig {
    private final CSExtensions plugin;
    private final Map<String, List<MythicEffect>> accessoryEffects = new HashMap<>();
    private final Map<String, List<MythicEffect>> timerEffects = new HashMap<>();

    private static final Pattern TRIGGER_PATTERN = Pattern.compile("~on(\\w+)(?::(\\d+))?");

    public AccessoryMythicConfig(CSExtensions plugin) {
        this.plugin = plugin;
    }

    public void loadConfig(ConfigurationSection accessorySection, String accessoryId) {
        List<String> mythicConfigs;

        if (accessorySection.contains("Mythic")) {
            mythicConfigs = accessorySection.getStringList("Mythic");
        }
        else if (accessorySection.contains("mythic")) {
            mythicConfigs = accessorySection.getStringList("mythic");
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[饰品配置] 使用小写mythic配置: " + accessoryId);
            }
        }
        else {
            return;
        }
        List<MythicEffect> effects = new ArrayList<>();

        for (String configLine : mythicConfigs) {
            MythicEffect effect = parseEffect(configLine);
            if (effect != null) {
                effects.add(effect);
                if (effect.getTrigger() == MythicTrigger.TIMER) {
                    timerEffects.computeIfAbsent(accessoryId, k -> new ArrayList<>()).add(effect);
                }
            }
        }

        if (!effects.isEmpty()) {
            accessoryEffects.put(accessoryId, effects);
        }
    }
    private MythicEffect parseEffect(String configLine) {
        String[] parts = configLine.split("\\s+");
        if (parts.length < 3) return null;

        String skillName = parts[0];
        String targetSelector = "@self";
        MythicTrigger trigger = null;
        double chance = 1.0;
        int timerTicks = 0;
        HealthCondition healthCondition = null;
        String elementCondition = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];

            if (part.startsWith("~")) {
                Matcher matcher = TRIGGER_PATTERN.matcher(part);
                if (matcher.find()) {
                    if ("TIMER".equalsIgnoreCase(matcher.group(1)) && matcher.group(2) != null) {
                        timerTicks = Integer.parseInt(matcher.group(2));
                        trigger = MythicTrigger.TIMER;
                    } else {
                        trigger = MythicTrigger.fromString(part);
                    }
                }
            } else if (part.equals("@self") || part.equals("@victim") ||
                    part.equals("@trigger") || part.equals("@hitlocation")) {
                targetSelector = part;
            } else if (part.contains(">") || part.contains("<") ||
                    part.contains("=") || part.contains("!")) {
                healthCondition = HealthCondition.parse(part);
            } else {
                try {
                    double num = Double.parseDouble(part);
                    if (num <= 1.0) {
                        chance = num;
                        continue;
                    }
                } catch (NumberFormatException e) {
                }

                elementCondition = part;
            }
        }

        if (trigger == null) return null;

        return new MythicEffect(skillName, targetSelector, trigger, chance,
                timerTicks, healthCondition, elementCondition);
    }

    public List<MythicEffect> getEffects(String accessoryId, MythicTrigger trigger) {
        List<MythicEffect> allEffects = accessoryEffects.get(accessoryId);
        if (allEffects == null) return Collections.emptyList();

        List<MythicEffect> result = new ArrayList<>();
        for (MythicEffect effect : allEffects) {
            if (effect.getTrigger() == trigger) {
                result.add(effect);
            }
        }
        return result;
    }

    public Map<String, List<MythicEffect>> getTimerEffects() {
        return timerEffects;
    }

    public void clear() {
        accessoryEffects.clear();
        timerEffects.clear();
    }
}