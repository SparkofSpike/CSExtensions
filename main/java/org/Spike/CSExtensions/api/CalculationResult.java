package org.Spike.CSExtensions.api;

import org.Spike.CSExtensions.Modifier.SpikeElements.CalculationType;
import org.Spike.CSExtensions.Modifier.SpikeElements.SpikeElementsData;

import java.util.*;

class CalculationResult {
    private final double baseDamage;
    private final double finalDamage;
    private final double multiplier;
    private final boolean fixApplied;
    private final Double fixValue;
    private final List<SpikeElementsData> appliedElements;
    private final Map<CalculationType, Double> stageValues;

    public CalculationResult(double baseDamage, double finalDamage) {
        this(baseDamage, finalDamage, false, null);
    }

    public CalculationResult(double baseDamage, double finalDamage, boolean fixApplied, Double fixValue) {
        this.baseDamage = baseDamage;
        this.finalDamage = finalDamage;
        this.multiplier = baseDamage > 0 ? finalDamage / baseDamage : 1.0;
        this.fixApplied = fixApplied;
        this.fixValue = fixValue;
        this.appliedElements = new ArrayList<>();
        this.stageValues = new EnumMap<>(CalculationType.class);
    }

    public double getBaseDamage() { return baseDamage; }
    public double getFinalDamage() { return finalDamage; }
    public double getMultiplier() { return multiplier; }
    public boolean isFixApplied() { return fixApplied; }
    public Optional<Double> getFixValue() { return Optional.ofNullable(fixValue); }
    public List<SpikeElementsData> getAppliedElements() {
        return Collections.unmodifiableList(appliedElements);
    }
    public Map<CalculationType, Double> getStageValues() {
        return Collections.unmodifiableMap(stageValues);
    }

    void addAppliedElement(SpikeElementsData data) { appliedElements.add(data); }
    void setStageValue(CalculationType type, double value) { stageValues.put(type, value); }

    @Override
    public String toString() {
        if (fixApplied) {
            return String.format("CalculationResult{base=%.2f, final=%.2f, fix=%.2f, mult=%.2fx}",
                    baseDamage, finalDamage, fixValue, multiplier);
        } else {
            return String.format("CalculationResult{base=%.2f, final=%.2f, mult=%.2fx, stages=%s}",
                    baseDamage, finalDamage, multiplier, stageValues);
        }
    }
}