package org.Spike.CSExtensions.Modifier.Projectiles;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProjectileData {
    private final int entityId;
    private final UUID shooterId;
    private final String weaponTitle;
    private final ProjectilesConfig config;

    private int penetrateCount = 0;
    private int bounceCount = 0;
    private int totalHits = 0;
    private double currentDamageMultiplier = 1.0;
    private double currentVelocityMultiplier = 1.0;
    private boolean isReturning = false;
    private boolean isExpired = false;
    private LivingEntity currentHomingTarget = null;
    private long lastHomingUpdate = 0;
    private boolean markedForRemoval = false;
    private boolean hitEntity = false;

    private Location shooterLocation;
    private Vector returnAcceleration;
    private long returnStartTime = 0;
    private boolean shouldDelayHit = false;

    private Vector lastVelocity;

    private boolean justPenetrated = false;
    private boolean justBounced = false;
    private double baseSpeed = 0;
    private boolean homingActivated = false;

    private LivingEntity initialHomingTarget;
    private boolean homingTargetSearched = false;

    private Set<Integer> trackedTargets = new HashSet<>();

    public ProjectileData(Projectile projectile, Player shooter, String weaponTitle, ProjectilesConfig config) {
        this(projectile, shooter, weaponTitle, config, 0, 0);
    }

    public ProjectileData(Projectile projectile, Player shooter, String weaponTitle,
                          ProjectilesConfig config, int bounceCount, int penetrateCount) {
        this(projectile, shooter, weaponTitle, config, bounceCount, penetrateCount, false, false);
    }


    public ProjectileData(Projectile projectile, Player shooter, String weaponTitle,
                          ProjectilesConfig config, int bounceCount, int penetrateCount,
                          boolean inheritHomingActivated, boolean wasHomingActivated) {
        this.entityId = projectile.getEntityId();
        this.shooterId = shooter.getUniqueId();
        this.weaponTitle = weaponTitle;
        this.config = config;
        this.shooterLocation = shooter.getLocation().clone();
        this.lastVelocity = projectile.getVelocity().clone();
        if (config.getBounce().isEnabled()) {
            this.bounceCount = bounceCount;
        }
        if (config.getPenetrate().isEnabled()) {
            this.penetrateCount = penetrateCount;
        }

        if (config.getReturnConfig().isEnabled()) {
            initReturnAcceleration(projectile, shooter);
        }

        this.justPenetrated = false;
        this.justBounced = false;

        this.baseSpeed = projectile.getVelocity().length();

        this.trackedTargets = new HashSet<>();

        if (inheritHomingActivated) {
            this.homingActivated = wasHomingActivated;
        } else {
            this.homingActivated = config.getHoming().isInitialLock();
        }
    }

    private void initReturnAcceleration(Projectile projectile, Player shooter) {
        ProjectilesConfig.ReturnConfig returnConfig = config.getReturnConfig();

        Vector toShooter = shooter.getLocation().toVector()
                .subtract(projectile.getLocation().toVector())
                .normalize();

        this.returnAcceleration = toShooter.multiply(returnConfig.getAcceleration());
    }

    public void updateReturnAcceleration(Projectile projectile, Player shooter) {
        if (!config.getReturnConfig().isTrackShooter() || !config.getReturnConfig().isEnabled()) {
            return;
        }

        Vector toShooter = shooter.getLocation().toVector()
                .subtract(projectile.getLocation().toVector())
                .normalize();

        this.returnAcceleration = toShooter.multiply(config.getReturnConfig().getAcceleration());
        this.shooterLocation = shooter.getLocation().clone();
    }

    public void applyReturnAcceleration(Projectile projectile) {
        if (!config.getReturnConfig().isEnabled() || isExpired) {
            return;
        }

        Vector currentVelocity = projectile.getVelocity();
        Vector newVelocity = currentVelocity.clone().add(returnAcceleration);
        projectile.setVelocity(newVelocity);

        if (returnStartTime == 0 && newVelocity.length() < currentVelocity.length()) {
            returnStartTime = System.currentTimeMillis();
            isReturning = true;
        }
    }

    public boolean canPenetrate() {
        if (!config.getPenetrate().isEnabled()) {
            return false;
        }
        return penetrateCount < config.getPenetrate().getNumber();
    }

    public void applyPenetrate() {
        if (!canPenetrate()) {
            return;
        }

        penetrateCount++;

        ProjectilesConfig.PenetrateConfig penetrateConfig = config.getPenetrate();
        currentDamageMultiplier *= penetrateConfig.getDamageCoef();

        currentVelocityMultiplier *= penetrateConfig.getVelocityCoef();

        justPenetrated = true;

        if (currentHomingTarget != null) {
            addTrackedTarget(currentHomingTarget);
            currentHomingTarget = null;
        }

        baseSpeed *= penetrateConfig.getVelocityCoef();

        checkHomingUpdate(ProjectilesConfig.HomingUpdate.PENETRATED);
    }

    public boolean canBounce() {
        if (!config.getBounce().isEnabled()) {
            return false;
        }

        if (hitEntity) {
            return false;
        }

        boolean result = bounceCount < config.getBounce().getMaxBounces();

        return result;
    }

    public void applyBounce(Vector newVelocity, ProjectilesConfig config) {
        if (!canBounce()) {
            return;
        }

        bounceCount++;
        double velocityCoef = config.getBounce().getVelocityCoef();
        currentVelocityMultiplier *= velocityCoef;
        currentDamageMultiplier *= config.getBounce().getDamageCoef();

        lastVelocity = newVelocity.clone();

        justBounced = true;

        if (!homingActivated && config.getHoming().isEnabled() &&
                config.getHoming().getUpdate() == ProjectilesConfig.HomingUpdate.BOUNCED) {
            homingActivated = true;
        }

        baseSpeed *= velocityCoef;

        checkHomingUpdate(ProjectilesConfig.HomingUpdate.BOUNCED);
    }

    public void checkHomingUpdate(ProjectilesConfig.HomingUpdate trigger) {
        if (!config.getHoming().isEnabled()) {
            return;
        }

        if (config.getHoming().getUpdate() == trigger ||
                config.getHoming().getUpdate() == ProjectilesConfig.HomingUpdate.ALWAYS) {

            currentHomingTarget = null;
        }
    }


    public void onTargetKilled() {
        if (currentHomingTarget != null && currentHomingTarget.isDead()) {
            currentHomingTarget = null;
        }
        checkHomingUpdate(ProjectilesConfig.HomingUpdate.KILLED);
    }

    public double getProjectileMultiplierForMain() {
        return currentDamageMultiplier;
    }

    public void completelyResetMultipliers() {
        currentDamageMultiplier = 1.0;
        currentVelocityMultiplier = 1.0;
    }

    public double getVelocityMultiplier() {
        return currentVelocityMultiplier;
    }

    public void applyVelocityMultiplier(Projectile projectile) {
        if (currentVelocityMultiplier != 1.0) {
            Vector currentVelocity = projectile.getVelocity();
            Vector newVelocity = currentVelocity.multiply(currentVelocityMultiplier);
            projectile.setVelocity(newVelocity);
            currentVelocityMultiplier = 1.0;
        }
    }

    public void markExpired() {
        this.isExpired = true;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getShooterId() {
        return shooterId;
    }

    public String getWeaponTitle() {
        return weaponTitle;
    }

    public ProjectilesConfig getConfig() {
        return config;
    }

    public int getPenetrateCount() {
        return penetrateCount;
    }

    public int getBounceCount() {
        return bounceCount;
    }

    public int getTotalHits() {
        return totalHits;
    }

    public boolean isReturning() {
        return isReturning;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public LivingEntity getCurrentHomingTarget() {
        return currentHomingTarget;
    }

    public void setCurrentHomingTarget(LivingEntity target) {
        this.currentHomingTarget = target;
    }

    public long getLastHomingUpdate() {
        return lastHomingUpdate;
    }

    public void setLastHomingUpdate(long time) {
        this.lastHomingUpdate = time;
    }

    public Vector getLastVelocity() {
        return lastVelocity;
    }

    public void setLastVelocity(Vector velocity) {
        this.lastVelocity = velocity;
    }

    public Location getShooterLocation() {
        return shooterLocation;
    }

    public void incrementHitCount() {
        totalHits++;
    }

    public void setPenetrateCount(int count) {
        this.penetrateCount = count;
    }

    public void setBounceCount(int count) {
        this.bounceCount = count;
    }

    public void setReturning(boolean returning) {
        this.isReturning = returning;
    }

    public void setExpired(boolean expired) {
        this.isExpired = expired;
    }

    public void markReturning() {
        this.isReturning = true;
        this.returnStartTime = System.currentTimeMillis();
    }

    public double getCurrentDamageMultiplier() {
        return currentDamageMultiplier;
    }

    public void setCurrentDamageMultiplier(double multiplier) {
        this.currentDamageMultiplier = multiplier;
    }

    public double getCurrentVelocityMultiplier() {
        return currentVelocityMultiplier;
    }

    public void setCurrentVelocityMultiplier(double multiplier) {
        this.currentVelocityMultiplier = multiplier;
    }

    public void markForRemoval() {
        this.markedForRemoval = true;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public void markHitEntity() {
        this.hitEntity = true;
    }

    public void clearHitEntity() {
        this.hitEntity = false;
    }

    public boolean hasHitEntity() {
        return hitEntity;
    }

    public void completeBounces() {
        this.bounceCount = 999;
    }

    public void setShouldDelayHit(boolean delay) {
        this.shouldDelayHit = delay;
    }

    public boolean shouldDelayHit() {
        return shouldDelayHit;
    }

    public boolean hasJustPenetrated() {
        return justPenetrated;
    }

    public boolean hasJustBounced() {
        return justBounced;
    }

    public void resetEventFlags() {
        justPenetrated = false;
        justBounced = false;
    }

    public void addTrackedTarget(LivingEntity target) {
        if (target != null) {
            trackedTargets.add(target.getEntityId());
        }
    }

    public boolean hasTrackedTarget(LivingEntity target) {
        return target != null && trackedTargets.contains(target.getEntityId());
    }

    public Set<Integer> getTrackedTargets() {
        return trackedTargets;
    }

    public void clearTrackedTargets() {
        trackedTargets.clear();
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public void setBaseSpeed(double speed) {
        this.baseSpeed = speed;
    }

    public boolean isHomingActivated() {
        return homingActivated;
    }

    public void setHomingActivated(boolean activated) {
        this.homingActivated = activated;
    }

    public void setJustBounced(boolean justBounced) {
        this.justBounced = justBounced;
    }

    public void setJustPenetrated(boolean justPenetrated) {
        this.justPenetrated = justPenetrated;
    }

    public LivingEntity getInitialHomingTarget() {
        return initialHomingTarget;
    }

    public void setInitialHomingTarget(LivingEntity target) {
        this.initialHomingTarget = target;
    }

    public boolean isHomingTargetSearched() {
        return homingTargetSearched;
    }

    public void setHomingTargetSearched(boolean searched) {
        this.homingTargetSearched = searched;
    }
}
