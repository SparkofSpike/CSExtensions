package org.Spike.CSExtensions.Modifier.Services;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Projectiles.ProjectileHider;
import org.Spike.CSExtensions.Modifier.Projectiles.ProjectilesConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class ProjectileEffectCoordinator {
    private final CSExtensions plugin;

    public ProjectileEffectCoordinator(CSExtensions plugin) {
        this.plugin = plugin;
    }

    public void applyEffectsForBounceProjectile(Projectile projectile, Player shooter, String weaponTitle,
                                                ProjectilesConfig config) {
        applyHiddenEffect(projectile, config);

        triggerTrailsEffect(projectile);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("协调器：为弹跳抛射物应用效果 - ID=" + projectile.getEntityId() +
                    ", 武器=" + weaponTitle + ", Hidden=" + config.isHidden());
        }
    }

    public void applyEffectsForPenetrateProjectile(Projectile projectile, Player shooter, String weaponTitle,
                                                   ProjectilesConfig config) {
        applyHiddenEffect(projectile, config);

        applyKnockbackEffect(projectile, config);

        triggerTrailsEffect(projectile);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("协调器：为穿透抛射物应用效果 - ID=" + projectile.getEntityId() +
                    ", 武器=" + weaponTitle + ", Hidden=" + config.isHidden());
        }
    }

    private void applyKnockbackEffect(Projectile projectile, ProjectilesConfig config) {
        if (config.getNoknock() != ProjectilesConfig.KnockbackType.NONE) {
            projectile.setMetadata("CSE_NoKnockback",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, config.getNoknock().name()));
        }
    }

    private void applyHiddenEffect(Projectile projectile, ProjectilesConfig config) {
        if (config.isHidden() && ProjectileHider.isInitialized()) {
            ProjectileHider.hideProjectile(projectile);
        }
    }

    private void triggerTrailsEffect(Projectile projectile) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (projectile.isValid() && !projectile.isDead()) {
                org.bukkit.event.entity.ProjectileLaunchEvent launchEvent =
                        new org.bukkit.event.entity.ProjectileLaunchEvent(projectile);
                Bukkit.getPluginManager().callEvent(launchEvent);
            }
        }, 1L);
    }

    public void cleanup() {
    }
}