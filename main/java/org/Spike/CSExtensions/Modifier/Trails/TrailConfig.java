package org.Spike.CSExtensions.Modifier.Trails;

import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

public class TrailConfig {
    private String weaponId;
    private String templateId;
    private TrailWeaponType weaponType;
    private List<String> effects;
    private int length;
    private Color particleColor;
    private double speed;
    private int amount;
    private int extraParticlesAhead;
    private double extraParticlesInterval;
    private TrailType trailType;
    private double radius;
    private int points;
    private GoThrough goThrough;

    public TrailConfig(String weaponId) {
        this.weaponId = weaponId;
        this.weaponType = TrailWeaponType.PROJECTILES;
        this.effects = new ArrayList<>();
        this.length = 40;
        this.particleColor = null;
        this.speed = 0.01;
        this.amount = 1;
        this.extraParticlesAhead = 0;
        this.extraParticlesInterval = 0.5;
        this.trailType = TrailType.STRAIGHT;
        this.radius = 0.5;
        this.points = 8;
        this.goThrough = GoThrough.NONE;
    }

    public String getWeaponId() {
        return weaponId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public TrailWeaponType getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(TrailWeaponType weaponType) {
        this.weaponType = weaponType;
    }

    public List<String> getEffects() {
        return effects;
    }

    public void setEffects(List<String> effects) {
        this.effects = effects;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Color getParticleColor() {
        return particleColor;
    }

    public void setParticleColor(Color color) {
        this.particleColor = color;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getExtraParticlesAhead() {
        return extraParticlesAhead;
    }

    public void setExtraParticlesAhead(int extraParticlesAhead) {
        this.extraParticlesAhead = extraParticlesAhead;
    }

    public double getExtraParticlesInterval() {
        return extraParticlesInterval;
    }

    public void setExtraParticlesInterval(int extraParticlesInterval) {
        this.extraParticlesInterval = extraParticlesInterval;
    }

    public TrailType getTrailType() {
        return trailType;
    }

    public void setTrailType(TrailType trailType) {
        this.trailType = trailType;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public GoThrough getGoThrough() {
        return goThrough;
    }

    public void setGoThrough(GoThrough goThrough) {
        this.goThrough = goThrough;
    }

    public int getTotalActiveParticles() {
        return extraParticlesAhead + 1;
    }

    public boolean isEnergyWeapon() {
        return weaponType == TrailWeaponType.ENERGY;
    }

    public boolean isProjectileWeapon() {
        return weaponType == TrailWeaponType.PROJECTILES;
    }
}