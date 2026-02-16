package org.Spike.CSExtensions.api;
import org.Spike.CSExtensions.Modifier.SpikeElements.CalculationType;
import org.Spike.CSExtensions.Modifier.SpikeElements.SpikeElementsData;
import org.bukkit.entity.Entity;
import java.util.List;
import java.util.Map;
import java.util.Set;
public interface SpikeElementsAPI {
    double calculateDamage(double baseDamage, Set<String> weaponTags, Entity target);
    CalculationResult calculateDamageDetailed(double baseDamage, Set<String> weaponTags, Entity target);
    Double getResistance(Entity target, String element, CalculationType type);
    boolean hasSpikeElements(Entity target);
    List<SpikeElementsData> getSpikeElements(Entity target);
    double getDamageMultiplier(Entity target, String element);
    boolean setResistance(Entity target, String element, CalculationType type, double value);
    boolean removeResistance(Entity target, String element, CalculationType type);
    void clearAllResistances(Entity target);
    int setResistances(Entity target, Map<String, Double> resistances);
}