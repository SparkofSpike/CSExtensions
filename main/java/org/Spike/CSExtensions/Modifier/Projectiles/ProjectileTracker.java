package org.Spike.CSExtensions.Modifier.Projectiles;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Services.RaycastUtil;
import org.Spike.CSExtensions.Modifier.Services.AimHeightCalculator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.util.Vector;

public class ProjectileTracker {
    private final CSExtensions plugin;
    private final ProjectilesManager projectilesManager;

    private final Map<Integer, ProjectileData> allProjectiles = new ConcurrentHashMap<>();

    private final Set<Integer> homingProjectiles = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> returnProjectiles = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> normalProjectiles = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, Set<Integer>> playerProjectiles = new ConcurrentHashMap<>();
    private final Set<Integer> pendingRemoval = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask homingTask;
    private BukkitTask returnTask;

    private volatile boolean isApplyingHoming = false;
    private final Set<Integer> currentlySteering = new HashSet<>();

    private final Map<Integer, Projectile> projectileCache = new ConcurrentHashMap<>();

    public ProjectileTracker(CSExtensions plugin, ProjectilesManager projectilesManager) {
        this.plugin = plugin;
        this.projectilesManager = projectilesManager;
        startOptimizedTasks();
        startOptimizedCleanupTask();
    }

    private void startOptimizedTasks() {

        homingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (homingProjectiles.isEmpty()) {
                //todo
                return;
            }
            updateOptimizedHomingProjectiles();
        }, 1L, 1L);

        returnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (returnProjectiles.isEmpty()) {
                return;
            }
            updateOptimizedReturnProjectiles();
        }, 1L, 1L);

        plugin.getLogger().info("ProjectileTracker优化任务已启动");
    }

    public void registerInheritedProjectile(Projectile newProjectile, Player shooter,
                                            String weaponTitle, ProjectileData originalData) {

        ProjectilesConfig config = projectilesManager.getProjectilesConfig(weaponTitle);
        if (config == null) return;

        ProjectileData newData = new ProjectileData(newProjectile, shooter, weaponTitle, config,
                originalData.getBounceCount(),
                originalData.getPenetrateCount(),
                true, originalData.isHomingActivated());
        newData.setCurrentDamageMultiplier(originalData.getCurrentDamageMultiplier());
        newData.setCurrentVelocityMultiplier(originalData.getCurrentVelocityMultiplier());

        newData.setBaseSpeed(originalData.getBaseSpeed());

        newData.getTrackedTargets().addAll(originalData.getTrackedTargets());

        if (config.getHoming().getUpdate() == ProjectilesConfig.HomingUpdate.NEVER) {
            newData.setInitialHomingTarget(originalData.getInitialHomingTarget());
            newData.setHomingTargetSearched(originalData.isHomingTargetSearched());
        }

        if (originalData.hasJustBounced()) {
            newData.setJustBounced(true);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Homing继承] 继承justBounced标记");
            }
        }

        if (originalData.hasJustPenetrated()) {
            newData.setJustPenetrated(true);
        }

        if (originalData.getCurrentHomingTarget() != null) {
            newData.setCurrentHomingTarget(originalData.getCurrentHomingTarget());
        }

        registerProjectileInternal(newProjectile.getEntityId(), newData, shooter, config);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Homing继承] 弹跳后抛射物继承 - 激活状态: " +
                    originalData.isHomingActivated() + " -> " + newData.isHomingActivated() +
                    ", justBounced: " + originalData.hasJustBounced() + " -> " + newData.hasJustBounced() +
                    ", NEVER模式初始目标: " + (newData.getInitialHomingTarget() != null ? newData.getInitialHomingTarget().getType() : "无"));
        }
    }

    private void registerProjectileInternal(int entityId, ProjectileData data, Player shooter, ProjectilesConfig config) {
        allProjectiles.put(entityId, data);

        if (config.getHoming().isEnabled()) {
            homingProjectiles.add(entityId);
        }

        if (config.getReturnConfig().isEnabled()) {
            returnProjectiles.add(entityId);
        }

        if (!config.getHoming().isEnabled() && !config.getReturnConfig().isEnabled()) {
            normalProjectiles.add(entityId);
        }

        playerProjectiles.computeIfAbsent(shooter.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(entityId);
    }

    public void registerProjectile(Projectile projectile, Player shooter, String weaponTitle,
                                   Integer bounceCount, Integer penetrateCount, Double damageMultiplier) {

        ProjectilesConfig config = projectilesManager.getProjectilesConfig(weaponTitle);
        if (config == null) {
            plugin.getLogger().warning("注册抛射物失败: 配置为空 - " + weaponTitle);
            return;
        }

        ProjectileData data;
        if (bounceCount != null || penetrateCount != null) {
            data = new ProjectileData(projectile, shooter, weaponTitle, config,
                    bounceCount != null ? bounceCount : 0,
                    penetrateCount != null ? penetrateCount : 0);
        } else {
            data = new ProjectileData(projectile, shooter, weaponTitle, config);
        }

        if (damageMultiplier != null) {
            data.setCurrentDamageMultiplier(damageMultiplier);
        }

        registerProjectileInternal(projectile.getEntityId(), data, shooter, config);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("=== 注册抛射物调试 ===");
            plugin.getLogger().info("抛射物ID: " + projectile.getEntityId());
            plugin.getLogger().info("类型: " + projectile.getType());
            plugin.getLogger().info("武器: " + weaponTitle);
            plugin.getLogger().info("玩家: " + shooter.getName());
            plugin.getLogger().info("追踪器当前总数: " + allProjectiles.size());
            plugin.getLogger().info("分类: 制导=" + homingProjectiles.size() +
                    ", 返回=" + returnProjectiles.size() +
                    ", 普通=" + normalProjectiles.size());
        }
    }

    public void registerProjectile(Projectile projectile, Player shooter, String weaponTitle) {
        registerProjectile(projectile, shooter, weaponTitle, null, null, null);
    }

    public void registerProjectile(Projectile projectile, Player shooter, String weaponTitle,
                                   Integer bounceCount, Integer penetrateCount) {
        registerProjectile(projectile, shooter, weaponTitle, bounceCount, penetrateCount, null);
    }

    public void registerProjectileConditionally(Projectile projectile, Player shooter,
                                                String weaponTitle, ProjectilesConfig config) {
        ProjectileData data = new ProjectileData(projectile, shooter, weaponTitle, config);

        int entityId = projectile.getEntityId();
        allProjectiles.put(entityId, data);

        projectileCache.put(entityId, projectile);

        if (config.getHoming().isEnabled()) {
            homingProjectiles.add(entityId);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Tracker] 注册制导抛射物: " + entityId);
            }
        }

        if (config.getReturnConfig().isEnabled()) {
            returnProjectiles.add(entityId);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Tracker] 注册返回抛射物: " + entityId);
            }
        }

        playerProjectiles.computeIfAbsent(shooter.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(entityId);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "[Tracker] 条件注册完成: ID=%d, 武器=%s, 制导=%s, 返回=%s, 穿透=%s, 弹跳=%s",
                    entityId, weaponTitle,
                    config.getHoming().isEnabled(),
                    config.getReturnConfig().isEnabled(),
                    config.getPenetrate().isEnabled(),
                    config.getBounce().isEnabled()
            ));
        }
    }

    public ProjectileData getProjectileData(int entityId) {
        ProjectileData data = allProjectiles.get(entityId);

        if (data == null && plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().warning("抛射物数据获取失败! ID: " + entityId);
            plugin.getLogger().warning("当前总数: " + allProjectiles.size());
        }

        return data;
    }

    public Set<Integer> getPlayerProjectiles(UUID playerId) {
        return playerProjectiles.getOrDefault(playerId, Collections.emptySet());
    }

    public void removeProjectile(int entityId) {
        ProjectileData data = allProjectiles.remove(entityId);
        if (data == null) return;

        homingProjectiles.remove(entityId);
        returnProjectiles.remove(entityId);
        normalProjectiles.remove(entityId);

        Set<Integer> playerProjs = playerProjectiles.get(data.getShooterId());
        if (playerProjs != null) {
            playerProjs.remove(entityId);
            if (playerProjs.isEmpty()) {
                playerProjectiles.remove(data.getShooterId());
            }
        }

        pendingRemoval.remove(entityId);
    }

    private void updateOptimizedHomingProjectiles() {
        if (isApplyingHoming) {
            return;
        }

        isApplyingHoming = true;
        try {
            long currentTime = System.currentTimeMillis();
            List<Integer> toRemove = new ArrayList<>();

            if (plugin.getConfig().getBoolean("debug", false) && homingProjectiles.size() > 0) {
                plugin.getLogger().info("[Homing调试] 当前制导抛射物数量: " + homingProjectiles.size());
            }

            for (int entityId : homingProjectiles) {
                ProjectileData data = allProjectiles.get(entityId);
                if (data == null) {
                    toRemove.add(entityId);
                    continue;
                }

                Projectile projectile = getProjectile(entityId);
                if (projectile == null || projectile.isDead() || !projectile.isValid()) {
                    toRemove.add(entityId);
                    continue;
                }

                if (!data.isHomingActivated()) {
                    ProjectilesConfig.HomingConfig homingConfig = data.getConfig().getHoming();

                    if (!homingConfig.isInitialLock()) {
                        boolean shouldActivate = false;

                        switch (homingConfig.getUpdate()) {
                            case ALWAYS:
                                shouldActivate = true;
                                break;
                            case PENETRATED:
                                shouldActivate = data.hasJustPenetrated();
                                break;
                            case BOUNCED:
                                shouldActivate = data.hasJustBounced();
                                break;
                            case KILLED:
                            case NEVER:
                                break;
                        }

                        if (shouldActivate) {
                            data.setHomingActivated(true);
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[Homing] 条件满足，激活制导系统，抛射物ID: " + entityId);
                            }
                        }
                    }

                    if (!data.isHomingActivated()) {
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("[Homing] 制导未激活，跳过处理，抛射物ID: " + entityId);
                        }
                        continue;
                    }
                }

                ProjectilesConfig.HomingConfig homingConfig = data.getConfig().getHoming();
                ProjectilesConfig.HomingUpdate updateCondition = homingConfig.getUpdate();

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Homing调试] 抛射物ID: " + entityId +
                            ", 更新条件: " + updateCondition +
                            ", 初始锁定: " + homingConfig.isInitialLock() +
                            ", 当前目标: " + (data.getCurrentHomingTarget() != null ? data.getCurrentHomingTarget().getType() : "无") +
                            ", 制导激活: " + data.isHomingActivated());
                }

                boolean shouldUpdate = false;
                boolean targetInvalid = false;

                LivingEntity currentTarget = data.getCurrentHomingTarget();
                if (currentTarget != null) {
                    if (currentTarget.isDead() || !currentTarget.isValid()) {
                        targetInvalid = true;
                        shouldUpdate = updateCondition != ProjectilesConfig.HomingUpdate.NEVER;
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("[Homing] 目标无效，需要更新: " + shouldUpdate);
                        }
                    }
                }

                if (currentTarget == null) {
                    if (homingConfig.isInitialLock()) {
                        shouldUpdate = true;
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            plugin.getLogger().info("[Homing] InitialLock=true，首次目标锁定");
                        }
                    } else {
                        switch (updateCondition) {
                            case ALWAYS:
                                shouldUpdate = true;
                                break;
                            case PENETRATED:
                                shouldUpdate = data.hasJustPenetrated();
                                break;
                            case BOUNCED:
                                shouldUpdate = data.hasJustBounced();
                                break;
                            case KILLED:
                                break;
                            case NEVER:
                                shouldUpdate = !data.isHomingTargetSearched();
                                break;
                        }

                        if (plugin.getConfig().getBoolean("debug", false) && shouldUpdate) {
                            plugin.getLogger().info("[Homing] InitialLock=false，满足条件激活：" + updateCondition);
                        }
                    }
                } else {
                    switch (updateCondition) {
                        case ALWAYS:
                            shouldUpdate = true;
                            break;
                        case PENETRATED:
                            shouldUpdate = data.hasJustPenetrated();
                            break;
                        case BOUNCED:
                            shouldUpdate = data.hasJustBounced();
                            break;
                        case KILLED:
                            if (currentTarget.isDead()) {
                                shouldUpdate = true;
                            }
                            break;
                        case NEVER:
                            shouldUpdate = false;
                            break;
                    }
                }

                if (shouldUpdate) {
                    updateHomingTarget(projectile, data);

                    if (data.hasJustPenetrated() || data.hasJustBounced()) {
                        data.resetEventFlags();
                    }
                }

                currentTarget = data.getCurrentHomingTarget();
                if (currentTarget != null && !currentTarget.isDead() && currentTarget.isValid()) {
                    if (!projectile.getWorld().equals(currentTarget.getWorld())) {
                        data.setCurrentHomingTarget(null);
                        continue;
                    }
                    applyHomingSteering(projectile, data, homingConfig.getTurnSpeed());

                    if (plugin.getConfig().getBoolean("debug", false) && !homingConfig.isInitialLock()) {
                        long timeSinceActivation = currentTime - data.getLastHomingUpdate();
                        if (timeSinceActivation < 1000) {
                            plugin.getLogger().info("[Homing] 延迟激活制导，目标：" + currentTarget.getType());
                        }
                    }
                } else if (plugin.getConfig().getBoolean("debug", false) && data.isHomingActivated()) {
                    plugin.getLogger().info("[Homing] 有制导但无有效目标");
                }
            }

            for (int entityId : toRemove) {
                removeProjectile(entityId);
            }
        } finally {
            isApplyingHoming = false;
        }
    }

    private void updateHomingTarget(Projectile projectile, ProjectileData data) {
        ProjectilesConfig.HomingUpdate updateCondition = data.getConfig().getHoming().getUpdate();

        if (updateCondition == ProjectilesConfig.HomingUpdate.NEVER) {
            if (data.isHomingTargetSearched()) {
                return;
            }

            LivingEntity initialTarget = data.getInitialHomingTarget();
            if (initialTarget != null) {
                data.setCurrentHomingTarget(initialTarget);
                data.setLastHomingUpdate(System.currentTimeMillis());
                data.setHomingTargetSearched(true);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Homing] NEVER模式使用初始目标: " + initialTarget.getType());
                }
                return;
            }
        }

        LivingEntity currentTarget = data.getCurrentHomingTarget();
        if (currentTarget != null) {
            data.addTrackedTarget(currentTarget);
        }

        LivingEntity newTarget = findHomingTarget(projectile, data);

        if (newTarget != null) {
            if (data.hasTrackedTarget(newTarget)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Homing] 新目标已被追踪过，跳过: " + newTarget.getType());
                }
            }

            data.setCurrentHomingTarget(newTarget);
            data.setLastHomingUpdate(System.currentTimeMillis());

            if (updateCondition == ProjectilesConfig.HomingUpdate.NEVER) {
                data.setInitialHomingTarget(newTarget);
                data.setHomingTargetSearched(true);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Homing] 锁定新目标: " + newTarget.getType() +
                        ", 更新模式: " + updateCondition +
                        ", 已追踪目标数: " + data.getTrackedTargets().size());
            }
        } else {
            if (updateCondition == ProjectilesConfig.HomingUpdate.NEVER) {
                data.setHomingTargetSearched(true);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Homing] 未找到合适目标");
            }
        }
    }

    private LivingEntity findHomingTarget(Projectile projectile, ProjectileData data) {
        ProjectilesConfig.HomingConfig homingConfig = data.getConfig().getHoming();
        Location projectileLoc = projectile.getLocation();
        double range = homingConfig.getRange();
        double angle = Math.toRadians(homingConfig.getAngle());

        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        Vector projectileDir = projectile.getVelocity().normalize();

        Location shooterLocation = null;
        if (data.getShooterId() != null) {
            Player shooter = Bukkit.getPlayer(data.getShooterId());
            if (shooter != null && shooter.isOnline()) {
                shooterLocation = shooter.getEyeLocation();
            }
        }

        if (shooterLocation == null) {
            shooterLocation = projectileLoc;
        }

        List<Entity> nearbyEntities = projectile.getNearbyEntities(range, range, range);

        for (Entity unprocessedentity : nearbyEntities) {
            if (!(unprocessedentity instanceof LivingEntity)) {
                continue;
            }

            LivingEntity entity = (LivingEntity) unprocessedentity;

            if (entity.getEntityId() == projectile.getEntityId() ||
                    entity.isDead() ||
                    (entity instanceof Player && ((Player) entity).getUniqueId().equals(data.getShooterId()))) {
                continue;
            }
            if (entity instanceof ArmorStand) {
                continue;
            }

            if (data.hasTrackedTarget(entity)) {
                continue;
            }

            double distance = entity.getLocation().distance(projectileLoc);
            if (distance > range) continue;

            Vector toEntity = entity.getLocation().toVector()
                    .subtract(projectileLoc.toVector())
                    .normalize();
            double dot = projectileDir.dot(toEntity);
            double entityAngle = Math.acos(dot);

            if (entityAngle > angle) continue;

            if (homingConfig.isNoBlockBetween()) {
                Location entityCenter = entity.getLocation().clone().add(0, entity.getEyeHeight() / 2, 0);
                if (!RaycastUtil.hasLineOfSightSimple(
                        shooterLocation, entityCenter)) {
                    continue;
                }
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = entity;
            }
        }

        if (nearestTarget == null) {
            nearestDistance = Double.MAX_VALUE;

            for (LivingEntity entity : projectile.getWorld().getLivingEntities()) {
                if (entity.getEntityId() == projectile.getEntityId() ||
                        entity.isDead() || entity instanceof ArmorStand ||
                        (entity instanceof Player && ((Player) entity).getUniqueId().equals(data.getShooterId()))) {
                    continue;
                }

                double distance = entity.getLocation().distance(projectileLoc);
                if (distance > range) continue;

                Vector toEntity = entity.getLocation().toVector()
                        .subtract(projectileLoc.toVector())
                        .normalize();
                double dot = projectileDir.dot(toEntity);
                double entityAngle = Math.acos(dot);

                if (entityAngle > angle) continue;

                if (homingConfig.isNoBlockBetween()) {
                    if (!RaycastUtil.hasLineOfSightSimple(
                            shooterLocation, entity.getLocation())) {
                        continue;
                    }
                }

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestTarget = entity;
                }
            }
        }

        return nearestTarget;
    }

    private void applyHomingSteering(Projectile projectile, ProjectileData data, double turnSpeed) {
        if (projectile == null || !projectile.isValid()) {
            return;
        }
        int entityId = projectile.getEntityId();
        if (currentlySteering.contains(entityId)) {
            return;
        }

        currentlySteering.add(entityId);
        try {
            LivingEntity target = data.getCurrentHomingTarget();
            if (target == null || target.isDead() || !target.isValid()) {
                data.setCurrentHomingTarget(null);
                return;
            }

            double baseSpeed = data.getBaseSpeed();
            if (baseSpeed <= 0 || Double.isNaN(baseSpeed)) {
                Vector currentVelocity = projectile.getVelocity();
                baseSpeed = currentVelocity.length();
                data.setBaseSpeed(baseSpeed);

                if (baseSpeed <= 0.001) {
                    return;
                }
            }

            Vector currentVelocity = projectile.getVelocity();
            double currentActualSpeed = currentVelocity.length();

            if (currentActualSpeed <= 0.001) {
                return;
            }

            double aimHeight = AimHeightCalculator.calculateAimHeight(target);

            Vector toTarget = target.getLocation().toVector()
                    .add(new Vector(0, aimHeight, 0))
                    .subtract(projectile.getLocation().toVector())
                    .normalize();

            Vector currentDir = currentVelocity.clone().normalize();

            if (Math.abs(currentActualSpeed - baseSpeed) / baseSpeed > 0.3) {
                baseSpeed = currentActualSpeed;
                data.setBaseSpeed(baseSpeed);
            }

            Vector newDir = currentDir.clone().multiply(1.0 - turnSpeed)
                    .add(toTarget.clone().multiply(turnSpeed));

            if (newDir.lengthSquared() < 0.0001) {
                return;
            }

            newDir.normalize();

            if (newDir == null || Double.isNaN(baseSpeed) || baseSpeed <= 0) {
                return;
            }
            Vector newVelocity = newDir.multiply(baseSpeed);

            if (newVelocity.lengthSquared() < 0.0001 ||
                    Double.isNaN(newVelocity.getX()) ||
                    Double.isNaN(newVelocity.getY()) ||
                    Double.isNaN(newVelocity.getZ())) {
                return;
            }

            if (!projectile.isValid() || projectile.isDead()) {
                return;
            }
            projectile.setVelocity(newVelocity);
        } finally {
            currentlySteering.remove(entityId);
        }
    }

    private void updateOptimizedReturnProjectiles() {
        List<Integer> toRemove = new ArrayList<>();

        for (int entityId : returnProjectiles) {
            ProjectileData data = allProjectiles.get(entityId);
            if (data == null) {
                toRemove.add(entityId);
                continue;
            }

            Projectile projectile = getProjectile(entityId);
            if (projectile == null || !projectile.isValid()) {
                toRemove.add(entityId);
                continue;
            }

            Player shooter = Bukkit.getPlayer(data.getShooterId());
            if (shooter != null && shooter.isOnline()) {
                data.updateReturnAcceleration(projectile, shooter);
            }

            data.applyReturnAcceleration(projectile);
        }

        for (int entityId : toRemove) {
            removeProjectile(entityId);
        }
    }

    private Projectile getProjectile(int entityId) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.Entity entity : world.getEntities()) {
                if (entity.getEntityId() == entityId && entity instanceof Projectile) {
                    return (Projectile) entity;
                }
            }
        }
        return null;
    }

    public void cleanup() {
        if (homingTask != null) homingTask.cancel();
        if (returnTask != null) returnTask.cancel();

        allProjectiles.clear();
        homingProjectiles.clear();
        returnProjectiles.clear();
        normalProjectiles.clear();
        playerProjectiles.clear();
        pendingRemoval.clear();

        plugin.getLogger().info("ProjectileTracker已清理");
    }

    public void reload() {
        cleanup();
        startOptimizedTasks();
        startOptimizedCleanupTask();
    }

    public Map<Integer, ProjectileData> getActiveProjectiles() {
        return Collections.unmodifiableMap(allProjectiles);
    }

    private void startOptimizedCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (allProjectiles.isEmpty()) {
                return;
            }

            int cleaned = 0;
            int maxClean = 100;
            List<Integer> toRemove = new ArrayList<>();

            for (Map.Entry<Integer, ProjectileData> entry : allProjectiles.entrySet()) {
                if (cleaned >= maxClean) break;

                int entityId = entry.getKey();
                Projectile projectile = projectileCache.get(entityId);

                if (projectile == null || !projectile.isValid() || projectile.isDead()) {
                    toRemove.add(entityId);
                    cleaned++;
                }
            }

            for (int entityId : toRemove) {
                removeProjectile(entityId);
            }

            if (cleaned > 0 && plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "清理完成: %d 个抛射物, 剩余总数: %d",
                        cleaned, allProjectiles.size()
                ));
            }
        }, 40L, 40L);
    }

    public void markForRemoval(int entityId) {
        pendingRemoval.add(entityId);
    }

    public boolean isPendingRemoval(int entityId) {
        return pendingRemoval.contains(entityId);
    }

    public void completeRemoval(int entityId) {
        pendingRemoval.remove(entityId);
        removeProjectile(entityId);
    }

    public void setProjectileData(Projectile projectile, ProjectileData data) {
        int entityId = projectile.getEntityId();
        allProjectiles.put(entityId, data);
    }

    public ProjectileTracker getProjectileTracker() {
        return this;
    }
}