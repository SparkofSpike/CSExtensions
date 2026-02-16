package org.Spike.CSExtensions.Modifier.HealthAdjust;

import java.util.HashMap;
import java.util.Map;

public class MergedEffectManager {
    private final Map<String, Map<Integer, Double>> weaponHealingMap = new HashMap<>();
    private final Map<String, Map<Integer, Double>> weaponDamageMap = new HashMap<>();

    private int currentTick = 0;

    public void addEffect(String weaponTitle, HealthAdjustEffect effect, int startTickOffset) {
        if (effect == null || effect.isInstant()) {
            return;
        }

        Map<String, Map<Integer, Double>> targetMap = effect.isHealing() ? weaponHealingMap : weaponDamageMap;

        Map<Integer, Double> tickMap = targetMap.computeIfAbsent(weaponTitle, k -> new HashMap<>());

        int startTick = currentTick + startTickOffset;
        for (int i = 0; i < effect.getDurationTicks(); i++) {
            int tick = startTick + i;
            double current = tickMap.getOrDefault(tick, 0.0);
            tickMap.put(tick, current + effect.getAmountPerTick());
        }
    }

    public void addSelfEffect(String weaponTitle, HealthAdjustEffect effect, int startTickOffset) {
        if (effect == null || effect.isInstant()) {
            return;
        }

        Map<String, Map<Integer, Double>> targetMap = effect.isHealing() ? weaponHealingMap : weaponDamageMap;

        Map<Integer, Double> tickMap = targetMap.computeIfAbsent(weaponTitle, k -> new HashMap<>());

        int startTick = currentTick + startTickOffset;
        for (int i = 0; i < effect.getDurationTicks(); i++) {
            int tick = startTick + i;
            double current = tickMap.getOrDefault(tick, 0.0);
            tickMap.put(tick, current + effect.getAmountPerTick());
        }
    }

    public double getTotalHealingForCurrentTick() {
        return getTotalForCurrentTick(true);
    }

    public double getTotalDamageForCurrentTick() {
        return getTotalForCurrentTick(false);
    }

    public double getHealingForWeapon(String weaponTitle) {
        return getValueForWeaponAndTick(weaponTitle, currentTick, true);
    }

    public double getDamageForWeapon(String weaponTitle) {
        return getValueForWeaponAndTick(weaponTitle, currentTick, false);
    }

    private double getTotalForCurrentTick(boolean isHealing) {
        double total = 0;
        Map<String, Map<Integer, Double>> targetMap = isHealing ? weaponHealingMap : weaponDamageMap;

        for (Map<Integer, Double> tickMap : targetMap.values()) {
            total += tickMap.getOrDefault(currentTick, 0.0);
        }
        return total;
    }

    private double getValueForWeaponAndTick(String weaponTitle, int tick, boolean isHealing) {
        Map<String, Map<Integer, Double>> targetMap = isHealing ? weaponHealingMap : weaponDamageMap;

        Map<Integer, Double> tickMap = targetMap.get(weaponTitle);
        return tickMap != null ? tickMap.getOrDefault(tick, 0.0) : 0.0;
    }

    public void advanceTick() {
        currentTick++;

        cleanupOldTicks();
    }

    private void cleanupOldTicks() {
        int cleanupThreshold = currentTick - 1000;

        cleanupMap(weaponHealingMap, cleanupThreshold);
        cleanupMap(weaponDamageMap, cleanupThreshold);
    }

    private void cleanupMap(Map<String, Map<Integer, Double>> weaponMap, int threshold) {
        for (Map<Integer, Double> tickMap : weaponMap.values()) {
            tickMap.entrySet().removeIf(entry -> entry.getKey() < threshold);
        }
        weaponMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void cleanupWeaponEffects(String weaponTitle) {
        weaponHealingMap.remove(weaponTitle);
        weaponDamageMap.remove(weaponTitle);
    }

    public void cleanupAll() {
        weaponHealingMap.clear();
        weaponDamageMap.clear();
        currentTick = 0;
    }
}