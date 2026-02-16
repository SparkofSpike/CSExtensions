//unused
package org.Spike.CSExtensions.Modifier.Events;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ProjectileBounceEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Projectile projectile;
    private final Player shooter;
    private final String weaponTitle;

    public ProjectileBounceEvent(Projectile projectile, Player shooter, String weaponTitle) {
        this.projectile = projectile;
        this.shooter = shooter;
        this.weaponTitle = weaponTitle;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public Player getShooter() {
        return shooter;
    }

    public String getWeaponTitle() {
        return weaponTitle;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}