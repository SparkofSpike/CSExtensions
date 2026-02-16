package org.Spike.CSExtensions.Modifier;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

public class ModifierManager {
    private final CSExtensions plugin;
    private final TemplateManager templateManager;
    private File gunsFile;
    private FileConfiguration gunsConfig;

    public ModifierManager(CSExtensions plugin) {
        this.plugin = plugin;
        this.templateManager = new TemplateManager(plugin);
        loadConfig();
    }

        public void loadConfig() {
        try {
            gunsFile = new File(plugin.getDataFolder(), "cse_guns.yml");
            if (!gunsFile.exists()) {
                plugin.saveResource("cse_guns.yml", false);
                plugin.getLogger().info("已创建默认 cse_guns.yml 配置文件");
            }

            gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
            reload();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载Modifier配置文件失败: ", e);
        }
    }

        public void reload() {
        Bukkit.getLogger().info("[ModifierManager] 开始重载配置...");

        try {
                        gunsFile = new File(plugin.getDataFolder(), "cse_guns.yml");
            Bukkit.getLogger().info("[ModifierManager] 配置文件路径: " + gunsFile.getAbsolutePath());
            Bukkit.getLogger().info("[ModifierManager] 文件存在: " + gunsFile.exists());

            if (!gunsFile.exists()) {
                plugin.saveResource("cse_guns.yml", false);
                Bukkit.getLogger().info("[ModifierManager] 已创建默认配置文件");
            }

                        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
            Bukkit.getLogger().info("[ModifierManager] 文件大小: " + gunsFile.length() + " 字节");

            templateManager.clearCache();
            templateManager.clear();
            templateManager.loadAllSections(gunsConfig);

            int weaponCount = templateManager.getWeaponIds().size();
            int templateCount = 0;

                        for (String key : gunsConfig.getKeys(false)) {
                if (templateManager.isTemplate(key)) {
                    templateCount++;
                }
            }

            plugin.getLogger().info("[ModifierManager] 重载完成");
            plugin.getLogger().info("[ModifierManager] 已加载 " + weaponCount + " 个武器配置");
            plugin.getLogger().info("[ModifierManager] 已加载 " + templateCount + " 个模板");

                        if (plugin.getConfig().getBoolean("debug", false)) {
                for (String weaponId : templateManager.getWeaponIds()) {
                    ConfigurationSection config = getWeaponConfig(weaponId);
                    Bukkit.getLogger().info("[ModifierManager] 武器: " + weaponId);
                    if (config != null) {
                        Bukkit.getLogger().info("[ModifierManager]   -> 配置节: " + config.getKeys(false).size() + " 个键");
                        if (config.contains("Trails")) {
                            Bukkit.getLogger().info("[ModifierManager]   -> 有Trails配置");
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[ModifierManager] 重载配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

        public ConfigurationSection getWeaponConfig(String weaponId) {
        return templateManager.getWeaponConfig(weaponId);
    }

        public boolean hasModifier(String weaponId, String modifierPath) {
        ConfigurationSection config = getWeaponConfig(weaponId);
        return config != null && config.contains(modifierPath);
    }

        public ConfigurationSection getModifierConfig(String weaponId, String modifierPath) {
        ConfigurationSection config = getWeaponConfig(weaponId);
        if (config == null || !config.contains(modifierPath)) {
            return null;
        }
        return config.getConfigurationSection(modifierPath);
    }

        public java.util.List<String> getLoadedWeaponIds() {
        return templateManager.getWeaponIds();
    }

        public TemplateManager getTemplateManager() {
        return templateManager;
    }

        public void saveConfig() {
        try {
            if (gunsConfig != null && gunsFile != null) {
                gunsConfig.save(gunsFile);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "保存Modifier配置文件失败: ", e);
        }
    }

        public ConfigurationSection getTemplateSection(String templateId) {
        if (templateId == null || templateId.isEmpty()) {
            return null;
        }


                if (gunsConfig != null && gunsConfig.contains(templateId)) {
            ConfigurationSection templateSection = gunsConfig.getConfigurationSection(templateId);

                        if (templateSection != null && templateSection.contains("Template")) {
                String templateValue = templateSection.getString("Template");
                if ("true".equalsIgnoreCase(templateValue)) {
                    return templateSection;
                }
            }
        }

        return null;
    }

    public String getWeaponBarMessageGroup(String weaponId) {
        ConfigurationSection weaponConfig = getWeaponConfig(weaponId);
        if (weaponConfig == null) {
            return null;
        }

        return weaponConfig.getString("BarMessageGroup");
    }


}