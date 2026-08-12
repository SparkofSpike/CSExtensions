package org.Spike.CSExtensions.Modifier.HealthAdjust;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealthAdjustManager {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private final Map<String, HealthAdjustConfig> configs;

    private File configFile;
    private YamlConfiguration config;

    public HealthAdjustManager(CSExtensions plugin, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.modifierManager = modifierManager;
        this.configs = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        try {
            configFile = new File(plugin.getDataFolder(), "cse_guns.yml");
            if (!configFile.exists()) {
                plugin.saveResource("cse_guns.yml", false);
            }

            config = YamlConfiguration.loadConfiguration(configFile);
            configs.clear();

            for (String weaponId : config.getKeys(false)) {
                ConfigurationSection weaponSection = config.getConfigurationSection(weaponId);
                if (weaponSection == null) continue;

                HealthAdjustConfig healthConfig = parseHealthAdjustConfig(weaponSection);
                configs.put(weaponId, healthConfig);
            }

            plugin.getLogger().info("已加载 " + configs.size() + " 个武器的HealthAdjust配置");

        } catch (Exception e) {
            plugin.getLogger().severe("加载HealthAdjust配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HealthAdjustConfig parseHealthAdjustConfig(ConfigurationSection weaponSection) {
        HealthAdjustConfig healthConfig = new HealthAdjustConfig();

        handleInheritance(weaponSection, healthConfig);

        parseCurrentConfig(weaponSection, healthConfig);

        return healthConfig;
    }

    private void handleInheritance(ConfigurationSection weaponSection, HealthAdjustConfig healthConfig) {
        handleInheritance(weaponSection, healthConfig, new HashSet<>());
    }

    private void handleInheritance(ConfigurationSection weaponSection, HealthAdjustConfig healthConfig,
                                   Set<String> visited) {
        if (weaponSection.contains("Template")) {
            String templateId = weaponSection.getString("Template");

            if ("true".equalsIgnoreCase(templateId)) {
                return;
            }

            if (!visited.add(templateId)) {
                plugin.getLogger().warning("检测到HealthAdjust模板循环引用: " + templateId);
                return;
            }

            ConfigurationSection parentSection = config.getConfigurationSection(templateId);
            if (parentSection != null) {
                handleInheritance(parentSection, healthConfig, visited);

                parseCurrentConfig(parentSection, healthConfig);
            }
        }
    }

    private void parseCurrentConfig(ConfigurationSection section, HealthAdjustConfig healthConfig) {
        ConfigurationSection healthSection = getSubSection(section, "HealthAdjust");
        if (healthSection == null) {
            return;
        }

        if (healthSection.contains("Enabled")) {
            healthConfig.setEnabled(healthSection.getBoolean("Enabled"));
        }

        if (!healthConfig.isEnabled()) {
            return;
        }

        if (healthSection.contains("HealSelfInstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("HealSelfInstant"), true);
            healthConfig.setHealSelfInstant(effects);
        }

        if (healthSection.contains("DamageSelfInstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("DamageSelfInstant"), false);
            healthConfig.setDamageSelfInstant(effects);
        }

        if (healthSection.contains("HealSelfConstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("HealSelfConstant"), true);
            healthConfig.setHealSelfConstant(effects);
        }

        if (healthSection.contains("DamageSelfConstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("DamageSelfConstant"), false);
            healthConfig.setDamageSelfConstant(effects);
        }

        if (healthSection.contains("HealTargetInstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("HealTargetInstant"), true);
            healthConfig.setHealTargetInstant(effects);
        }

        if (healthSection.contains("DamageTargetInstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("DamageTargetInstant"), false);
            healthConfig.setDamageTargetInstant(effects);
        }

        if (healthSection.contains("HealTargetConstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("HealTargetConstant"), true);
            healthConfig.setHealTargetConstant(effects);
        }

        if (healthSection.contains("DamageTargetConstant")) {
            List<HealthAdjustEffect> effects = parseEffectList(healthSection.getString("DamageTargetConstant"), false);
            healthConfig.setDamageTargetConstant(effects);
        }
    }

    private List<HealthAdjustEffect> parseEffectList(String configString, boolean isHealing) {
        List<HealthAdjustEffect> effects = new ArrayList<>();

        if (configString == null || configString.trim().isEmpty()) {
            return effects;
        }

        String[] effectStrings = configString.split(",");
        for (String effectStr : effectStrings) {
            HealthAdjustEffect effect = HealthAdjustEffect.fromString(effectStr.trim(), isHealing);
            if (effect != null) {
                effects.add(effect);
            } else {
                plugin.getLogger().warning("解析HealthAdjust效果失败: " + effectStr);
            }
        }

        return effects;
    }

    private ConfigurationSection getSubSection(ConfigurationSection parent, String path) {
        String[] parts = path.split("\\.");
        ConfigurationSection current = parent;

        for (String part : parts) {
            if (current.contains(part)) {
                if (current.isConfigurationSection(part)) {
                    current = current.getConfigurationSection(part);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    public boolean hasHealthAdjustConfig(String weaponTitle) {
        HealthAdjustConfig config = configs.get(weaponTitle);
        return config != null && config.isEnabled() && config.hasAnyEffect();
    }

    public HealthAdjustConfig getHealthAdjustConfig(String weaponTitle) {
        return configs.get(weaponTitle);
    }

    public void reload() {
        loadConfig();
    }

    public void cleanup() {
        configs.clear();
    }
}