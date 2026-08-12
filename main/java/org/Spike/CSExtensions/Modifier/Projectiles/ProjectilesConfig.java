package org.Spike.CSExtensions.Modifier.Projectiles;

import org.bukkit.entity.LivingEntity;


public class ProjectilesConfig {

    private boolean hidden;
    private KnockbackType noknock;
    private PenetrateConfig penetrate;
    private ReturnConfig returnConfig;
    private BounceConfig bounce;
    private HomingConfig homing;

    public enum KnockbackType {
        NONE("none"),
        PLAYERS("players"),
        MOBS("mobs"),
        ALL("all");

        private final String configName;

        KnockbackType(String configName) {
            this.configName = configName;
        }

        public String getConfigName() {
            return configName;
        }

        public static KnockbackType fromString(String name) {
            if (name == null) return NONE;
            for (KnockbackType type : values()) {
                if (type.configName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return NONE;
        }
    }


    public enum HomingUpdate {
        ALWAYS("always"),
        PENETRATED("penetrated"),
        BOUNCED("bounced"),
        KILLED("killed"),
        NEVER("never");

        private final String configName;

        HomingUpdate(String configName) {
            this.configName = configName;
        }

        public String getConfigName() {
            return configName;
        }

        public static HomingUpdate fromString(String name) {
            if (name == null) return NEVER;
            for (HomingUpdate type : values()) {
                if (type.configName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return NEVER;
        }
    }


    public static class PenetrateConfig {
        private boolean enabled;
        private int number;
        private double velocityCoef;
        private double damageCoef;

        public PenetrateConfig() {
            this.enabled = false;
            this.number = 0;
            this.velocityCoef = 1.0;
            this.damageCoef = 1.0;
        }


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }

        public double getVelocityCoef() { return velocityCoef; }
        public void setVelocityCoef(double velocityCoef) { this.velocityCoef = velocityCoef; }

        public double getDamageCoef() { return damageCoef; }
        public void setDamageCoef(double damageCoef) { this.damageCoef = damageCoef; }
    }


    public static class ReturnConfig {
        private boolean enabled;
        private double acceleration;
        private boolean trackShooter;
        private boolean triggerOnBlock;

        public ReturnConfig() {
            this.enabled = false;
            this.acceleration = 0.0;
            this.trackShooter = false;
            this.triggerOnBlock = false;
        }


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getAcceleration() { return acceleration; }
        public void setAcceleration(double acceleration) { this.acceleration = acceleration; }

        public boolean isTrackShooter() { return trackShooter; }
        public void setTrackShooter(boolean trackShooter) { this.trackShooter = trackShooter; }

        public boolean isTriggerOnBlock() { return triggerOnBlock; }
        public void setTriggerOnBlock(boolean triggerOnBlock) { this.triggerOnBlock = triggerOnBlock; }
    }


    public static class BounceConfig {
        private boolean enabled;
        private int maxBounces;
        private double velocityCoef;
        private double damageCoef;
        private double randomAngle;
        private boolean autoAimNearest;
        private double autoAimRadius;

        public BounceConfig() {
            this.enabled = false;
            this.maxBounces = 0;
            this.velocityCoef = 1.0;
            this.damageCoef = 1.0;
            this.randomAngle = 0.0;
            this.autoAimNearest = false;
            this.autoAimRadius = 10.0;
        }


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getMaxBounces() { return maxBounces; }
        public void setMaxBounces(int maxBounces) { this.maxBounces = maxBounces; }

        public double getVelocityCoef() { return velocityCoef; }
        public void setVelocityCoef(double velocityCoef) { this.velocityCoef = velocityCoef; }

        public double getDamageCoef() { return damageCoef; }
        public void setDamageCoef(double damageCoef) { this.damageCoef = damageCoef; }

        public double getRandomAngle() { return randomAngle; }
        public void setRandomAngle(double randomAngle) { this.randomAngle = randomAngle; }

        public boolean isAutoAimNearest() { return autoAimNearest; }
        public void setAutoAimNearest(boolean autoAimNearest) { this.autoAimNearest = autoAimNearest; }

        public double getAutoAimRadius() { return autoAimRadius; }
        public void setAutoAimRadius(double autoAimRadius) { this.autoAimRadius = autoAimRadius; }
    }

    public static class HomingConfig {
        private boolean enabled;
        private double range;
        private double angle;
        private double turnSpeed;
        private HomingUpdate update;
        private boolean noBlockBetween;
        private boolean initialLock;

        public HomingConfig() {
            this.enabled = false;
            this.range = 20.0;
            this.angle = 45.0;
            this.turnSpeed = 0.2;
            this.update = HomingUpdate.NEVER;
            this.noBlockBetween = false;
            this.initialLock = true;
        }


        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getRange() { return range; }
        public void setRange(double range) { this.range = range; }

        public double getAngle() { return angle; }
        public void setAngle(double angle) { this.angle = angle; }

        public double getTurnSpeed() { return turnSpeed; }
        public void setTurnSpeed(double turnSpeed) { this.turnSpeed = turnSpeed; }

        public HomingUpdate getUpdate() { return update; }
        public void setUpdate(HomingUpdate update) { this.update = update; }

        public boolean isNoBlockBetween() { return noBlockBetween; }
        public void setNoBlockBetween(boolean noBlockBetween) { this.noBlockBetween = noBlockBetween; }

        public boolean isInitialLock() { return initialLock; }
        public void setInitialLock(boolean initialLock) { this.initialLock = initialLock; }

    }


    public ProjectilesConfig() {
        this.hidden = false;
        this.noknock = KnockbackType.NONE;
        this.penetrate = new PenetrateConfig();
        this.returnConfig = new ReturnConfig();
        this.bounce = new BounceConfig();
        this.homing = new HomingConfig();
    }


    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public KnockbackType getNoknock() { return noknock; }
    public void setNoknock(KnockbackType noknock) { this.noknock = noknock; }

    public PenetrateConfig getPenetrate() { return penetrate; }
    public void setPenetrate(PenetrateConfig penetrate) { this.penetrate = penetrate; }

    public ReturnConfig getReturnConfig() { return returnConfig; }
    public void setReturnConfig(ReturnConfig returnConfig) { this.returnConfig = returnConfig; }

    public BounceConfig getBounce() { return bounce; }
    public void setBounce(BounceConfig bounce) { this.bounce = bounce; }

    public HomingConfig getHoming() { return homing; }
    public void setHoming(HomingConfig homing) { this.homing = homing; }



    public boolean shouldCancelKnockback(org.bukkit.entity.Entity entity) {
        if (noknock == KnockbackType.NONE) return false;
        if (noknock == KnockbackType.ALL) return true;

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            if (noknock == KnockbackType.PLAYERS) {
                return living instanceof org.bukkit.entity.Player;
            } else if (noknock == KnockbackType.MOBS) {
                return !(living instanceof org.bukkit.entity.Player);
            }
        }

        return false;
    }
}