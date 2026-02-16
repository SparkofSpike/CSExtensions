package org.Spike.CSExtensions.Modifier.SpikeElements;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.io.File;
import java.util.*;

public class SpikeElementsConfig implements Listener {
    private final CSExtensions plugin;
    private final Map<String, List<SpikeElementsData>> mobElementsMap = new HashMap<>();
    private final Map<String, Set<String>> mobElementsCache = new HashMap<>();

    private static final String METADATA_PREFIX = "SpikeElements_";

    public SpikeElementsConfig(CSExtensions plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadAllConfigs();
    }

    public void loadAllConfigs() {
        mobElementsMap.clear();
        mobElementsCache.clear();

        File mmDataFolder = new File(MythicMobs.inst().getDataFolder(), "Mobs");
        if (!mmDataFolder.exists() || !mmDataFolder.isDirectory()) {
            plugin.getLogger().warning("MythicMobs配置文件夹不存在: " + mmDataFolder.getPath());
            return;
        }

        File[] files = mmDataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            plugin.getLogger().warning("未找到MythicMobs配置文件");
            return;
        }

        int loadedMobs = 0;
        int loadedElements = 0;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            loadFromConfig(config, file.getName());
        }

        plugin.getLogger().info(String.format("SpikeElements配置加载完成: %d个怪物, %d个元素配置",
                mobElementsMap.size(), loadedElements));
    }

    private void loadFromConfig(YamlConfiguration config, String fileName) {
        for (String mobKey : config.getKeys(false)) {
            if (!config.isConfigurationSection(mobKey)) {
                continue;
            }

            ConfigurationSection mobSection = config.getConfigurationSection(mobKey);
            if (mobSection == null || !mobSection.contains("SpikeElements")) {
                continue;
            }

            List<String> spikeElementsList = mobSection.getStringList("SpikeElements");
            if (spikeElementsList.isEmpty()) {
                continue;
            }

            List<SpikeElementsData> elementDataList = new ArrayList<>();
            Set<String> definedElements = new HashSet<>();

            for (String elementConfig : spikeElementsList) {
                SpikeElementsData data = parseElementConfig(elementConfig);
                if (data != null) {
                    elementDataList.add(data);
                    if (!"null".equals(data.getIdentifier()) && !"others".equals(data.getIdentifier())) {
                        definedElements.add(data.getIdentifier().toLowerCase());
                    }
                }
            }

            if (!elementDataList.isEmpty()) {
                mobElementsMap.put(mobKey, elementDataList);
                mobElementsCache.put(mobKey, definedElements);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(String.format("[SpikeElements] 加载怪物 %s: %d个元素配置",
                            mobKey, elementDataList.size()));
                }
            }
        }
    }

    private SpikeElementsData parseElementConfig(String configLine) {

        String[] parts = configLine.trim().split("\\s+", 3);
        if (parts.length < 3) {
            plugin.getLogger().warning("SpikeElements配置格式错误: " + configLine);
            return null;
        }

        String elementOrCondition = parts[0];
        double value;
        CalculationType type;

        try {
            value = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("SpikeElements数值格式错误: " + parts[1] + " in " + configLine);
            return null;
        }

        type = CalculationType.fromString(parts[2]);
        if (type == null) {
            plugin.getLogger().warning("SpikeElements计算类型错误: " + parts[2] + " in " + configLine);
            return null;
        }

        if (isConditionExpression(elementOrCondition)) {
            return new SpikeElementsData("conditional", value, type, elementOrCondition);
        } else {
            return new SpikeElementsData(elementOrCondition.toLowerCase(), value, type, null);
        }
    }

    private boolean isConditionExpression(String str) {
        return str.contains("&&") || str.contains("||") ||
                str.contains("!") || str.contains("(") || str.contains(")");
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        String mobName = event.getMobType().getInternalName();
        Entity entity = event.getEntity();

        List<SpikeElementsData> elements = mobElementsMap.get(mobName);
        if (elements == null || elements.isEmpty()) {
            return;
        }

        applySpikeElementsToEntity(entity, elements);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "[SpikeElements] 为怪物 %s(%s) 应用了 %d 个元素配置",
                    mobName, entity.getUniqueId(), elements.size()
            ));
        }
    }

    private void applySpikeElementsToEntity(Entity entity, List<SpikeElementsData> elements) {
        entity.setMetadata(METADATA_PREFIX + "DATA",
                new FixedMetadataValue(plugin, elements));

        for (SpikeElementsData data : elements) {
            String metadataKey = String.format("%s%s_%s",
                    METADATA_PREFIX, data.getType().name(), data.getElement());

            entity.setMetadata(metadataKey,
                    new FixedMetadataValue(plugin, data.getValue()));

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "[SpikeElements] 设置Metadata: %s = %.2f", metadataKey, data.getValue()));
            }
        }
    }

    public String getMobNameForEntity(Entity entity) {
        if (!isMythicMob(entity)) {
            return null;
        }

        ActiveMob activeMob = MythicMobs.inst().getMobManager().getMythicMobInstance(
                io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(entity));

        if (activeMob == null) {
            return null;
        }

        return activeMob.getType().getInternalName();
    }

    public boolean isMythicMob(Entity entity) {
        return MythicMobs.inst().getAPIHelper().isMythicMob(entity);
    }

    public List<SpikeElementsData> getMobElements(String mobName) {
        return mobElementsMap.getOrDefault(mobName, Collections.emptyList());
    }

    public Set<String> getDefinedElements(String mobName) {
        return mobElementsCache.getOrDefault(mobName, Collections.emptySet());
    }

    public List<SpikeElementsData> getElementsFromEntity(Entity entity) {
        if (!isMythicMob(entity)) {
            return Collections.emptyList();
        }

        List<MetadataValue> metadataList = entity.getMetadata(METADATA_PREFIX + "DATA");
        if (!metadataList.isEmpty()) {
            Object value = metadataList.get(0).value();
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<SpikeElementsData> cachedData = (List<SpikeElementsData>) value;
                return new ArrayList<>(cachedData);
            }
        }

        String mobName = getMobNameForEntity(entity);
        if (mobName == null) {
            return Collections.emptyList();
        }

        List<SpikeElementsData> elements = getMobElements(mobName);
        if (!elements.isEmpty()) {
            applySpikeElementsToEntity(entity, elements);
        }

        return elements;
    }

    public boolean hasSpikeElements(String mobName) {
        return mobElementsMap.containsKey(mobName);
    }

    public boolean hasSpikeElements(Entity entity) {
        if (!isMythicMob(entity)) {
            return false;
        }

        if (!entity.getMetadata(METADATA_PREFIX + "DATA").isEmpty()) {
            return true;
        }

        String mobName = getMobNameForEntity(entity);
        return mobName != null && hasSpikeElements(mobName);
    }

    public void setElementResistance(Entity entity, String element, CalculationType type, double value) {
        if (!isMythicMob(entity)) {
            return;
        }

        String metadataKey = String.format("%s%s_%s", METADATA_PREFIX, type.name(), element);
        entity.setMetadata(metadataKey, new FixedMetadataValue(plugin, value));

        updateCachedData(entity, element, type, value);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "[SpikeElements] 动态修改抗性: %s -> %s = %.2f",
                    entity.getUniqueId(), metadataKey, value
            ));
        }
    }

    private void updateCachedData(Entity entity, String element, CalculationType type, double value) {
        List<MetadataValue> metadataList = entity.getMetadata(METADATA_PREFIX + "DATA");
        if (metadataList.isEmpty()) {
            return;
        }

        Object valueObj = metadataList.get(0).value();
        if (!(valueObj instanceof List)) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<SpikeElementsData> dataList = (List<SpikeElementsData>) valueObj;

        for (int i = 0; i < dataList.size(); i++) {
            SpikeElementsData data = dataList.get(i);
            if (data.getElement().equals(element) && data.getType() == type) {
                dataList.set(i, new SpikeElementsData(element, value, type,null));
                break;
            }
        }
    }

    public Double getElementResistance(Entity entity, String element, CalculationType type) {
        String metadataKey = String.format("%s%s_%s", METADATA_PREFIX, type.name(), element);
        List<MetadataValue> values = entity.getMetadata(metadataKey);

        if (values.isEmpty()) {
            return null;
        }

        Object value = values.get(0).value();
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        }

        return null;
    }

    public void reload() {
        loadAllConfigs();
    }

    public Set<String> getConfiguredMobs() {
        return new HashSet<>(mobElementsMap.keySet());
    }
}