package org.Spike.CSExtensions.Modifier;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TemplateManager {
    private final CSExtensions plugin;
    private final Map<String, ConfigurationSection> allSections = new HashMap<>();
    private final Map<String, ConfigurationSection> configCache = new HashMap<>();
    private final Set<String> resolving = new HashSet<>();

    public TemplateManager(CSExtensions plugin) {
        this.plugin = plugin;
    }

    public void loadAllSections(FileConfiguration config) {
        clearCache();
        allSections.clear();

        int totalSections = 0;
        int validSections = 0;

        for (String key : config.getKeys(false)) {
            totalSections++;
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                allSections.put(key, section);
                validSections++;

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[TemplateManager] 加载配置节: " + key);
                    if (section.contains("Template")) {
                        plugin.getLogger().info("[TemplateManager]   -> Template: " + section.getString("Template"));
                    }
                    if (section.contains("Trails")) {
                        plugin.getLogger().info("[TemplateManager]   -> 有Trails配置");
                    }
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[TemplateManager] 加载了 " + validSections + "/" + totalSections + " 个配置节");
        }
    }

    public void clearCache() {
        int cacheSize = configCache.size();
        configCache.clear();
        resolving.clear();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[TemplateManager] 清除缓存，原有缓存大小: " + cacheSize);
        }
    }


    public ConfigurationSection getWeaponConfig(String weaponId) {
        if (configCache.containsKey(weaponId)) {
            ConfigurationSection cached = configCache.get(weaponId);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[TemplateManager] 使用缓存配置: " + weaponId);
            }
            return cached;
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[TemplateManager] 开始解析配置: " + weaponId);
        }

        if (!allSections.containsKey(weaponId)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("[TemplateManager] 配置不存在: " + weaponId);
            }
            return null;
        }

        if (!resolving.add(weaponId)) {
            plugin.getLogger().warning("[TemplateManager] 检测到模板循环引用: " + weaponId + "，使用自身原始配置");
            return allSections.get(weaponId);
        }

        ConfigurationSection result = null;
        ConfigurationSection weaponSection = allSections.get(weaponId);

        if (weaponSection.contains("Template")) {
            String templateId = weaponSection.getString("Template");

            if ("true".equalsIgnoreCase(templateId)) {
                result = weaponSection;
            } else if (templateId != null && !templateId.isEmpty() && allSections.containsKey(templateId)) {
                ConfigurationSection templateConfig = getWeaponConfig(templateId);

                if (templateConfig != null) {
                    ConfigurationSection mergedSection = new org.bukkit.configuration.file.YamlConfiguration();
                    copySection(templateConfig, mergedSection);
                    mergeSections(weaponSection, mergedSection);
                    mergedSection.set("Template", null);
                    result = mergedSection;

                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[TemplateManager] " + weaponId + " 继承了 " + templateId + " 的配置");
                    }
                } else {
                    plugin.getLogger().warning("[TemplateManager] 模板配置获取失败: " + templateId);
                }
            } else {
                plugin.getLogger().warning("[TemplateManager] 无效的模板引用: " + weaponId + " -> " + templateId);
            }
        } else {
            result = weaponSection;
        }

        if (result != null) {
            configCache.put(weaponId, result);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[TemplateManager] 缓存配置: " + weaponId + " (键数: " + result.getKeys(false).size() + ")");
            }
        } else {
            plugin.getLogger().warning("[TemplateManager] 配置解析结果为null: " + weaponId);
        }

        return result;
    }

    public java.util.List<String> getWeaponIds() {
        java.util.List<String> weaponIds = new java.util.ArrayList<>();

        for (Map.Entry<String, ConfigurationSection> entry : allSections.entrySet()) {
            ConfigurationSection section = entry.getValue();

            if (section.contains("Template")) {
                String templateValue = section.getString("Template");
                if ("true".equalsIgnoreCase(templateValue)) {
                    continue;
                }
            }

            weaponIds.add(entry.getKey());
        }

        return weaponIds;
    }

    public boolean isTemplate(String id) {
        if (!allSections.containsKey(id)) {
            return false;
        }

        ConfigurationSection section = allSections.get(id);
        if (section.contains("Template")) {
            String templateValue = section.getString("Template");
            return "true".equalsIgnoreCase(templateValue);
        }

        return false;
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection childSection = target.createSection(key);
                copySection((ConfigurationSection) value, childSection);
            } else {
                target.set(key, value);
            }
        }
    }

    private void mergeSections(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            Object sourceValue = source.get(key);
            Object targetValue = target.get(key);

            if (sourceValue instanceof ConfigurationSection && targetValue instanceof ConfigurationSection) {
                mergeSections((ConfigurationSection) sourceValue, (ConfigurationSection) targetValue);
            } else {
                target.set(key, sourceValue);
            }
        }
    }

    public void clear() {
        allSections.clear();
    }
}