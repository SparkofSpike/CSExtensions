package org.Spike.CSExtensions.Modifier.Trails;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class TrailConfigManager {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private final Map<String, TrailConfig> trailConfigs = new HashMap<>();

    public TrailConfigManager(CSExtensions plugin, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.modifierManager = modifierManager;
        reload();
    }

    public void reload() {
        trailConfigs.clear();

        for (String weaponId : modifierManager.getLoadedWeaponIds()) {
            ConfigurationSection weaponConfig = modifierManager.getWeaponConfig(weaponId);
            if (weaponConfig == null) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Trails] 武器 " + weaponId + " 的配置为空");
                }
                continue;
            }

            if (weaponConfig.contains("Trails")) {
                ConfigurationSection trailsSection = weaponConfig.getConfigurationSection("Trails");
                if (trailsSection != null) {
                    TrailConfig config = parseTrailConfig(weaponId, trailsSection);
                    if (config != null) {
                        trailConfigs.put(weaponId, config);
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("[Trails] 成功加载武器 " + weaponId + " 的配置");
                            plugin.getLogger().info("[Trails]  特效: " + config.getEffects());
                            plugin.getLogger().info("[Trails]  长度: " + config.getLength());
                        }
                    } else {
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("[Trails] 解析武器 " + weaponId + " 的配置失败");
                        }
                    }
                } else {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Trails] 武器 " + weaponId + " 的Trails不是配置节");
                    }
                }
            } else {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Trails] 武器 " + weaponId + " 没有Trails配置");
                }
            }
        }

        plugin.getLogger().info("已加载 " + trailConfigs.size() + " 个武器的Trails配置");
    }

    private TrailConfig parseTrailConfig(String weaponId, ConfigurationSection trailsSection) {
        try {
            TrailConfig config = new TrailConfig(weaponId);

            if (!trailsSection.contains("WeaponType")) {
                plugin.getLogger().warning("武器 " + weaponId + " 缺少 WeaponType");
                return null;
            }
            config.setWeaponType(TrailWeaponType.fromString(trailsSection.getString("WeaponType")));

            if (!trailsSection.contains("Trail")) {
                plugin.getLogger().warning("武器 " + weaponId + " 缺少 Trail");
                return null;
            }
            List<String> effects = parseEffects(trailsSection.getString("Trail"));
            config.setEffects(effects);


            if (trailsSection.contains("Length")) {
                config.setLength(trailsSection.getInt("Length"));
            }

            if (trailsSection.contains("ParticleColor")) {
                config.setParticleColor(parseColor(trailsSection.getString("ParticleColor")));
            }

            if (trailsSection.contains("Speed")) {
                config.setSpeed(trailsSection.getDouble("Speed"));
            }

            if (trailsSection.contains("Amount")) {
                config.setAmount(trailsSection.getInt("Amount"));
            }

            if (trailsSection.contains("ExtraParticlesAhead")) {
                config.setExtraParticlesAhead(trailsSection.getInt("ExtraParticlesAhead"));
            }

            if (trailsSection.contains("ExtraParticlesInterval")) {
                config.setExtraParticlesInterval(trailsSection.getInt("ExtraParticlesInterval"));
            }

            if (trailsSection.contains("Advanced")) {
                ConfigurationSection advancedSection = trailsSection.getConfigurationSection("Advanced");
                if (advancedSection != null) {
                    if (advancedSection.contains("TrailType")) {
                        config.setTrailType(TrailType.fromString(advancedSection.getString("TrailType")));
                    }

                    if (advancedSection.contains("Radius")) {
                        config.setRadius(advancedSection.getDouble("Radius"));
                    }

                    if (advancedSection.contains("Points")) {
                        config.setPoints(advancedSection.getInt("Points"));
                    }

                    if (advancedSection.contains("GoThrough")) {
                        config.setGoThrough(GoThrough.fromString(advancedSection.getString("GoThrough")));
                    }
                }
            }

            return config;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "解析武器 " + weaponId + " 的Trails配置失败: ", e);
            return null;
        }
    }

    private List<String> parseEffects(String effectString) {
        List<String> effects = new ArrayList<>();
        if (effectString == null || effectString.isEmpty()) {
            return effects;
        }

        String[] parts = effectString.split(",");
        for (String part : parts) {
            effects.add(part.trim().toLowerCase());
        }

        return effects;
    }

    private Color parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            return null;
        }

        try {
            String[] parts = colorString.split("-");
            if (parts.length != 3) {
                return null;
            }

            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());

            r = Math.max(0, Math.min(255, r));
            g = Math.max(0, Math.min(255, g));
            b = Math.max(0, Math.min(255, b));

            return Color.fromRGB(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public TrailConfig getTrailConfig(String weaponId) {
        return trailConfigs.get(weaponId);
    }

    public boolean hasTrails(String weaponId) {
        return trailConfigs.containsKey(weaponId);
    }

    public void cleanup() {
        trailConfigs.clear();
    }

    public List<String> getLoadedWeaponIds() {
        return new ArrayList<>(trailConfigs.keySet());
    }
}