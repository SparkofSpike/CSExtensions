package org.Spike.CSExtensions.Modifier.HealthAdjust;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponReloadCompleteEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.Spike.CSExtensions.Modifier.Projectiles.ProjectilesManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HealthAdjustHandler implements Listener {
    private final CSExtensions plugin;
    private final HealthAdjustManager healthAdjustManager;
    private final ProjectilesManager projectilesManager;
    private final Map<UUID, List<ActiveHealthEffect>> activeEffects = new HashMap<>();
    private final MergedEffectManager mergedEffectManager = new MergedEffectManager();
    private final Map<UUID, String> activeWeapons = new ConcurrentHashMap<>();

    private int schedulerTaskId = -1;

    public HealthAdjustHandler(CSExtensions plugin, ProjectilesManager projectilesManager) {
        this.plugin = plugin;
        this.healthAdjustManager = new HealthAdjustManager(plugin,
                new ModifierManager(plugin));
        this.projectilesManager = projectilesManager;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        startScheduler();

        plugin.getLogger().info("HealthAdjust系统已初始化");
    }

    private void startScheduler() {
        schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            mergedEffectManager.advanceTick();

            applyMergedEffects();

            updateActiveEffects();

        }, 1L, 1L);
    }

    private void applyMergedEffects() {
        double totalHealing = mergedEffectManager.getTotalHealingForCurrentTick();
        double totalDamage = mergedEffectManager.getTotalDamageForCurrentTick();

        if (totalHealing > 0 || totalDamage > 0) {

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[HealthAdjust] 当前tick自我合并效果 - 治疗: " +
                        totalHealing + ", 伤害: " + totalDamage);
            }
        }
    }

    private void updateActiveEffects() {
        Iterator<Map.Entry<UUID, List<ActiveHealthEffect>>> playerIterator = activeEffects.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<UUID, List<ActiveHealthEffect>> entry = playerIterator.next();
            UUID playerId = entry.getKey();
            List<ActiveHealthEffect> effects = entry.getValue();

            LivingEntity entity = findEntityByUuid(playerId);
            if (entity == null) {
                playerIterator.remove();
                continue;
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[HealthAdjust] 更新效果 - " +
                        "实体: " + entity.getName() + " (" + entity.getUniqueId() + "), " +
                        "效果数量: " + effects.size());
            }

            Iterator<ActiveHealthEffect> effectIterator = effects.iterator();
            while (effectIterator.hasNext()) {
                ActiveHealthEffect effect = effectIterator.next();
                if (!effect.applyTick()) {
                    effectIterator.remove();
                }
            }

            if (effects.isEmpty()) {
                playerIterator.remove();
            }
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[HealthAdjust] 处理玩家 " + playerId + " 的效果，数量: " + effects.size());
            }
        }
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

    public void triggerEffects(Player player, String weaponTitle,
                               HealthAdjustConfig.Trigger trigger, LivingEntity target) {
        HealthAdjustConfig config = healthAdjustManager.getHealthAdjustConfig(weaponTitle);
        if (config == null || !config.isEnabled()) {
            return;
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[HealthAdjust] 触发效果 - 武器: " + weaponTitle +
                    ", 触发: " + trigger + ", 玩家: " + player.getName());
        }

        handleSelfEffects(player, weaponTitle, trigger, config);

        if (target != null && target != player) {
            handleTargetEffects(player, weaponTitle, target, config, trigger);
        }
    }


    private void handleSelfEffects(Player player, String weaponTitle,
                                   HealthAdjustConfig.Trigger trigger, HealthAdjustConfig config) {
        UUID playerId = player.getUniqueId();

        for (HealthAdjustEffect effect : config.getEffectsForTrigger(trigger)) {
            boolean isTargetEffect =
                    config.getHealTargetInstant().contains(effect) ||
                            config.getDamageTargetInstant().contains(effect) ||
                            config.getHealTargetConstant().contains(effect) ||
                            config.getDamageTargetConstant().contains(effect);

            if (isTargetEffect) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust调试] 跳过目标效果: " +
                            (effect.isHealing() ? "治疗" : "伤害") + ", 触发: " + trigger);
                }
                continue;
            }

            if (effect.isInstant()) {
                ActiveHealthEffect activeEffect = new ActiveHealthEffect(
                        plugin, playerId, effect.isHealing(), effect.getAmountPerTick(),
                        0, weaponTitle, playerId, effect.isTrueDamage()
                );
                activeEffect.applyOnce();

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 应用自我即时效果 - " +
                            (effect.isHealing() ? "治疗" : "伤害") + ": " +
                            effect.getAmountPerTick() + ", 触发: " + trigger);
                }
            } else {
                mergedEffectManager.addSelfEffect(weaponTitle, effect, 0);

                ActiveHealthEffect activeEffect = new ActiveHealthEffect(
                        plugin, playerId, effect.isHealing(), effect.getAmountPerTick(),
                        effect.getDurationTicks(), weaponTitle, playerId, effect.isTrueDamage()
                );

                activeEffects.computeIfAbsent(playerId, k -> new ArrayList<>())
                        .add(activeEffect);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 添加自我持续效果 - " +
                            (effect.isHealing() ? "治疗" : "伤害") + ": " +
                            effect.getAmountPerTick() + "/tick, 持续: " +
                            effect.getDurationTicks() + "tick, 触发: " + trigger);
                }
            }
        }
    }

    private void handleTargetEffects(Player shooter, String weaponTitle,
                                     LivingEntity target, HealthAdjustConfig config,
                                     HealthAdjustConfig.Trigger trigger) {
        if (target instanceof ArmorStand) return;

        for (HealthAdjustEffect effect : config.getHealTargetInstant()) {
            if (effect.getTrigger() == trigger && effect.isInstant()) {
                ActiveHealthEffect healingEffect = new ActiveHealthEffect(plugin, target.getUniqueId(), true, effect.getAmountPerTick(), 0, weaponTitle, shooter.getUniqueId(), effect.isTrueDamage());
                healingEffect.applyOnce();
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 应用目标即时治疗: " +
                            effect.getAmountPerTick() + ", 触发: " + trigger);
                }
            }
        }

        for (HealthAdjustEffect effect : config.getDamageTargetInstant()) {
            if (effect.getTrigger() == trigger && effect.isInstant()) {
                ActiveHealthEffect damageEffect = new ActiveHealthEffect(plugin, target.getUniqueId(), false, effect.getAmountPerTick(), 0, weaponTitle, shooter.getUniqueId(), effect.isTrueDamage());
                damageEffect.applyOnce();
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 应用目标即时伤害: " +
                            effect.getAmountPerTick() + ", 触发: " + trigger);
                }
            }
        }

        for (HealthAdjustEffect effect : config.getHealTargetConstant()) {
            if (effect.getTrigger() == trigger) {
                ActiveHealthEffect activeEffect = new ActiveHealthEffect(
                        plugin, target.getUniqueId(), true, effect.getAmountPerTick(),
                        effect.getDurationTicks(), weaponTitle, shooter.getUniqueId(), effect.isTrueDamage()
                );

                activeEffects.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>())
                        .add(activeEffect);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 添加目标持续治疗: " +
                            effect.getAmountPerTick() + "/tick, 持续: " +
                            effect.getDurationTicks() + "tick, 触发: " + trigger);
                }
            }
        }

        for (HealthAdjustEffect effect : config.getDamageTargetConstant()) {
            if (effect.getTrigger() == trigger) {
                ActiveHealthEffect activeEffect = new ActiveHealthEffect(
                        plugin, target.getUniqueId(), false, effect.getAmountPerTick(),
                        effect.getDurationTicks(), weaponTitle, shooter.getUniqueId(), effect.isTrueDamage()
                );

                activeEffects.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>())
                        .add(activeEffect);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[HealthAdjust] 添加目标持续伤害: " +
                            effect.getAmountPerTick() + "/tick, 持续: " +
                            effect.getDurationTicks() + "tick, 触发: " + trigger);
                }
            }
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


    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponShoot(WeaponShootEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

        triggerEffects(player, weaponTitle, HealthAdjustConfig.Trigger.SHOOT, null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

        if (player == null || !(event.getVictim() instanceof LivingEntity)) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getVictim();

        triggerEffects(player, weaponTitle, HealthAdjustConfig.Trigger.HIT, victim);

        if (event.isCritical()) {
            triggerEffects(player, weaponTitle, HealthAdjustConfig.Trigger.CRIT, victim);
        }

        if (event.isHeadshot()) {
            triggerEffects(player, weaponTitle, HealthAdjustConfig.Trigger.HEADSHOT, victim);
        }

        activeWeapons.put(player.getUniqueId(), event.getWeaponTitle());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

        triggerEffects(player, weaponTitle, HealthAdjustConfig.Trigger.RELOAD, null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeathMonitor(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            String weaponTitle = activeWeapons.get(killer.getUniqueId());
            if (weaponTitle != null) {
                triggerEffects(killer, weaponTitle, HealthAdjustConfig.Trigger.KILL, victim);
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayerEffects(event.getPlayer().getUniqueId());
        activeWeapons.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanupPlayerEffects(event.getEntity().getUniqueId());
        activeWeapons.remove(event.getEntity().getUniqueId());
    }

    private void cleanupPlayerEffects(UUID playerId) {
        activeEffects.remove(playerId);


    }

    public void reload() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
        }

        activeEffects.clear();
        mergedEffectManager.cleanupAll();

        healthAdjustManager.reload();

        startScheduler();

        plugin.getLogger().info("HealthAdjust系统已重新加载");
    }

    public void cleanup() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
        }

        activeEffects.clear();
        mergedEffectManager.cleanupAll();
        healthAdjustManager.cleanup();
    }
}