package org.Spike.CSExtensions.Modifier.SpikeElements;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class WeaponTagReader {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;

    private final Map<String, Set<String>> weaponTagsCache = new HashMap<>();

    public WeaponTagReader(CSExtensions plugin, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.modifierManager = modifierManager;
    }

    public Set<String> getWeaponTags(String weaponId) {
        if (weaponTagsCache.containsKey(weaponId)) {
            return weaponTagsCache.get(weaponId);
        }

        Set<String> tags = readWeaponTags(weaponId);

        weaponTagsCache.put(weaponId, tags);

        return tags;
    }

    private Set<String> readWeaponTags(String weaponId) {
        ConfigurationSection weaponConfig = modifierManager.getWeaponConfig(weaponId);

        if (weaponConfig == null) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning(String.format(
                        "[SpikeElements] 武器配置不存在: %s", weaponId));
            }
            return Collections.emptySet();
        }

        if (!weaponConfig.contains("Tag")) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "[SpikeElements] 武器 %s 没有配置Tag", weaponId));
            }
            return Collections.emptySet();
        }

        String tagString = weaponConfig.getString("Tag", "");
        if (tagString == null || tagString.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> tags = parseTagString(tagString);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "[SpikeElements] 武器 %s 的Tags: %s",
                    weaponId, String.join(", ", tags)));
        }

        return tags;
    }

    private Set<String> parseTagString(String tagString) {
        Set<String> tags = new HashSet<>();

        String[] parts = tagString.split(",");
        for (String part : parts) {
            String tag = part.trim();
            if (!tag.isEmpty()) {
                tags.add(tag.toLowerCase());
            }
        }

        return tags;
    }

    public boolean hasTag(String weaponId, String tag) {
        Set<String> tags = getWeaponTags(weaponId);
        return tags.contains(tag.toLowerCase());
    }

    public void clearCache() {
        weaponTagsCache.clear();
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[SpikeElements] 武器Tag缓存已清除");
        }
    }

    public void reload() {
        clearCache();

        List<String> weaponIds = modifierManager.getLoadedWeaponIds();
        for (String weaponId : weaponIds) {
            getWeaponTags(weaponId);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "[SpikeElements] 重新加载了 %d 个武器的Tags", weaponIds.size()));
        }
    }

    public List<String> getWeaponsWithTags() {
        List<String> result = new ArrayList<>();

        for (String weaponId : modifierManager.getLoadedWeaponIds()) {
            Set<String> tags = getWeaponTags(weaponId);
            if (!tags.isEmpty()) {
                result.add(weaponId);
            }
        }

        return result;
    }

    public List<String> getWeaponsByTag(String tag) {
        List<String> result = new ArrayList<>();

        for (String weaponId : modifierManager.getLoadedWeaponIds()) {
            if (hasTag(weaponId, tag)) {
                result.add(weaponId);
            }
        }

        return result;
    }
}