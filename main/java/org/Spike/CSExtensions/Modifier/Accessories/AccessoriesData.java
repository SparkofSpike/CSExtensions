package org.Spike.CSExtensions.Modifier.Accessories;

import org.bukkit.Material;

import java.util.*;

public class AccessoriesData {
    private final String id;
    private final String name;
    private final List<String> lore;
    private final Material material;
    private final short data;
    private final double weight;
    private final boolean isArmor;

    private final Map<AttributeType, List<AccessoryAttribute>> attributes;

    public AccessoriesData(String id, String name, List<String> lore, Material material, short data,
                           double weight, boolean isArmor,
                           Map<AttributeType, List<AccessoryAttribute>> attributes) {
        this.id = id;
        this.name = name;
        this.lore = lore != null ? lore : new ArrayList<>();
        this.material = material != null ? material : Material.PAPER;
        this.data = data;
        this.weight = weight;
        this.isArmor = isArmor;
        this.attributes = attributes != null ? attributes : new EnumMap<>(AttributeType.class);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public Material getMaterial() { return material; }
    public short getData() { return data; }
    public double getWeight() { return weight; }
    public boolean isArmor() { return isArmor; }

    public List<AccessoryAttribute> getAttributes(AttributeType type) {
        return attributes.getOrDefault(type, Collections.emptyList());
    }

    public Set<AttributeType> getAttributeTypes() {
        return attributes.keySet();
    }

    public double getAttributeValue(AttributeType type, String element, CalculationType calcType) {
        List<AccessoryAttribute> attrs = getAttributes(type);
        for (AccessoryAttribute attr : attrs) {
            if (attr.matchesElement(element) && attr.getCalcType() == calcType) {
                return attr.getValue();
            }
        }
        return 0.0;
    }

    public boolean hasAttributes(AttributeType type) {
        return !getAttributes(type).isEmpty();
    }

    public String getDisplayName() {
        return name.replace('&', '§');
    }

    public List<String> getDisplayLore() {
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(line.replace('&', '§'));
        }
        return coloredLore;
    }

    @Override
    public String toString() {
        return String.format("Accessory[%s] weight=%.1f armor=%s attrs=%d",
                id, weight, isArmor, attributes.size());
    }
}