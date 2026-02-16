package org.Spike.CSExtensions.Modifier.HealthAdjust;

import net.minecraft.server.v1_8_R3.DamageSource;
import net.minecraft.server.v1_8_R3.EntityLiving;
import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ActiveHealthEffect {
    private final CSExtensions plugin;
    private final UUID targetId;
    private final boolean isHealing;
    private final boolean trueDamage;
    private final double amountPerTick;
    private final String weaponTitle;
    private final UUID shooterId;
    private int remainingTicks;

    public ActiveHealthEffect(CSExtensions plugin, UUID targetId, boolean isHealing, double amountPerTick, int durationTicks, String weaponTitle, UUID shooterId, boolean trueDamage) {
        this.plugin = plugin;
        this.targetId = targetId;
        this.isHealing = isHealing;
        this.trueDamage = trueDamage;
        this.amountPerTick = amountPerTick;
        this.remainingTicks = durationTicks;
        this.weaponTitle = weaponTitle;
        this.shooterId = shooterId;
    }

    private LivingEntity findEntityByUuid(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid) && entity instanceof LivingEntity) {
                    return (LivingEntity) entity;
                }
            }
        }
        return null;
    }

    public boolean applyTick() {
        if (remainingTicks <= 0) {
            return false;
        }

        LivingEntity target = findEntityByUuid(targetId);
        if (target == null || target.isDead()) {
            return false;
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            String targetName = target.getName();
            UUID targetUUID = target.getUniqueId();
            String effectType = isHealing ? "治疗" : "伤害";

            plugin.getLogger().info("[HealthAdjust调试] applyTick - " + "目标: " + targetName + " (" + targetUUID + "), " + "类型: " + effectType + ", " + "量: " + amountPerTick + ", " + "剩余tick: " + remainingTicks + ", " + "武器: " + weaponTitle);
        }

        if (isHealing) {
            applyHealing(target, amountPerTick);
        } else {
            applyDamage(target, amountPerTick);
        }

        remainingTicks--;
        return remainingTicks > 0;
    }

    public void applyOnce() {
        LivingEntity target = findEntityByUuid(targetId);
        if (target == null || target.isDead()) {
            return;
        }

        if (isHealing) {
            applyHealing(target, amountPerTick);
        } else {
            applyDamage(target, amountPerTick);
        }
    }

    private void applyHealing(LivingEntity entity, double amount) {
        if (amount <= 0) return;

        double maxHealth = entity.getMaxHealth();
        double currentHealth = entity.getHealth();
        double newHealth = currentHealth + amount;

        if (newHealth > maxHealth) {
            entity.setHealth(maxHealth);
        } else {
            entity.setHealth(newHealth);
        }
    }

    private void applyDamage(LivingEntity entity, double amount) {
        if (amount <= 0) return;

        if (trueDamage) {
            applyTrueDamage(entity, amount);
        } else {
            applyNormalDamage(entity, amount);
        }
    }

    private void applyTrueDamage(LivingEntity entity, double amount) {
        try {
            CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
            EntityLiving nmsEntity = craftEntity.getHandle();

            nmsEntity.noDamageTicks = 0;

            nmsEntity.damageEntity(DamageSource.OUT_OF_WORLD, (float) amount);

            nmsEntity.noDamageTicks = 0;

        } catch (Exception e) {
            entity.damage(amount);
        }
    }

    private void applyNormalDamage(LivingEntity entity, double amount) {
        try {
            CraftLivingEntity craftEntity = (CraftLivingEntity) entity;
            EntityLiving nmsEntity = craftEntity.getHandle();

            nmsEntity.noDamageTicks = 0;

            entity.damage(amount);

            nmsEntity.noDamageTicks = 0;

        } catch (Exception e) {
            entity.damage(amount);
        }
    }

    public UUID getTargetId() {
        return targetId;
    }

    public boolean isHealing() {
        return isHealing;
    }

    public double getAmountPerTick() {
        return amountPerTick;
    }

    public int getRemainingTicks() {
        return remainingTicks;
    }

    public String getWeaponTitle() {
        return weaponTitle;
    }

    public UUID getShooterId() {
        return shooterId;
    }

    public boolean isSelfEffect() {
        LivingEntity target = findEntityByUuid(targetId);
        if (target instanceof Player) {
            return ((Player) target).getUniqueId().equals(shooterId);
        }
        return false;
    }
}