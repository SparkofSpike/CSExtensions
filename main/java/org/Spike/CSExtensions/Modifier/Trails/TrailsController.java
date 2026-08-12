package org.Spike.CSExtensions.Modifier.Trails;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import com.shampaggon.crackshot.CSUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.logging.Level;

public class TrailsController implements Listener {
    private final CSExtensions plugin;
    private final ModifierManager modifierManager;
    private TrailConfigManager trailConfigManager;
    private TrailEffectManager trailEffectManager;
    private CSUtility csUtility;

    public TrailsController(CSExtensions plugin, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.modifierManager = modifierManager;
        this.trailConfigManager = new TrailConfigManager(plugin, modifierManager);
        this.trailEffectManager = new TrailEffectManager(plugin, trailConfigManager);

        try {
            this.csUtility = new CSUtility();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "无法初始化CSUtility，某些功能可能受限", e);
        }
    }

        public void reload() {
        trailConfigManager.reload();
        trailEffectManager.cleanup();
    }

        public void cleanup() {
        trailEffectManager.cleanup();
        trailConfigManager.cleanup();
    }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponShoot(WeaponShootEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

                org.bukkit.entity.Entity entity = event.getProjectile();
        Projectile projectile = null;

        if (entity instanceof Projectile) {
            projectile = (Projectile) entity;
        }

                if (!trailConfigManager.hasTrails(weaponTitle)) {
            return;
        }

        TrailConfig config = trailConfigManager.getTrailConfig(weaponTitle);
        if (config == null) {
            return;
        }

                if (config.isProjectileWeapon() && projectile != null) {
                        trailEffectManager.startProjectileTrail(projectile, weaponTitle, player);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(player.getName() + " 射击 " + weaponTitle +
                        " (实体弹道) - 启动轨迹, Length: " + config.getLength() + " ticks, " +
                        "额外粒子: " + config.getExtraParticlesAhead());
            }
        } else if (config.isEnergyWeapon()) {
                        trailEffectManager.startEnergyTrail(
                    player,
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    weaponTitle
            );

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(player.getName() + " 射击 " + weaponTitle +
                        " (能量武器) - 瞬间轨迹, Length: " + config.getLength() + " ticks, " +
                        "类型: " + config.getTrailType() + ", 半径: " + config.getRadius());
            }
        }
    }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) {
            return;
        }

        Player shooter = (Player) event.getEntity().getShooter();
        Projectile projectile = event.getEntity();

                if (csUtility != null) {
            String weaponTitle = csUtility.getWeaponTitle(projectile);
            if (weaponTitle != null && trailConfigManager.hasTrails(weaponTitle)) {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (trailConfigManager.hasTrails(weaponTitle)) {
                        TrailConfig config = trailConfigManager.getTrailConfig(weaponTitle);
                        if (config != null && config.isProjectileWeapon()
                                && !trailEffectManager.hasActiveTrail(projectile.getUniqueId())) {
                            trailEffectManager.startProjectileTrail(projectile, weaponTitle, shooter);
                        }
                    }
                }, 1L);
            }
        }
    }

        @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
                if (event.getEntity() instanceof Projectile) {
            trailEffectManager.stopTrail(event.getEntity().getUniqueId());
        }
    }

        public TrailConfigManager getConfigManager() {
        return trailConfigManager;
    }

        public TrailEffectManager getEffectManager() {
        return trailEffectManager;
    }

}