//damn it im a fool, wtf is this lamp of shit
package org.Spike.CSExtensions.Modifier.Services;

import org.bukkit.entity.*;

import java.util.Random;

public class AimHeightCalculator {
    private static final Random random = new Random();

    public static double calculateAimHeight(LivingEntity target) {
        double baseHeight = 0.0;

        target.getEyeHeight();

        if (target instanceof Player) {
            baseHeight = 1.5;
        } else if (target instanceof Zombie) {
            baseHeight = 1.7;
        } else if (target instanceof Skeleton) {
            baseHeight = 1.7;
        } else if (target instanceof Spider) {
            baseHeight = 0.7;
        } else if (target instanceof Creeper) {
            baseHeight = 1.5;
        } else if (target instanceof Enderman) {
            baseHeight = 2.5;
        } else if (target instanceof Witch) {
            baseHeight = 1.7;
        } else if (target instanceof Blaze) {
            baseHeight = 1.8;
        } else if (target instanceof Slime || target instanceof MagmaCube) {
            baseHeight = 0.6;
        } else {
            baseHeight = 0.8;
        }

        return target.getEyeHeight();
    }

    public static double getEstimatedHeight(LivingEntity entity) {
        if (entity instanceof Player) {
            return 1.8;
        } else if (entity instanceof Zombie) {
            return 1.8;
        } else if (entity instanceof Skeleton) {
            return 1.8;
        } else if (entity instanceof Creeper) {
            return 1.7;
        } else if (entity instanceof Spider) {
            return 0.9;
        } else if (entity instanceof Enderman) {
            return 2.9;
        } else if (entity instanceof Witch) {
            return 1.8;
        } else if (entity instanceof Blaze) {
            return 1.8;
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            return 0.6 * slime.getSize();
        } else if (entity instanceof MagmaCube) {
            MagmaCube magmaCube = (MagmaCube) entity;
            return 0.6 * magmaCube.getSize();
        }
        return 1.8;
    }
}