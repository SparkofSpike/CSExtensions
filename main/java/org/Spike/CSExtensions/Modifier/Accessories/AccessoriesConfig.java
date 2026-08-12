package org.Spike.CSExtensions.Modifier.Accessories;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Mythic.accessory.AccessoryMythicConfig;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AccessoriesConfig {
    private final CSExtensions plugin;
    private final Map<String, AccessoriesData> accessoriesMap = new HashMap<>();
    private final Map<String, Material> materialCache = new HashMap<>();
    private final AccessoryMythicConfig mythicConfig;
    private final Map<String, ConflictGroup> conflictGroups = new HashMap<>();

    private File configFile;
    private FileConfiguration config;

    public AccessoriesConfig(CSExtensions plugin) {
        this.plugin = plugin;
        this.mythicConfig = new AccessoryMythicConfig(plugin);
        plugin.getLogger().info("[饰品配置] 创建AccessoryMythicConfig实例: " + mythicConfig.hashCode());
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "accessories.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            plugin.saveResource("accessories.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        reload();
    }

    public void reload() {
        plugin.getLogger().info("[饰品配置] 开始重载配置文件...");

        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("[饰品配置] 文件加载完成: " + configFile.getPath());

            accessoriesMap.clear();
            materialCache.clear();
            mythicConfig.clear();

            if (!config.contains("accessories")) {
                plugin.getLogger().warning("[饰品配置] 配置文件中未找到 'accessories' 节点");
                return;
            }
            parseConflictGroups();
            ConfigurationSection accessoriesSection = config.getConfigurationSection("accessories");
            if (accessoriesSection == null) {
                plugin.getLogger().warning("[饰品配置] accessoriesSection为null");
                return;
            }
            int loadedCount = 0;
            for (String accessoryId : accessoriesSection.getKeys(false)) {
                ConfigurationSection section = accessoriesSection.getConfigurationSection(accessoryId);
                if (section != null) {
                    AccessoriesData data = parseAccessory(accessoryId, section);
                    if (data != null) {
                        accessoriesMap.put(accessoryId, data);
                        loadedCount++;
                    }
                }
            }

            plugin.getLogger().info("[饰品配置] 成功加载 " + loadedCount + " 个饰品");

        } catch (Exception e) {
            plugin.getLogger().severe("[饰品配置] 重载异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private AccessoriesData parseAccessory(String accessoryId, ConfigurationSection section) {
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[饰品配置] 解析饰品: " + accessoryId);
            if (section.contains("Mythic")) {
                plugin.getLogger().info("[饰品配置] 找到Mythic配置");
                List<String> mythicList = section.getStringList("Mythic");
                plugin.getLogger().info("[饰品配置] Mythic列表大小: " + mythicList.size());
                for (String line : mythicList) {
                    plugin.getLogger().info("[饰品配置] Mythic行: " + line);
                }
            } else if (section.contains("mythic")) {
                plugin.getLogger().info("[饰品配置] 找到mythic配置");
                List<String> mythicList = section.getStringList("mythic");
                plugin.getLogger().info("[饰品配置] mythic列表大小: " + mythicList.size());
            } else {
                plugin.getLogger().info("[饰品配置] 没有Mythic配置");
            }
            if (section == null) {
                return null;
            }
        }
        String name = section.getString("name", "未命名饰品");
        List<String> lore = section.getStringList("lore");
        Material material = parseMaterial(section.getString("material", "PAPER"));
        short data = (short) section.getInt("data", 0);
        double weight = section.getDouble("weight", 0.0);
        boolean isArmor = section.getBoolean("armor", false);

        Map<AttributeType, List<AccessoryAttribute>> attributes = new EnumMap<>(AttributeType.class);

        if (section.contains("damage")) {
            attributes.put(AttributeType.DAMAGE, parseAttributeList(section.getStringList("damage"), "damage"));
        }

        if (section.contains("reload")) {
            attributes.put(AttributeType.RELOAD, parseAttributeList(section.getStringList("reload"), "reload"));
        }

        if (section.contains("spread")) {
            attributes.put(AttributeType.SPREAD, parseAttributeList(section.getStringList("spread"), "spread"));
        }

        List<AccessoryAttribute> healthAttrs = new ArrayList<>();
        if (section.contains("health")) {
            healthAttrs.addAll(parseAttributeList(section.getStringList("health"), "health"));
        }
        if (section.contains("health_adjustment")) {
            double healthValue = section.getDouble("health_adjustment", 0.0);
            if (Math.abs(healthValue) > 0.001) {
                healthAttrs.add(new AccessoryAttribute("all", healthValue, CalculationType.FLAT,
                        plugin));
            }
        }
        if (!healthAttrs.isEmpty()) {
            attributes.put(AttributeType.HEALTH, healthAttrs);
        }

        if (section.contains("weight_effect")) {
            attributes.put(AttributeType.WEIGHT, parseAttributeList(section.getStringList("weight_effect"), "weight"));
        }

        mythicConfig.loadConfig(section, accessoryId);
        plugin.getLogger().info("[饰品配置] 调用mythicConfig.loadConfig() 实例: " + mythicConfig.hashCode());

        return new AccessoriesData(accessoryId, name, lore, material,data, weight, isArmor,
                attributes);
    }

    private List<AccessoryAttribute> parseAttributeList(List<String> configList, String attrName) {
        List<AccessoryAttribute> attributes = new ArrayList<>();

        if (configList == null || configList.isEmpty()) {
            return attributes;
        }

        for (String configLine : configList) {
            AccessoryAttribute attr = parseAttribute(configLine);
            if (attr != null) {
                attributes.add(attr);
            } else {
                plugin.getLogger().warning(String.format("饰品属性解析失败: %s -> %s", attrName, configLine));
            }
        }

        return attributes;
    }

    private AccessoryAttribute parseAttribute(String configLine) {

        String[] parts = configLine.trim().split("\\s+", 3);
        if (parts.length < 3) return null;

        String elementOrCondition = parts[0];
        double value;
        CalculationType calcType;

        try {
            value = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        calcType = CalculationType.fromString(parts[2]);
        if (calcType == null) return null;


        if (isConditionExpression(elementOrCondition)) {
            return new AccessoryAttribute("conditional", value, calcType, elementOrCondition,
                    plugin);
        } else {
            return new AccessoryAttribute(elementOrCondition.toLowerCase(), value, calcType, null);
        }
    }

    private Material parseMaterial(String materialString) {
        if (materialString == null || materialString.isEmpty()) {
            return Material.PAPER;
        }

        String[] parts = materialString.split(":");
        String materialName = parts[0];
        short durability = 0;

        if (parts.length > 1) {
            try {
                durability = Short.parseShort(parts[1]);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("材质附加值格式错误: " + parts[1] + " in " + materialString);
            }
        }

        String cacheKey = materialString.toUpperCase();
        if (materialCache.containsKey(cacheKey)) {
            return materialCache.get(cacheKey);
        }

        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            materialCache.put(cacheKey, material);
            return material;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("材质不存在: " + materialName + "，使用默认材质 PAPER");
            materialCache.put(cacheKey, Material.PAPER);
            return Material.PAPER;
        }
    }

    private boolean isConditionExpression(String str) {
        if (str == null) return false;

        return str.contains("&&") || str.contains("||") ||
                str.contains("!") || str.contains("(") || str.contains(")");
    }

    public AccessoriesData getAccessory(String accessoryId) {
        return accessoriesMap.get(accessoryId);
    }

    public Set<String> getAllAccessoryIds() {
        return new HashSet<>(accessoriesMap.keySet());
    }

    public boolean accessoryExists(String accessoryId) {
        return accessoriesMap.containsKey(accessoryId);
    }

    public AccessoryMythicConfig getMythicConfig() {
        return mythicConfig;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存饰品配置失败: " + e.getMessage());
        }
    }

    private void parseConflictGroups() {
        if (!config.contains("Conflict_Groups")) {
            return;
        }

        ConfigurationSection groupsSection = config.getConfigurationSection("Conflict_Groups");
        if (groupsSection == null) {
            return;
        }

        int loadedGroups = 0;
        for (String groupName : groupsSection.getKeys(false)) {
            ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
            if (groupSection == null) continue;

            int limit = groupSection.getInt("Limit", 1);
            String message = groupSection.getString("Message_Exceeded",
                    "&c饰品冲突，请卸下部分饰品");
            List<String> included = groupSection.getStringList("Included");

            if (!included.isEmpty()) {
                ConflictGroup group = new ConflictGroup(
                        groupName,
                        limit,
                        message.replace('&', '§'),
                        new HashSet<>(included)
                );

                conflictGroups.put(groupName, group);
                loadedGroups++;

                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().info(String.format(
                            "[饰品冲突组] 加载组 %s: 限制%d个, 包含%d个饰品",
                            groupName, limit, included.size()
                    ));
                }
            }
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[饰品冲突组] 加载了 %d 个冲突组",
                    loadedGroups
            ));
        }
    }

    public Map<String, ConflictGroup> getConflictGroups() {
        return new HashMap<>(conflictGroups);
    }

    public List<ConflictGroup> getGroupsForAccessory(String accessoryId) {
        List<ConflictGroup> groups = new ArrayList<>();
        for (ConflictGroup group : conflictGroups.values()) {
            if (group.containsAccessory(accessoryId)) {
                groups.add(group);
            }
        }
        return groups;
    }
}