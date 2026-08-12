package org.Spike.CSExtensions.Modifier.Projectiles;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponHitBlockEvent;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Services.AimHeightCalculator;
import org.Spike.CSExtensions.Modifier.Services.ProjectileEffectCoordinator;
import org.Spike.CSExtensions.Modifier.Services.RaycastUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class ProjectileEventHandler implements Listener {
    private final CSExtensions plugin;
    private final ProjectileTracker tracker;
    private final Random random = new Random();
    private final CSUtility csUtility;
    private final ProjectilesManager projectilesManager;
    private final ProjectileEffectCoordinator projectileEffectCoordinator;

    private final Set<Integer> processingProjectiles = new HashSet<>();
    private final Set<Integer> currentlyProcessing = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> pendingDamageProjectiles = new HashSet<>();
    private final Set<Integer> currentlyPenetrating = new HashSet<>();

    public ProjectileEventHandler(CSExtensions plugin, ProjectileTracker tracker,
                                  ProjectilesManager projectilesManager,
                                  ProjectileEffectCoordinator projectileEffectCoordinator) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.csUtility = new CSUtility();
        this.projectilesManager = projectilesManager;
        this.projectileEffectCoordinator = projectileEffectCoordinator;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        System.out.println("[DEBUG] ProjectileEventHandler已注册事件监听器");
    }

    private static EntityType getEntityType(ProjectileData originalData) {
        EntityType projectileType = originalData.getProjectileType();

        if (projectileType == EntityType.EGG) {
            projectileType = EntityType.SNOWBALL;
        }
        return projectileType;
    }

    @EventHandler
    public void onWeaponHitBlock(WeaponHitBlockEvent event) {
        org.bukkit.entity.Entity projectileEntity = event.getProjectile();

        if (!(projectileEntity instanceof Projectile)) {
            return;
        }
        Block hitBlock = event.getBlock();

        Projectile projectile = (Projectile) event.getProjectile();
        Location projectileLoc = projectile.getLocation();
        int entityId = projectile.getEntityId();
        ProjectileData data = tracker.getProjectileData(entityId);

        for (org.bukkit.entity.Entity entity : projectile.getWorld().getEntities()) {
            if (entity instanceof LivingEntity &&
                    !entity.getUniqueId().equals(projectile.getUniqueId()) &&
                    entity.getLocation().distance(projectileLoc) < 1.2) {
                processingProjectiles.add(entityId);
                projectile.remove();
                return;
            }
        }


        if (pendingDamageProjectiles.contains(entityId)) {
            return;
        }

        if (hitBlock == null || hitBlock.getType() == Material.AIR) {
            return;
        }

        if (!isBounceableBlock(hitBlock)) {
            return;
        }

        if (processingProjectiles.contains(entityId)) {
            return;
        }

        processingProjectiles.add(entityId);

        try {
            if (data == null) {
                return;
            }

            data.setShouldDelayHit(false);

            if (data.getConfig().getBounce().isEnabled() && data.canBounce()) {
                handleFixedBounce(projectile, data);
            } else {
                tracker.removeProjectile(entityId);
            }

        } finally {
            processingProjectiles.remove(entityId);
        }
    }

    private boolean isBounceableBlock(Block block) {
        Material material = block.getType();

        if (material == Material.AIR ||
                material == Material.WATER ||
                material == Material.STATIONARY_WATER ||
                material == Material.LAVA ||
                material == Material.STATIONARY_LAVA ||
                material == Material.FIRE ||
                material == Material.SNOW) {
            return false;
        }

        return material.isSolid();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) {
            return;
        }

        Projectile projectile = (Projectile) event.getDamager();
        int entityId = projectile.getEntityId();


        if (!currentlyProcessing.add(entityId)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("[事件防重入] 已在处理中: ID=" + entityId);
            }
            return;
        }

        try {
            ProjectileData data = tracker.getProjectileData(entityId);
            if (data == null) {
                return;
            }

            if (data.hasHitEntity()) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("[命中检查] 抛射物已命中过实体: ID=" + entityId);
                }
                return;
            }

            Entity victim = event.getEntity();

            if (data.getTrackedTargets().contains(victim.getEntityId())) {
                event.setCancelled(true);
                projectile.remove();
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("穿透链抛射物命中已穿透实体，取消伤害并移除: " + victim.getType().name());
                }
                return;
            }

            Player shooter = Bukkit.getPlayer(data.getShooterId());
            data.markHitEntity();

            if (victim instanceof LivingEntity && ((LivingEntity) victim).getHealth() - event.getFinalDamage() <= 0) {
                if (data != null && data.getCurrentHomingTarget() != null &&
                        data.getCurrentHomingTarget().getEntityId() == victim.getEntityId()) {
                    data.onTargetKilled();
                }
            }

            if (shouldProjectileIgnoreEntity(projectile, victim)) {
                event.setCancelled(true);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("穿透抛射物忽略刚穿透的实体: " + victim.getType().name());
                }
                return;
            }

            if (shooter != null && isEntityImmune(victim, data.getWeaponTitle(), shooter.getUniqueId())) {
                event.setCancelled(true);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("实体对武器免疫，取消伤害: " + data.getWeaponTitle());
                }
                return;
            }

            handlePenetrate(projectile, data, victim);

        } finally {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                currentlyProcessing.remove(entityId);
            }, 10L);
        }
    }

    private void handlePenetrate(Projectile originalProjectile, ProjectileData data, Entity hitEntity) {
        int entityId = originalProjectile.getEntityId();
        String lockKey = "penetrate_" + entityId;
        if (originalProjectile.hasMetadata(lockKey)) {
            return;
        }

        originalProjectile.setMetadata(lockKey, new FixedMetadataValue(plugin, true));

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[调用追踪] handlePenetrate被调用，ID=" + entityId);
            plugin.getLogger().info("[调用追踪] 调用栈:");
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element.getClassName().contains("Spike")) {
                    plugin.getLogger().info("  " + element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber());
                }
            }
        }

        if (currentlyPenetrating.contains(entityId)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("[防重入] 阻止重复穿透处理: ID=" + entityId);
            }
            return;
        }

        currentlyPenetrating.add(entityId);

        try {

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[防重入] 开始穿透处理: ID=" + entityId);
            }

            if (!data.canPenetrate()) {
                tracker.markForRemoval(originalProjectile.getEntityId());
                return;
            }

            Player shooter = Bukkit.getPlayer(data.getShooterId());
            if (shooter == null || !shooter.isOnline()) {
                tracker.markForRemoval(originalProjectile.getEntityId());
                return;
            }

            String weaponTitle = data.getWeaponTitle();
            ProjectilesConfig config = data.getConfig();

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[穿透调试] 原弹射物位置=" + originalProjectile.getLocation() +
                        " velocity=" + originalProjectile.getVelocity() +
                        " lastVelocity=" + data.getLastVelocity() +
                        " 命中实体位置=" + hitEntity.getLocation() +
                        " 射手位置=" + shooter.getLocation());
            }

            data.applyPenetrate();

            if (hitEntity instanceof LivingEntity) {
                data.addTrackedTarget((LivingEntity) hitEntity);
            }

            Vector bulletVelocity = data.getLastVelocity();

            Location respawnLocation = calculatePenetrateRespawnLocation(originalProjectile, hitEntity, data);
            if (respawnLocation == null) {
                tracker.markForRemoval(originalProjectile.getEntityId());
                return;
            }

            double distanceToEntity = respawnLocation.distance(hitEntity.getLocation());
            double minDistance = getEstimatedEntityWidth(hitEntity) + 0.5;

            if (distanceToEntity < minDistance) {
                respawnLocation.add(bulletVelocity.normalize().multiply(minDistance - distanceToEntity + 0.3));
            }

            Vector originalVelocity = originalProjectile.getVelocity();
            if (originalVelocity.lengthSquared() < 0.001) {
                originalVelocity = bulletVelocity;
            }
            Vector newVelocity = originalVelocity.clone();

            double velocityMultiplier = data.getVelocityMultiplier();
            if (velocityMultiplier != 1.0) {
                newVelocity.multiply(velocityMultiplier);
                data.setCurrentVelocityMultiplier(1.0);
            }

            if (!shooter.isOnline()) return;

            if (!hitEntity.isValid() || hitEntity.isDead()) {
                return;
            }

            spawnPenetrateProjectile(respawnLocation, newVelocity, shooter, weaponTitle, data, config);

            tracker.markForRemoval(originalProjectile.getEntityId());
            originalProjectile.remove();

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "穿透成功: 武器=%s, 计数=%d/%d, 伤害系数=%.2f, 距离=%.1f",
                        weaponTitle, data.getPenetrateCount(), config.getPenetrate().getNumber(),
                        data.getCurrentDamageMultiplier(), distanceToEntity
                ));
            }
        } finally {
            currentlyPenetrating.remove(entityId);
        }
    }

    private boolean isEntityImmune(Entity entity, String weaponTitle, UUID shooterId) {
        String immunityKey = "CSE_Penetrate_Immune_" + weaponTitle + "_" + shooterId;
        return entity.hasMetadata(immunityKey);
    }

    private void markProjectileIgnoreEntity(Projectile projectile, Entity entityToIgnore) {
        projectile.setMetadata("CSE_Ignore_Entity",
                new org.bukkit.metadata.FixedMetadataValue(plugin, entityToIgnore.getEntityId()));
    }

    private boolean shouldProjectileIgnoreEntity(Projectile projectile, Entity entity) {
        if (!projectile.hasMetadata("CSE_Ignore_Entity")) {
            return false;
        }

        int ignoreEntityId = projectile.getMetadata("CSE_Ignore_Entity").get(0).asInt();
        return entity.getEntityId() == ignoreEntityId;
    }

    private Location calculatePenetrateRespawnLocation(Projectile projectile, Entity hitEntity, ProjectileData data) {
        try {
            Location projectileLoc = projectile.getLocation();
            Vector direction = projectile.getVelocity().normalize();
            if (direction.lengthSquared() < 0.001) {
                direction = data.getLastVelocity().normalize();
            }

            double entityWidth = getEstimatedEntityWidth(hitEntity);

            double offsetDistance = entityWidth + 3.5;

            Vector offset = direction.clone().multiply(offsetDistance);
            Location respawnLoc = projectileLoc.clone().add(offset);

            for (int i = 0; i < 3; i++) {
                if (!respawnLoc.getBlock().getType().isSolid()) {
                    break;
                }
                respawnLoc.add(0, 0.5, 0);
            }

            double yaw = Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
            double pitch = Math.toDegrees(Math.asin(direction.getY() / direction.length()));
            respawnLoc.setYaw((float) yaw);
            respawnLoc.setPitch((float) pitch);

            if (plugin.getConfig().getBoolean("debug", false)) {
                double distance = respawnLoc.distance(hitEntity.getLocation());
                plugin.getLogger().info(String.format(
                        "穿透重生位置: 实体=%s, 大小=%.1f, 偏移=%.1f, 实际距离=%.1f",
                        hitEntity.getType().name(), entityWidth, offsetDistance, distance
                ));
            }

            return respawnLoc;

        } catch (Exception e) {
            plugin.getLogger().warning("计算穿透重生位置失败: " + e.getMessage());
            return null;
        }
    }

    private double getEstimatedEntityWidth(Entity entity) {
        switch (entity.getType()) {
            case PLAYER:
                return 0.6;
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
                return 0.6;
            case SPIDER:
            case CAVE_SPIDER:
                return 1.4;
            case ENDERMAN:
                return 0.6;
            case WITCH:
                return 0.6;
            case BLAZE:
                return 0.6;
            case GHAST:
                return 4.0;
            case SLIME:
            case MAGMA_CUBE:
                if (entity instanceof org.bukkit.entity.Slime) {
                    org.bukkit.entity.Slime slime = (org.bukkit.entity.Slime) entity;
                    return 0.6 * slime.getSize();
                }
                return 0.6;
            default:
                return 0.6;
        }
    }

    private double getEstimatedEntityHeight(Entity entity) {
        switch (entity.getType()) {
            case PLAYER:
                return 1.8;
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
                return 1.8;
            case SPIDER:
            case CAVE_SPIDER:
                return 0.9;
            case ENDERMAN:
                return 2.9;
            case WITCH:
                return 1.8;
            case BLAZE:
                return 1.8;
            case GHAST:
                return 4.0;
            case SLIME:
            case MAGMA_CUBE:
                if (entity instanceof org.bukkit.entity.Slime) {
                    org.bukkit.entity.Slime slime = (org.bukkit.entity.Slime) entity;
                    return 0.6 * slime.getSize();
                }
                return 0.6;
            default:
                return 1.8;
        }
    }

    private void spawnPenetrateProjectile(Location location, Vector velocity, Player shooter,
                                          String weaponTitle, ProjectileData originalData,
                                          ProjectilesConfig config) {
        try {
            EntityType projectileType = getEntityType(originalData);

            Projectile newProjectile = (Projectile) location.getWorld().spawnEntity(
                    location, projectileType
            );
            newProjectile.setVelocity(velocity);
            newProjectile.setShooter(shooter);

            csUtility.setProjectile(shooter, newProjectile, weaponTitle);

            applyKnockbackImmunity(newProjectile, config);

            tracker.registerInheritedProjectile(newProjectile, shooter, weaponTitle, originalData);

            ProjectileData newData = tracker.getProjectileData(newProjectile.getEntityId());
            if (newData != null) {
                newData.clearHitEntity();
                newData.setCurrentDamageMultiplier(originalData.getCurrentDamageMultiplier());

                projectileEffectCoordinator.applyEffectsForPenetrateProjectile(newProjectile, shooter, weaponTitle, config);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "生成穿透抛射物: 类型=%s, 位置=%.1f,%.1f,%.1f, 速度=%.2f, 向量=%s",
                        projectileType.name(),
                        location.getX(), location.getY(), location.getZ(),
                        velocity.length(), velocity
                ));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("生成穿透抛射物失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFixedBounce(Projectile projectile, ProjectileData data) {
        Player shooter = Bukkit.getPlayer(data.getShooterId());
        if (shooter == null) {
            tracker.removeProjectile(projectile.getEntityId());
            return;
        }

        String weaponTitle = data.getWeaponTitle();

        ProjectilesConfig newConfig = projectilesManager.getProjectilesConfig(weaponTitle);
        if (newConfig == null) {
            tracker.removeProjectile(projectile.getEntityId());
            return;
        }

        Vector velocity = projectile.getVelocity();
        Location location = projectile.getLocation();

        BlockFace collisionFace = detectFinalCollisionFace(location, velocity);

        Vector bounceDirection = calculateBaseBounce(velocity, collisionFace, data);

        if (newConfig.getBounce().isAutoAimNearest()) {
            bounceDirection = applyAutoAimToTarget(bounceDirection, location, newConfig, shooter);
        }

        if (newConfig.getBounce().getRandomAngle() > 0) {
            bounceDirection = applyFinalRandomAngle(bounceDirection, getNormalFromBlockFace(collisionFace),
                    newConfig.getBounce().getRandomAngle());
        }

        double originalSpeed = velocity.length();
        double bounceSpeed = calculateFinalSpeed(originalSpeed, data);

        double velocityCoef = newConfig.getBounce().getVelocityCoef();
        bounceSpeed *= velocityCoef;

        if (bounceSpeed > 20) {
            bounceSpeed = 20;
        }

        Vector finalVelocity = bounceDirection.normalize().multiply(bounceSpeed);

        if (!data.canBounce()) {
            tracker.removeProjectile(projectile.getEntityId());
            return;
        }

        data.applyBounce(finalVelocity, newConfig);

        Location spawnLocation = calculateOptimizedSpawnLocation(location, collisionFace, finalVelocity);

        projectile.remove();

        spawnBounceProjectile(spawnLocation, finalVelocity, shooter, weaponTitle, data, newConfig, projectile);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format(
                    "弹跳成功: 武器=%s, 计数=%d/%d, 速度 %.2f→%.2f, 配置已重载",
                    weaponTitle, data.getBounceCount(), newConfig.getBounce().getMaxBounces(),
                    originalSpeed, bounceSpeed
            ));
        }
    }

    private void spawnBounceProjectile(Location location, Vector velocity, Player shooter,
                                       String weaponTitle, ProjectileData originalData,
                                       ProjectilesConfig config, Projectile originalProjectile) {
        try {
            EntityType bounceType = originalProjectile.getType();

            if (bounceType == EntityType.EGG) {
                bounceType = EntityType.SNOWBALL;
            }

            Projectile newProjectile = (Projectile) location.getWorld().spawnEntity(
                    location, bounceType
            );
            newProjectile.setVelocity(velocity);
            newProjectile.setShooter(shooter);

            csUtility.setProjectile(shooter, newProjectile, weaponTitle);

            if (config.isHidden()) {
                newProjectile.setCustomNameVisible(false);
            }

            applyKnockbackImmunity(newProjectile, config);

            tracker.registerInheritedProjectile(newProjectile, shooter, weaponTitle, originalData);

            ProjectileData newData = tracker.getProjectileData(newProjectile.getEntityId());
            if (newData != null) {
                newData.setCurrentDamageMultiplier(originalData.getCurrentDamageMultiplier());
                newData.setCurrentVelocityMultiplier(originalData.getCurrentVelocityMultiplier());
            }

            projectileEffectCoordinator.applyEffectsForBounceProjectile(newProjectile, shooter, weaponTitle, config);

        } catch (Exception e) {
            plugin.getLogger().warning("生成弹跳抛射物失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void applyProjectileEffects(Projectile projectile, ProjectileData data) {
        ProjectilesConfig config = data.getConfig();

        if (config.isHidden()) {
            projectile.setCustomNameVisible(false);
        }

        applyKnockbackImmunity(projectile, config);
    }

    private void applyKnockbackImmunity(Projectile projectile, ProjectilesConfig config) {
        if (config.getNoknock() != ProjectilesConfig.KnockbackType.NONE) {
            projectile.setMetadata("CSE_NoKnockback", new org.bukkit.metadata.FixedMetadataValue(plugin, config.getNoknock().name()));
        }
    }


    private Vector applyAutoAimToTarget(Vector currentDirection, Location projectileLoc,
                                        ProjectilesConfig config, Player shooter) {
        if (!config.getBounce().isAutoAimNearest()) {
            return currentDirection;
        }

        double searchRange = config.getBounce().getAutoAimRadius();

        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        org.bukkit.World world = projectileLoc.getWorld();
        if (world == null) return currentDirection;

        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity.isDead()) continue;
            if (entity.getEntityId() == shooter.getEntityId()) continue;
            if (entity instanceof ArmorStand) continue;

            if (!shouldTargetEntity(entity, config.getNoknock())) {
                continue;
            }

            double distance = entity.getLocation().distance(projectileLoc);
            if (distance > searchRange) continue;

            if (!RaycastUtil.hasLineOfSightSimple(projectileLoc, entity.getLocation())) {
                continue;
            }

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTarget = entity;
            }
        }

        if (nearestTarget != null) {
            Location targetLocation = nearestTarget.getLocation().clone();

            double aimHeight = AimHeightCalculator.calculateAimHeight(nearestTarget);
            targetLocation.add(0, aimHeight, 0);

            Vector toTarget = targetLocation.toVector()
                    .subtract(projectileLoc.toVector())
                    .normalize();

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "自动瞄准: 目标 %s, 距离 %.1f, 瞄准高度 +%.1f",
                        nearestTarget.getName(), nearestDistance, aimHeight
                ));
            }

            return toTarget;
        }

        return currentDirection;
    }


    private boolean shouldTargetEntity(Entity entity, ProjectilesConfig.KnockbackType noknock) {
        if (noknock == ProjectilesConfig.KnockbackType.NONE) {
            return true;
        }

        if (entity instanceof LivingEntity) {
            boolean isPlayer = entity instanceof Player;

            switch (noknock) {
                case PLAYERS:
                    return isPlayer;
                case MOBS:
                    return !isPlayer;
                case ALL:
                    return true;
                default:
                    return true;
            }
        }
        return false;
    }

    private Location calculateOptimizedSpawnLocation(Location hitLocation, BlockFace collisionFace,
                                                     Vector bounceVelocity) {
        Location location = hitLocation.clone();

        switch (collisionFace) {
            case UP:
                location.add(0, 0.1, 0);
                break;
            case DOWN:
                location.add(0, -0.1, 0);
                break;
            case NORTH:
                location.add(0, 0, 0.1);
                break;
            case SOUTH:
                location.add(0, 0, -0.1);
                break;
            case EAST:
                location.add(-0.1, 0, 0);
                break;
            case WEST:
                location.add(0.1, 0, 0);
                break;
        }

        if (!location.getBlock().isEmpty()) {
            Vector normal = getNormalFromBlockFace(collisionFace);
            for (int i = 1; i <= 5; i++) {
                Location testLoc = hitLocation.clone().add(normal.clone().multiply(i * 0.2));
                if (testLoc.getBlock().isEmpty()) {
                    location = testLoc;
                    break;
                }
            }
        }

        Vector direction = bounceVelocity.clone().normalize();
        location.add(direction.multiply(0.15));
        return location;
    }


    private Vector calculateBaseBounce(Vector velocity, BlockFace collisionFace, ProjectileData data) {
        Vector v = velocity.clone().normalize();
        Vector normal = getNormalFromBlockFace(collisionFace);

        double dot = v.dot(normal);
        Vector reflect = v.clone().subtract(normal.clone().multiply(2 * dot));

        switch (collisionFace) {
            case UP:
                if (reflect.getY() < 0.2) {
                    reflect = new Vector(reflect.getX() * 0.5, 0.3, reflect.getZ() * 0.5);
                } else {
                    reflect = new Vector(reflect.getX() * 0.7, reflect.getY(), reflect.getZ() * 0.7);
                }
                break;
            case DOWN:
                if (reflect.getY() > -0.2) {
                    reflect = new Vector(reflect.getX() * 0.5, -0.3, reflect.getZ() * 0.5);
                } else {
                    reflect = new Vector(reflect.getX() * 0.7, reflect.getY(), reflect.getZ() * 0.7);
                }
                break;
            default:
                reflect = new Vector(reflect.getX(), reflect.getY() * 0.3, reflect.getZ());
                break;
        }
        return reflect.normalize();
    }


    private double calculateFinalSpeed(double originalSpeed, ProjectileData data) {
        double configCoef = data.getConfig().getBounce().getVelocityCoef();
        double totalCoef = configCoef * 0.8;
        return originalSpeed * totalCoef;
    }


    private BlockFace detectFinalCollisionFace(Location location, Vector velocity) {
        Block centerBlock = location.getBlock();
        Vector v = velocity.clone().normalize();

        double posX = location.getX();
        double posY = location.getY();
        double posZ = location.getZ();

        double distEast = Math.ceil(posX) - posX;
        double distWest = posX - Math.floor(posX);
        double distUp = Math.ceil(posY) - posY;
        double distDown = posY - Math.floor(posY);
        double distSouth = Math.ceil(posZ) - posZ;
        double distNorth = posZ - Math.floor(posZ);

        double minDist = Double.MAX_VALUE;
        BlockFace closestFace = BlockFace.UP;

        if (distDown < minDist && v.getY() <= 0) {
            Block downBlock = centerBlock.getRelative(BlockFace.DOWN);
            if (!downBlock.isEmpty() && downBlock.getType().isSolid()) {
                minDist = distDown;
                closestFace = BlockFace.UP;
            }
        }

        if (distUp < minDist && v.getY() >= 0) {
            Block upBlock = centerBlock.getRelative(BlockFace.UP);
            if (!upBlock.isEmpty() && upBlock.getType().isSolid()) {
                minDist = distUp;
                closestFace = BlockFace.DOWN;
            }
        }

        if (distWest < minDist && v.getX() <= 0) {
            Block westBlock = centerBlock.getRelative(BlockFace.WEST);
            if (!westBlock.isEmpty() && westBlock.getType().isSolid()) {
                minDist = distWest;
                closestFace = BlockFace.EAST;
            }
        }

        if (distEast < minDist && v.getX() >= 0) {
            Block eastBlock = centerBlock.getRelative(BlockFace.EAST);
            if (!eastBlock.isEmpty() && eastBlock.getType().isSolid()) {
                minDist = distEast;
                closestFace = BlockFace.WEST;
            }
        }

        if (distNorth < minDist && v.getZ() <= 0) {
            Block northBlock = centerBlock.getRelative(BlockFace.NORTH);
            if (!northBlock.isEmpty() && northBlock.getType().isSolid()) {
                minDist = distNorth;
                closestFace = BlockFace.SOUTH;
            }
        }

        if (distSouth < minDist && v.getZ() >= 0) {
            Block southBlock = centerBlock.getRelative(BlockFace.SOUTH);
            if (!southBlock.isEmpty() && southBlock.getType().isSolid()) {
                minDist = distSouth;
                closestFace = BlockFace.NORTH;
            }
        }

        return closestFace;
    }


    private Vector applyFinalRandomAngle(Vector direction, Vector normal, double maxAngleDegrees) {
        if (maxAngleDegrees <= 0) return direction.clone();

        double angle = Math.toRadians(random.nextDouble() * maxAngleDegrees * 2 - maxAngleDegrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = direction.getX() * cos - direction.getZ() * sin;
        double z = direction.getX() * sin + direction.getZ() * cos;

        return new Vector(x, direction.getY(), z).normalize();
    }


    private Vector getNormalFromBlockFace(BlockFace face) {
        switch (face) {
            case UP:
                return new Vector(0, 1, 0);
            case DOWN:
                return new Vector(0, -1, 0);
            case NORTH:
                return new Vector(0, 0, -1);
            case SOUTH:
                return new Vector(0, 0, 1);
            case EAST:
                return new Vector(1, 0, 0);
            case WEST:
                return new Vector(-1, 0, 0);
            default:
                return new Vector(0, 1, 0);
        }
    }


    public void handleEntityDamage(EntityDamageByEntityEvent event, Projectile projectile) {
        ProjectileData data = tracker.getProjectileData(projectile.getEntityId());
        if (data == null) return;

        applyKnockbackToTarget(event, data.getConfig());
        data.markHitEntity();
    }


    private void applyKnockbackToTarget(EntityDamageByEntityEvent event, ProjectilesConfig config) {
        if (config.shouldCancelKnockback(event.getEntity())) {
            event.getEntity().setMetadata("CSE_NoKnockback",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, config.getNoknock().name()));
        }
    }


    public void cleanup() {
        processingProjectiles.clear();
    }
}
