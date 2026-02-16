package org.Spike.CSExtensions.Modifier.Services;

import org.bukkit.Location;

public class HitLocationTarget {
    private final Location location;

    public HitLocationTarget(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("HitLocationTarget(%.1f,%.1f,%.1f)",
                location.getX(), location.getY(), location.getZ());
    }
}