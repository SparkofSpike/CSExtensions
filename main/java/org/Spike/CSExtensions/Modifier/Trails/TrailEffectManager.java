package org.Spike.CSExtensions.Modifier.Trails;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class TrailEffectManager {
    private final CSExtensions plugin;
    private final TrailConfigManager configManager;

    public TrailEffectManager(CSExtensions plugin, TrailConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void startProjectileTrail(Projectile projectile, String weaponId, Player shooter) {
        TrailConfig config = configManager.getTrailConfig(weaponId);
        if (config == null || !config.isProjectileWeapon()) {
            return;
        }

        ProjectileTrail trail = new ProjectileTrail(projectile, config, shooter);
        trail.start();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("启动实体弹道轨迹: " + weaponId +
                    ", 持续时间: " + config.getLength() + " ticks");
        }
    }

    public void startEnergyTrail(Player shooter, Location startLoc, Vector direction, String weaponId) {
        TrailConfig config = configManager.getTrailConfig(weaponId);
        if (config == null || !config.isEnergyWeapon()) {
            return;
        }

        EnergyTrail trail = new EnergyTrail(shooter, startLoc, direction, config);
        trail.spawn();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("启动能量武器轨迹: " + weaponId +
                    ", 距离: " + config.getLength() + " 格, " +
                    "类型: " + config.getTrailType());
        }
    }

    public void cleanup() {
        for (ProjectileTrail trail : projectileTrails.values()) {
            trail.stop();
        }
        projectileTrails.clear();
    }

    private final Map<UUID, ProjectileTrail> projectileTrails = new HashMap<>();

    private class ProjectileTrail {
        private final Projectile projectile;
        private final TrailConfig config;
        private final Player shooter;
        private BukkitRunnable task;
        private int tickCounter = 0;

        ProjectileTrail(Projectile projectile, TrailConfig config, Player shooter) {
            this.projectile = projectile;
            this.config = config;
            this.shooter = shooter;
            projectileTrails.put(projectile.getUniqueId(), this);
        }

        void start() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isValid()) {
                        stop();
                        return;
                    }

                    spawnParticles();
                    tickCounter++;
                }
            };

            task.runTaskTimer(plugin, 0L, 1L);
        }

        void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
            projectileTrails.remove(projectile.getUniqueId());
        }

        private boolean isValid() {
            return !projectile.isDead() && projectile.isValid() && tickCounter < config.getLength();
        }

        private void spawnParticles() {
            Location location = projectile.getLocation();
            Vector direction = projectile.getVelocity().normalize();

            int totalPositions = config.getExtraParticlesAhead() + 1;

            double spacing = config.getExtraParticlesInterval() > 0 ?
                    config.getExtraParticlesInterval() : 0.5;

            for (int i = 0; i < totalPositions; i++) {
                double distance = i * spacing;
                Vector offset = direction.clone().multiply(distance);
                Location particleLoc = location.clone().add(offset);

                spawnParticlesAtLocation(particleLoc, config);
            }
        }
    }

    private class EnergyTrail {
        private final Player shooter;
        private final Location startLoc;
        private final Vector direction;
        private final TrailConfig config;

        EnergyTrail(Player shooter, Location startLoc, Vector direction, TrailConfig config) {
            this.shooter = shooter;
            this.startLoc = startLoc.clone();
            this.direction = direction.clone().normalize();
            this.config = config;
        }

        void spawn() {
            double maxDistance = config.getLength();
            Location endLoc = calculateEndLocation(startLoc, direction, maxDistance);

            if (config.getTrailType() == TrailType.STRAIGHT) {
                spawnStraightTrail(startLoc, endLoc);
            } else if (config.getTrailType() == TrailType.CIRCLE) {
                spawnCircleTrail(startLoc, endLoc);
            }
        }

        private Location calculateEndLocation(Location start, Vector dir, double maxDistance) {
            World world = start.getWorld();
            if (world == null) {
                return start.clone().add(dir.clone().multiply(maxDistance));
            }

            GoThrough goThrough = config.getGoThrough();

            if (goThrough.canGoThroughAny()) {
                return start.clone().add(dir.clone().multiply(maxDistance));
            }

            Location current = start.clone();
            double step = 0.5;
            double traveled = 0;

            while (traveled < maxDistance) {
                current.add(dir.clone().multiply(step));
                traveled += step;

                if (!goThrough.canGoThroughWalls()) {
                    if (!current.getBlock().isEmpty()) {
                        return current;
                    }
                }

            }

            return current;
        }

        private void spawnStraightTrail(Location start, Location end) {
            Vector path = end.toVector().subtract(start.toVector());
            double totalDistance = path.length();
            path.normalize();

            int totalPositions = config.getExtraParticlesAhead() + 1;
            double spacing = totalDistance / Math.max(1, totalPositions - 1);

            for (int i = 0; i < totalPositions; i++) {
                double distance = i * spacing;
                Vector offset = path.clone().multiply(distance);
                Location particleLoc = start.clone().add(offset);

                spawnParticlesAtLocation(particleLoc, config);
            }
        }

        private void spawnCircleTrail(Location start, Location end) {
            Vector path = end.toVector().subtract(start.toVector());
            double totalDistance = path.length();
            path.normalize();

            int totalSections = config.getExtraParticlesAhead() + 1;
            double spacing = totalDistance / Math.max(1, totalSections - 1);

            Vector perpendicular = getPerpendicularVector(path);

            for (int section = 0; section < totalSections; section++) {
                double distance = section * spacing;
                Vector offset = path.clone().multiply(distance);
                Location sectionCenter = start.clone().add(offset);

                spawnCircleSection(sectionCenter, path, perpendicular, config);
            }
        }

        private void spawnCircleSection(Location center, Vector direction, Vector perpendicular, TrailConfig config) {
            double radius = config.getRadius();
            int points = config.getPoints();

            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;

                Vector rotated = rotateVector(perpendicular, direction, angle);
                Location pointLoc = center.clone().add(rotated.multiply(radius));

                spawnParticlesAtLocation(pointLoc, config);
            }
        }

        private Vector getPerpendicularVector(Vector vector) {
            Vector[] candidates = {
                    new Vector(-vector.getZ(), 0, vector.getX()),
                    new Vector(0, 1, 0),
                    new Vector(1, 0, 0)
            };

            for (Vector candidate : candidates) {
                if (candidate.lengthSquared() > 0.01) {
                    return candidate.normalize();
                }
            }

            return new Vector(1, 0, 0);
        }

        private Vector rotateVector(Vector vector, Vector axis, double angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double dot = vector.dot(axis);
            Vector parallel = axis.clone().multiply(dot);

            Vector perpendicular = vector.clone().subtract(parallel);

            Vector w = axis.getCrossProduct(perpendicular);

            return parallel.add(perpendicular.multiply(cos)).add(w.multiply(sin));
        }
    }

    private void spawnParticlesAtLocation(Location location, TrailConfig config) {
        if (location.getWorld() == null) return;

        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        for (String effectName : config.getEffects()) {
            try {
                ParticleUtil.spawnParticleByName(
                        location,
                        effectName,
                        config.getParticleColor(),
                        (float) config.getSpeed(),
                        (float) config.getSpeed(),
                        config.getAmount()
                );
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("生成粒子失败: " + effectName + " at " + location);
                }
            }
        }
    }

    public void stopTrail(UUID projectileId) {
        ProjectileTrail trail = projectileTrails.remove(projectileId);
        if (trail != null) {
            trail.stop();
        }
    }

    public boolean hasActiveTrail(UUID projectileId) {
        return projectileTrails.containsKey(projectileId);
    }
}