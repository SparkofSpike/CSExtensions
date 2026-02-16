package org.Spike.CSExtensions.api;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.*;
import org.Spike.CSExtensions.Modifier.SpikeElements.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.*;

class SpikeElementsAPIImpl implements SpikeElementsAPI {
    private final CSExtensions plugin;
    private final SpikeElementsManager manager;
    private final SpikeElementsConfig config;
    private final SpikeElementsCalculator calculator;

    SpikeElementsAPIImpl(SpikeElementsManager manager) {
        this.plugin = manager.getPlugin();
        this.manager = manager;
        this.config = manager.getConfig();
        this.calculator = manager.getCalculator();
    }

    @Override
    public double calculateDamage(double baseDamage, Set<String> weaponTags, Entity target) {
        if (!isValidTarget(target)) return baseDamage;
        return calculator.calculateDamageFromEntity(baseDamage, weaponTags, target, config);
    }

    @Override
    public CalculationResult calculateDamageDetailed(double baseDamage, Set<String> weaponTags, Entity target) {
        if (!isValidTarget(target)) {
            return new CalculationResult(baseDamage, baseDamage);
        }

        List<SpikeElementsData> mobElements = config.getElementsFromEntity(target);
        if (mobElements.isEmpty()) {
            return new CalculationResult(baseDamage, baseDamage);
        }

        String mobName = config.getMobNameForEntity(target);
        Set<String> definedElements = mobName != null ?
                config.getDefinedElements(mobName) : Collections.emptySet();

        return calculateDamageInternal(baseDamage, weaponTags, mobElements, definedElements);
    }

    @Override
    public Double getResistance(Entity target, String element, CalculationType type) {
        if (!isValidTarget(target)) return null;

        String metadataKey = String.format("SpikeElements_%s_%s", type.name(), element);
        List<MetadataValue> values = target.getMetadata(metadataKey);

        for (MetadataValue value : values) {
            if (value.getOwningPlugin() == plugin) {
                Object obj = value.value();
                if (obj instanceof Double) return (Double) obj;
                if (obj instanceof Integer) return ((Integer) obj).doubleValue();
                if (obj instanceof Float) return ((Float) obj).doubleValue();
            }
        }
        return null;
    }

    @Override
    public boolean hasSpikeElements(Entity target) {
        return isValidTarget(target) && target.hasMetadata("SpikeElements_DATA");
    }

    @Override
    public List<SpikeElementsData> getSpikeElements(Entity target) {
        if (!isValidTarget(target)) return Collections.emptyList();
        return config.getElementsFromEntity(target);
    }

    @Override
    public double getDamageMultiplier(Entity target, String element) {
        Double fix = getResistance(target, element, CalculationType.FIX);
        if (fix != null) return fix;

        Double add = getResistance(target, element, CalculationType.ADD);
        Double mul = getResistance(target, element, CalculationType.MUL);
        Double ultiAdd = getResistance(target, element, CalculationType.ULTIADD);
        Double ultiMul = getResistance(target, element, CalculationType.ULTIMUL);

        double result = 1.0;
        if (add != null) result *= (1.0 + add);
        if (mul != null) result *= mul;
        if (ultiAdd != null) result *= (1.0 + ultiAdd);
        if (ultiMul != null) result *= ultiMul;

        return result;
    }

    @Override
    public boolean setResistance(Entity target, String element, CalculationType type, double value) {
        if (!isValidTarget(target)) return false;

        String metadataKey = String.format("SpikeElements_%s_%s", type.name(), element);
        target.setMetadata(metadataKey, new FixedMetadataValue(plugin, value));

        updateCachedData(target, element, type, value);

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[API] 修改抗性: %s %s %s = %.2f",
                    target.getType(), element, type, value
            ));
        }
        return true;
    }

    @Override
    public boolean removeResistance(Entity target, String element, CalculationType type) {
        if (!isValidTarget(target)) return false;

        String metadataKey = String.format("SpikeElements_%s_%s", type.name(), element);
        if (target.hasMetadata(metadataKey)) {
            target.removeMetadata(metadataKey, plugin);


            removeFromCachedData(target, element, type);

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info(String.format(
                        "[API] 移除抗性: %s %s %s",
                        target.getType(), element, type
                ));
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearAllResistances(Entity target) {
        if (!isValidTarget(target)) return;


        target.removeMetadata("SpikeElements_DATA", plugin);


        for (MetadataValue meta : new ArrayList<>(target.getMetadata("SpikeElements_DATA"))) {
            if (meta.getOwningPlugin() == plugin) {
                target.removeMetadata("SpikeElements_DATA", plugin);
                break;
            }
        }


        String[] types = {"ADD", "MUL", "ULTIADD", "ULTIMUL", "FIX"};
        String[] elements = {"fire", "ice", "lightning", "water", "magic", "all", "null", "others"};

        for (String type : types) {
            for (String element : elements) {
                String key = String.format("SpikeElements_%s_%s", type, element);
                target.removeMetadata(key, plugin);
            }
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[API] 清空所有抗性: %s", target.getType()
            ));
        }
    }

    @Override
    public int setResistances(Entity target, Map<String, Double> resistances) {
        int count = 0;
        for (Map.Entry<String, Double> entry : resistances.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("_", 2);

            if (parts.length == 2) {
                CalculationType type = null;
                try {
                    type = CalculationType.valueOf(parts[0].toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                String element = parts[1].toLowerCase();
                if (setResistance(target, element, type, entry.getValue())) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isValidTarget(Entity target) {
        return target != null && !target.isDead() && target instanceof LivingEntity;
    }


    private void updateCachedData(Entity target, String element, CalculationType type, double value) {
        List<MetadataValue> dataList = target.getMetadata("SpikeElements_DATA");
        if (dataList.isEmpty()) {

            List<SpikeElementsData> newList = new ArrayList<>();
            newList.add(new SpikeElementsData(element, value, type, null));
            target.setMetadata("SpikeElements_DATA", new FixedMetadataValue(plugin, newList));
            return;
        }

        for (MetadataValue meta : dataList) {
            if (meta.getOwningPlugin() == plugin) {
                Object obj = meta.value();
                if (obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<SpikeElementsData> list = (List<SpikeElementsData>) obj;


                    boolean found = false;
                    for (int i = 0; i < list.size(); i++) {
                        SpikeElementsData data = list.get(i);
                        if (data.getIdentifier().equals(element) && data.getType() == type) {
                            list.set(i, new SpikeElementsData(element, value, type, null));
                            found = true;
                            break;
                        }
                    }


                    if (!found) {
                        list.add(new SpikeElementsData(element, value, type, null));
                    }

                    target.setMetadata("SpikeElements_DATA", new FixedMetadataValue(plugin, list));
                }
                break;
            }
        }
    }


    private void removeFromCachedData(Entity target, String element, CalculationType type) {
        List<MetadataValue> dataList = target.getMetadata("SpikeElements_DATA");
        for (MetadataValue meta : dataList) {
            if (meta.getOwningPlugin() == plugin) {
                Object obj = meta.value();
                if (obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<SpikeElementsData> list = (List<SpikeElementsData>) obj;
                    list.removeIf(data -> data.getIdentifier().equals(element) && data.getType() == type);
                    target.setMetadata("SpikeElements_DATA", new FixedMetadataValue(plugin, list));
                }
                break;
            }
        }
    }

    private CalculationResult calculateDamageInternal(double baseDamage,
                                                      Set<String> weaponTags,
                                                      List<SpikeElementsData> mobElements,
                                                      Set<String> definedElements) {


        return new CalculationResult(baseDamage, baseDamage);
    }
}