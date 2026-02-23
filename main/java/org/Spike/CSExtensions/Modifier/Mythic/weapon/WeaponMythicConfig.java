package org.Spike.CSExtensions.Modifier.Mythic.weapon;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.Spike.CSExtensions.Modifier.Mythic.core.HealthCondition;
import org.bukkit.configuration.ConfigurationSection;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicEffect;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicTrigger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeaponMythicHandler {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private final Map<String, List<MythicEffect>> weaponEffects = new HashMap<>();
    private final Map<String, List<MythicEffect>> timerEffects = new HashMap<>();
    private final Map<String, Boolean> weaponHasMythicCache = new HashMap<>();
    private final Map<String, Set<MythicTrigger>> weaponTriggersCache = new HashMap<>();

    private static final Pattern TRIGGER_PATTERN = Pattern.compile("~on(\\w+)(?::(\\d+))?");

    public WeaponMythicHandler(CSExtensions plugin, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.modifierManager = modifierManager;
        loadConfigs();
    }

    public void loadConfigs() {
        weaponEffects.clear();
        timerEffects.clear();
        weaponHasMythicCache.clear();
        weaponTriggersCache.clear();

        for (String weaponId : modifierManager.getLoadedWeaponIds()) {
            ConfigurationSection weaponConfig = modifierManager.getWeaponConfig(weaponId);
            if (weaponConfig == null || !weaponConfig.contains("Mythic")) {
                continue;
            }

            List<String> mythicConfigs = weaponConfig.getStringList("Mythic");
            if (mythicConfigs.isEmpty()) {
                weaponHasMythicCache.put(weaponId, false);
                continue;
            }

            weaponHasMythicCache.put(weaponId, true);
            Set<MythicTrigger> triggerTypes = new HashSet<>();
            List<MythicEffect> effects = new ArrayList<>();

            for (String configLine : mythicConfigs) {
                MythicEffect effect = parseEffect(configLine);
                if (effect != null) {
                    effects.add(effect);
                    triggerTypes.add(effect.getTrigger());

                    if (effect.getTrigger() == MythicTrigger.TIMER) {
                        timerEffects.computeIfAbsent(weaponId, k -> new ArrayList<>()).add(effect);
                    }
                }
            }

            if (!effects.isEmpty()) {
                weaponEffects.put(weaponId, effects);
                weaponTriggersCache.put(weaponId, triggerTypes);
            }
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
                    String triggerName = matcher.group(1).toUpperCase();
                    if ("TIMER".equals(triggerName) && matcher.group(2) != null) {
                        timerTicks = Integer.parseInt(matcher.group(2));
                        trigger = MythicTrigger.TIMER;
                    } else {
                        try {
                            trigger = MythicTrigger.valueOf(triggerName);
                        } catch (IllegalArgumentException e) {
                            // 忽略
                        }
                    }
                }
            } else if (part.equals("@self") || part.equals("@victim") || part.equals("@trigger") || part.equals("@hitlocation")) {
                targetSelector = part;
            } else if (part.contains(">") || part.contains("<") || part.contains("=") || part.contains("!")) {
                healthCondition = HealthCondition.parse(part);
            } else if (part.contains("&&") || part.contains("||") || part.contains("(") || part.contains(")")) {
                elementCondition = part;
            } else {
                try {
                    double num = Double.parseDouble(part);
                    if (num <= 1.0) chance = num;
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }

        if (trigger == null) return null;

        return new MythicEffect(skillName, targetSelector, trigger, chance,
                timerTicks, healthCondition, elementCondition);
    }

    public List<MythicEffect> getEffects(String weaponId, MythicTrigger trigger) {
        List<MythicEffect> allEffects = weaponEffects.get(weaponId);
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

    public boolean hasMythicConfig(String weaponId) {
        return weaponHasMythicCache.getOrDefault(weaponId, false);
    }

    public boolean hasTriggerType(String weaponId, MythicTrigger triggerType) {
        Set<MythicTrigger> triggers = weaponTriggersCache.get(weaponId);
        return triggers != null && triggers.contains(triggerType);
    }

    public void reload() {
        loadConfigs();
    }
}