package org.Spike.CSExtensions.Modifier.Accessories;

import java.util.*;

public class AccessoriesCalculator {
    public AccessoriesCalculator() {}

    public double calculateEffect(List<AccessoriesData> accessories, AttributeType attrType, Set<String> weaponTags) {

        for (AccessoriesData accessory : accessories) {
            List<AccessoryAttribute> attrs = accessory.getAttributes(attrType);
            for (AccessoryAttribute attr : attrs) {
                if (attr.getCalcType() == CalculationType.FIX) {
                    boolean matches = attr.isConditional() ?
                            attr.matchesCondition(weaponTags) : checkSingleMatch(attr, weaponTags);

                    if (matches) {

                        return attr.getValue();
                    }
                }
            }
        }

        double addSum = 0.0;
        double mulProduct = 1.0;

        for (AccessoriesData accessory : accessories) {
            List<AccessoryAttribute> attrs = accessory.getAttributes(attrType);
            for (AccessoryAttribute attr : attrs) {
                if (attr.getCalcType() == CalculationType.FIX) {
                    continue;
                }

                boolean matches = attr.isConditional() ?
                        attr.matchesCondition(weaponTags) : checkSingleMatch(attr, weaponTags);

                if (matches) {
                    if (attr.getCalcType() == CalculationType.ADD) {
                        addSum += attr.getValue();
                    } else if (attr.getCalcType() == CalculationType.MUL) {
                        mulProduct *= attr.getValue();
                    }
                }
            }
        }

        return (1.0 + addSum) * mulProduct;
    }

    private boolean checkSingleMatch(AccessoryAttribute attr, Set<String> weaponTags) {
        if ("all".equals(attr.getElement())) {
            return true;
        }
        if ("null".equals(attr.getElement())) {
            return weaponTags == null || weaponTags.isEmpty();
        }
        if (weaponTags == null) {
            return false;
        }
        return weaponTags.contains(attr.getElement());
    }

    private double getDefaultValue(AttributeType attrType) {
        switch (attrType) {
            case DAMAGE:
            case RELOAD:
            case SPREAD:
            case HEALTH:
                return 1.0;
            case WEIGHT:
                return 0.0;
            default:
                return 1.0;
        }
    }

    public double calculateTotalWeight(List<AccessoriesData> accessories) {
        double totalWeight = 0.0;

        for (AccessoriesData accessory : accessories) {
            totalWeight += accessory.getWeight();

            List<AccessoryAttribute> weightAttrs = accessory.getAttributes(AttributeType.WEIGHT);
            for (AccessoryAttribute attr : weightAttrs) {
                if (attr.getCalcType() == CalculationType.FLAT) {
                    totalWeight += attr.getValue();
                }

            }
        }

        return totalWeight;
    }

    public float calculateWalkSpeed(double totalWeight) {
        final float BASE_SPEED = 0.2f;
        final float WEIGHT_SLOW_FACTOR = 0.2f;

        float speedReduction = (float) (totalWeight * WEIGHT_SLOW_FACTOR);
        float newSpeed = BASE_SPEED - speedReduction;

        if (newSpeed < 0.001f) newSpeed = 0.001f;
        if (newSpeed > 10.0f) newSpeed = 10.0f;

        return newSpeed;
    }

    public double calculateMaxHealth(List<AccessoriesData> accessories) {
        final double BASE_HEALTH = 20.0;
        double healthBonus = 0.0;

        for (AccessoriesData accessory : accessories) {
            List<AccessoryAttribute> healthAttrs = accessory.getAttributes(AttributeType.HEALTH);
            for (AccessoryAttribute attr : healthAttrs) {
                if (attr.getCalcType() == CalculationType.FLAT) {
                    healthBonus += attr.getValue();
                }
            }
        }

        double maxHealth = BASE_HEALTH + healthBonus;

        if (maxHealth < 1.0) maxHealth = 1.0;
        if (maxHealth > 2048.0) maxHealth = 2048.0;

        return maxHealth;
    }

    public List<AccessoriesData> filterByArmorRequirement(List<AccessoriesData> accessories,
                                                          boolean isInArmorSlot) {
        if (isInArmorSlot) {
            return accessories;
        }

        List<AccessoriesData> filtered = new ArrayList<>();
        for (AccessoriesData accessory : accessories) {
            if (!accessory.isArmor()) {
                filtered.add(accessory);
            }
        }
        return filtered;
    }
}