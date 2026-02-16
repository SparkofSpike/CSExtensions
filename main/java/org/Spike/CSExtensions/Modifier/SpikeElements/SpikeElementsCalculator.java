package org.Spike.CSExtensions.Modifier.SpikeElements;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.entity.Entity;

import java.util.*;

public class SpikeElementsCalculator {
    private final CSExtensions plugin;

    public SpikeElementsCalculator(CSExtensions plugin) {
        this.plugin = plugin;
    }
    public double calculateDamageFromEntity(double baseDamage,
                                            Set<String> weaponTags,
                                            Entity targetEntity,
                                            SpikeElementsConfig config) {

        List<SpikeElementsData> mobElements = config.getElementsFromEntity(targetEntity);
        if (mobElements.isEmpty()) {
            return baseDamage;
        }

        String mobName = config.getMobNameForEntity(targetEntity);
        Set<String> definedElements = mobName != null ?
                config.getDefinedElements(mobName) : Collections.emptySet();

        return calculateDamage(baseDamage, weaponTags, mobElements, definedElements);
    }

    public double calculateDamage(double baseDamage,
                                  Set<String> weaponTags,
                                  List<SpikeElementsData> mobElements,
                                  Set<String> definedElements) {

        if (mobElements == null || mobElements.isEmpty()) {
            return baseDamage;
        }

        List<SpikeElementsData> addElements = new ArrayList<>();
        List<SpikeElementsData> mulElements = new ArrayList<>();
        List<SpikeElementsData> ultiAddElements = new ArrayList<>();
        List<SpikeElementsData> ultiMulElements = new ArrayList<>();
        List<SpikeElementsData> fixElements = new ArrayList<>();

        boolean hasUnknownElements = false;
        if (weaponTags != null && !weaponTags.isEmpty()) {
            for (String tag : weaponTags) {
                if (!isDefinedElement(tag, mobElements, definedElements)) {
                    hasUnknownElements = true;
                    break;
                }
            }
        }

        for (SpikeElementsData data : mobElements) {
            boolean matches = checkMatch(data, weaponTags, hasUnknownElements);

            if (matches) {
                switch (data.getType()) {
                    case ADD: addElements.add(data); break;
                    case MUL: mulElements.add(data); break;
                    case ULTIADD: ultiAddElements.add(data); break;
                    case ULTIMUL: ultiMulElements.add(data); break;
                    case FIX: fixElements.add(data); break;
                }
            }
        }

        if (!fixElements.isEmpty()) {
            SpikeElementsData lastFix = fixElements.get(fixElements.size() - 1);
            double fixValue = lastFix.getValue();

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info(String.format(
                        "[FIX算法] 硬性设定生效: %.2f倍 (条件: %s)",
                        fixValue, lastFix.getCondition() != null ? lastFix.getCondition() : lastFix.getIdentifier()
                ));
            }

            return baseDamage * fixValue;
        }

        double addSum = calculateSum(addElements);
        double mulProduct = calculateProduct(mulElements);
        double ultiAddSum = calculateSum(ultiAddElements);
        double ultiMulProduct = calculateProduct(ultiMulElements);

        double finalDamage = baseDamage * (1.0 + addSum) * mulProduct *
                (1.0 + ultiAddSum) * ultiMulProduct;

        if (plugin.getConfig().getBoolean("debug")) {
            logCalculation(baseDamage, weaponTags, addElements, mulElements,
                    ultiAddElements, ultiMulElements, fixElements, finalDamage);
        }

        return finalDamage;
    }

    private boolean checkMatch(SpikeElementsData data, Set<String> weaponTags, boolean hasUnknownElements) {
        if (data.isConditional()) {
            return data.matches(weaponTags);
        }

        String identifier = data.getIdentifier();

        if ("all".equals(identifier)) {
            return true;
        }
        if ("null".equals(identifier)) {
            return weaponTags == null || weaponTags.isEmpty();
        }
        if ("others".equals(identifier)) {
            return hasUnknownElements;
        }

        return weaponTags != null && weaponTags.contains(identifier);
    }

    private double calculateSum(List<SpikeElementsData> elements) {
        double sum = 0.0;
        for (SpikeElementsData data : elements) {
            sum += data.getValue();
        }
        return sum;
    }

    private double calculateProduct(List<SpikeElementsData> elements) {
        double product = 1.0;
        for (SpikeElementsData data : elements) {
            product *= data.getValue();
        }
        return product;
    }

    private boolean isDefinedElement(String tag, List<SpikeElementsData> mobElements, Set<String> definedElements) {
        if (definedElements.contains(tag)) {
            return true;
        }

        for (SpikeElementsData data : mobElements) {
            if (data.isConditional() && data.getCondition() != null &&
                    data.getCondition().contains(tag)) {
                return true;
            }
        }

        return false;
    }

    private void logCalculation(double baseDamage, Set<String> weaponTags,
                                List<SpikeElementsData> addElements,
                                List<SpikeElementsData> mulElements,
                                List<SpikeElementsData> ultiAddElements,
                                List<SpikeElementsData> ultiMulElements,
                                List<SpikeElementsData> fixElements,
                                double finalDamage) {

        StringBuilder log = new StringBuilder();
        log.append("\n=== [SpikeElements] 伤害计算详情 ===\n");
        log.append(String.format("基础伤害: %.2f\n", baseDamage));

        if (weaponTags == null || weaponTags.isEmpty()) {
            log.append("武器元素: 无属性\n");
        } else {
            log.append("武器元素: ").append(String.join(", ", weaponTags)).append("\n");
        }

        if (!fixElements.isEmpty()) {
            log.append("FIX算法生效，覆盖所有计算！\n");
            for (SpikeElementsData fix : fixElements) {
                log.append(String.format("  FIX: %s = %.2f\n",
                        fix.toString(), fix.getValue()));
            }
        } else {
            log.append(String.format("加算阶段: %.2f (数量: %d)\n",
                    calculateSum(addElements), addElements.size()));
            log.append(String.format("乘算阶段: %.2f (数量: %d)\n",
                    calculateProduct(mulElements), mulElements.size()));
            log.append(String.format("最终加算阶段: %.2f (数量: %d)\n",
                    calculateSum(ultiAddElements), ultiAddElements.size()));
            log.append(String.format("最终乘算阶段: %.2f (数量: %d)\n",
                    calculateProduct(ultiMulElements), ultiMulElements.size()));
        }

        log.append(String.format("最终伤害: %.2f\n", finalDamage));
        log.append(String.format("总倍率: %.2f倍\n", finalDamage / baseDamage));
        log.append("======================================\n");

        plugin.getLogger().info(log.toString());
    }

    private SpikeElementsCalculator getCalculator() {return this;}
}