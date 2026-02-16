package org.Spike.CSExtensions.Modifier.Projectiles;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProjectilesManager {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private final Map<String, ProjectilesConfig> configs;

    private File configFile;
    private YamlConfiguration config;

    public ProjectilesManager(CSExtensions plugin, ModifierManager modifierManager) {
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

                ProjectilesConfig projConfig = parseProjectilesConfig(weaponSection);
                configs.put(weaponId, projConfig);
            }

            plugin.getLogger().info("已加载 " + configs.size() + " 个武器的Projectiles配置");

        } catch (Exception e) {
            plugin.getLogger().severe("加载Projectiles配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ProjectilesConfig parseProjectilesConfig(ConfigurationSection weaponSection) {
        ProjectilesConfig projConfig = new ProjectilesConfig();

        handleInheritance(weaponSection, projConfig);

        parseCurrentConfig(weaponSection, projConfig);

        return projConfig;
    }

    private void handleInheritance(ConfigurationSection weaponSection, ProjectilesConfig projConfig) {
        if (weaponSection.contains("Template")) {
            String templateId = weaponSection.getString("Template");

            if ("true".equalsIgnoreCase(templateId)) {
                return;
            }

            ConfigurationSection parentSection = config.getConfigurationSection(templateId);
            if (parentSection != null) {
                handleInheritance(parentSection, projConfig);

                parseCurrentConfig(parentSection, projConfig);
            }
        }
    }

    private void parseCurrentConfig(ConfigurationSection section, ProjectilesConfig projConfig) {
        if (section.contains("Projectiles.Hidden")) {
            projConfig.setHidden(section.getBoolean("Projectiles.Hidden"));
        }

        if (section.contains("Projectiles.Noknock")) {
            String noknockStr = section.getString("Projectiles.Noknock");
            projConfig.setNoknock(ProjectilesConfig.KnockbackType.fromString(noknockStr));
        }

        parsePenetrateConfig(section, projConfig.getPenetrate());

        parseReturnConfig(section, projConfig.getReturnConfig());

        parseBounceConfig(section, projConfig.getBounce());

        parseHomingConfig(section, projConfig.getHoming());

    }


    private void applyTemplateConfig(ProjectilesConfig projConfig, String templateId) {
        ConfigurationSection templateSection = modifierManager.getTemplateSection(templateId);
        if (templateSection == null) return;

        if (templateSection.contains("Template")) {
            String parentTemplateId = templateSection.getString("Template");
            if (!"true".equalsIgnoreCase(parentTemplateId)) {
                applyTemplateConfig(projConfig, parentTemplateId);
            }
        }

        if (templateSection.contains("Projectiles")) {
            ConfigurationSection projSection = templateSection.getConfigurationSection("Projectiles");
            if (projSection != null) {
                if (projSection.contains("Hidden")) {
                    projConfig.setHidden(projSection.getBoolean("Hidden"));
                }

                if (projSection.contains("Noknock")) {
                    String noknockStr = projSection.getString("Noknock");
                    projConfig.setNoknock(ProjectilesConfig.KnockbackType.fromString(noknockStr));
                }

                parsePenetrateConfig(projSection, projConfig.getPenetrate());

                parseReturnConfig(projSection, projConfig.getReturnConfig());

                parseBounceConfig(projSection, projConfig.getBounce());

                parseHomingConfig(projSection, projConfig.getHoming());

            }
        }
    }

    private void parsePenetrateConfig(ConfigurationSection section, ProjectilesConfig.PenetrateConfig penetrateConfig) {
        ConfigurationSection penetrateSection = getSubSection(section, "Projectiles.Penetrate");
        if (penetrateSection == null) return;

        if (penetrateSection.contains("Enabled")) {
            penetrateConfig.setEnabled(penetrateSection.getBoolean("Enabled"));
        }

        if (penetrateSection.contains("Number")) {
            penetrateConfig.setNumber(penetrateSection.getInt("Number"));
        }

        if (penetrateSection.contains("VelocityCoef")) {
            penetrateConfig.setVelocityCoef(penetrateSection.getDouble("VelocityCoef"));
        }

        if (penetrateSection.contains("DamageCoef")) {
            penetrateConfig.setDamageCoef(penetrateSection.getDouble("DamageCoef"));
        }

    }

    private void parseReturnConfig(ConfigurationSection section, ProjectilesConfig.ReturnConfig returnConfig) {
        ConfigurationSection returnSection = getSubSection(section, "Projectiles.Return");
        if (returnSection == null) return;

        if (returnSection.contains("Enabled")) {
            returnConfig.setEnabled(returnSection.getBoolean("Enabled"));
        }

        if (returnSection.contains("Acceleration")) {
            returnConfig.setAcceleration(returnSection.getDouble("Acceleration"));
        }

        if (returnSection.contains("TrackShooter")) {
            returnConfig.setTrackShooter(returnSection.getBoolean("TrackShooter"));
        }

        if (returnSection.contains("TriggerOnBlock")) {
            returnConfig.setTriggerOnBlock(returnSection.getBoolean("TriggerOnBlock"));
        }
    }

    private void parseBounceConfig(ConfigurationSection section, ProjectilesConfig.BounceConfig bounceConfig) {
        ConfigurationSection bounceSection = getSubSection(section, "Projectiles.Bounce");
        if (bounceSection == null) return;

        if (bounceSection.contains("Enabled")) {
            bounceConfig.setEnabled(bounceSection.getBoolean("Enabled"));
        }

        if (bounceSection.contains("MaxBounces")) {
            bounceConfig.setMaxBounces(bounceSection.getInt("MaxBounces"));
        }

        if (bounceSection.contains("VelocityCoef")) {
            bounceConfig.setVelocityCoef(bounceSection.getDouble("VelocityCoef"));
        }

        if (bounceSection.contains("DamageCoef")) {
            bounceConfig.setDamageCoef(bounceSection.getDouble("DamageCoef"));
        }

        if (bounceSection.contains("RandomAngle")) {
            bounceConfig.setRandomAngle(bounceSection.getDouble("RandomAngle"));
        }

        if (bounceSection.contains("AutoAimNearest")) {
            bounceConfig.setAutoAimNearest(bounceSection.getBoolean("AutoAimNearest"));
        }

        if (bounceSection.contains("AutoAimRadius")) {
            bounceConfig.setAutoAimRadius(bounceSection.getDouble("AutoAimRadius"));
        }

    }

    private void parseHomingConfig(ConfigurationSection section, ProjectilesConfig.HomingConfig homingConfig) {
        ConfigurationSection homingSection = getSubSection(section, "Projectiles.Homing");
        if (homingSection == null) return;

        if (homingSection.contains("Enabled")) {
            homingConfig.setEnabled(homingSection.getBoolean("Enabled"));
        }

        if (homingSection.contains("Range")) {
            homingConfig.setRange(homingSection.getDouble("Range"));
        }

        if (homingSection.contains("Angle")) {
            homingConfig.setAngle(homingSection.getDouble("Angle"));
        }

        if (homingSection.contains("TurnSpeed")) {
            homingConfig.setTurnSpeed(homingSection.getDouble("TurnSpeed"));
        }

        if (homingSection.contains("Update")) {
            String updateStr = homingSection.getString("Update");
            homingConfig.setUpdate(ProjectilesConfig.HomingUpdate.fromString(updateStr));
        }

        if (homingSection.contains("NoBlockBetween")) {
            homingConfig.setNoBlockBetween(homingSection.getBoolean("NoBlockBetween"));
        }

        if (homingSection.contains("InitialLock")) {
            homingConfig.setInitialLock(homingSection.getBoolean("InitialLock"));
        }

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

    public boolean hasProjectilesConfig(String weaponTitle) {
        return configs.containsKey(weaponTitle);
    }

    public ProjectilesConfig getProjectilesConfig(String weaponTitle) {
        return configs.get(weaponTitle);
    }

    public void reload() {
        loadConfig();
    }

    public void cleanup() {
        configs.clear();
    }
}