package org.Spike.CSExtensions.Modifier.Mythic;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class MythicConfigManager {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private final Map<String, List<MythicEffect>> weaponEffects = new HashMap<>();
    private final Map<String, List<MythicEffect>> timerEffects = new HashMap<>();
    private final Map<String, Boolean> weaponHasMythicCache = new HashMap<>();
    private final Map<String, Set<TriggerType>> weaponTriggersCache = new HashMap<>();

    public MythicConfigManager(CSExtensions plugin, ModifierManager modifierManager) {
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
            Set<TriggerType> triggerTypes = new HashSet<>();
            List<MythicEffect> effects = new ArrayList<>();
            for (String configLine : mythicConfigs) {
                MythicEffect effect = MythicSkillParser.parseEffect(configLine, "@Self");
                if (effect != null) {
                    effects.add(effect);
                    triggerTypes.add(effect.getTrigger());

                    if (effect.getTrigger() == TriggerType.TIMER) {
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

    public List<MythicEffect> getEffects(String weaponId, TriggerType trigger) {
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

    public boolean hasTriggerType(String weaponId, TriggerType triggerType) {
        Set<TriggerType> triggers = weaponTriggersCache.get(weaponId);
        return triggers != null && triggers.contains(triggerType);
    }

    public void reload() {
        loadConfigs();
    }
}