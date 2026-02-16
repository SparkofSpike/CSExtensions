package org.Spike.CSExtensions.Modifier.Projectiles;

import org.Spike.CSExtensions.CSExtensions;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import org.Spike.CSExtensions.Modifier.Services.ProjectileEffectCoordinator;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ProjectilesController implements Listener {
    private final CSExtensions plugin;
    private final ProjectilesManager projectilesManager;
    private final ProjectileTracker projectileTracker;
    private final ProjectileEventHandler projectileEventHandler;
    private final ProjectileKnockbackCanceller projectileKnockbackCanceller;
    private final ProjectileEffectCoordinator projectileEffectCoordinator;

    public ProjectilesController(CSExtensions plugin, ProjectilesManager projectilesManager) {
        this.plugin = plugin;
        this.projectilesManager = projectilesManager;
        this.projectileEffectCoordinator = new ProjectileEffectCoordinator(plugin);
        this.projectileTracker = new ProjectileTracker(plugin, projectilesManager);
        this.projectileEventHandler = new ProjectileEventHandler(plugin, projectileTracker, projectilesManager, projectileEffectCoordinator);
        this.projectileKnockbackCanceller = new ProjectileKnockbackCanceller(plugin, projectilesManager);

        Bukkit.getPluginManager().registerEvents(projectileKnockbackCanceller, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWeaponShoot(WeaponShootEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

        if (!projectilesManager.hasProjectilesConfig(weaponTitle)) {
            return;
        }

        ProjectilesConfig config = projectilesManager.getProjectilesConfig(weaponTitle);
        if (config == null) {
            return;
        }

        if (config.isHidden()) {
            org.bukkit.entity.Entity entity = event.getProjectile();
            if (entity instanceof Projectile) {
                Projectile projectile = (Projectile) entity;
                ProjectileHider.hideProjectile(projectile);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(player.getName() + " 射击隐藏抛射物: " + weaponTitle +
                            " (实体ID: " + projectile.getEntityId() + ")");
                }
            }
        }

        if (!needsTrackerRegistration(config)) {
            return;
        }

        org.bukkit.entity.Entity entity = event.getProjectile();
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;

            projectileTracker.registerProjectileConditionally(projectile, player, weaponTitle, config);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            logDebugInfo(weaponTitle, config);
        }
    }

    private boolean needsTrackerRegistration(ProjectilesConfig config) {

        return config.getHoming().isEnabled() ||
                config.getReturnConfig().isEnabled() ||
                config.getPenetrate().isEnabled() ||
                config.getBounce().isEnabled();
    }

    private void logDebugInfo(String weaponTitle, ProjectilesConfig config) {
        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("武器 ").append(weaponTitle).append(" 需要Tracker: ");
        debugInfo.append("制导=").append(config.getHoming().isEnabled());
        debugInfo.append(", 返回=").append(config.getReturnConfig().isEnabled());
        debugInfo.append(", 穿透=").append(config.getPenetrate().isEnabled());
        debugInfo.append(", 弹跳=").append(config.getBounce().isEnabled());
        debugInfo.append(", 隐藏=").append(config.isHidden());

        plugin.getLogger().info(debugInfo.toString());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        for (ProjectileData data : projectileTracker.getActiveProjectiles().values()) {
            if (entity.equals(data.getCurrentHomingTarget())) {
                data.onTargetKilled();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for (int entityId : projectileTracker.getPlayerProjectiles(player.getUniqueId())) {
            projectileTracker.removeProjectile(entityId);

            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity entity : world.getEntities()) {
                    if (entity.getEntityId() == entityId) {
                        entity.remove();
                        break;
                    }
                }
            }
        }
    }

    public void reload() {
        projectilesManager.reload();
        projectileTracker.reload();
    }

    public void cleanup() {
        projectilesManager.cleanup();
        projectileTracker.cleanup();
    }

    public ProjectilesManager getProjectilesManager() {
        return projectilesManager;
    }

    public ProjectileTracker getProjectileTracker() {
        return projectileTracker;
    }
}