package org.Spike.CSExtensions.Modifier.Services;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class RaycastUtil {

    public static boolean hasLineOfSightSimple(Location from, Location to) {
        try {
            if (!from.getWorld().equals(to.getWorld())) {
                return false;
            }

            double distance = from.distance(to);
            if (distance < 0.5) {
                return true;
            }

            Vector direction = to.toVector().subtract(from.toVector()).normalize();

            int samples = Math.max(3, (int) (distance * 3));
            double step = distance / samples;

            Location current = from.clone().add(direction.clone().multiply(0.3));

            for (int i = 0; i < samples; i++) {
                Block block = current.getBlock();
                Material type = block.getType();

                if (type.isSolid() && !isTransparentSimple(type)) {
                    return false;
                }

                current.add(direction.clone().multiply(step));
            }

            return true;

        } catch (Exception e) {
            return true;
        }
    }

    public static boolean isTransparentSimple(Material material) {
        switch (material) {
            case AIR:
            case WATER:
            case STATIONARY_WATER:
            case LAVA:
            case STATIONARY_LAVA:

            case LONG_GRASS:
            case DEAD_BUSH:
            case YELLOW_FLOWER:
            case RED_ROSE:
            case DOUBLE_PLANT:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case SAPLING:
            case VINE:
            case WATER_LILY:
            case CROPS:
            case CARROT:
            case POTATO:
            case MELON_STEM:
            case PUMPKIN_STEM:

            case THIN_GLASS:
            case GLASS:
            case STAINED_GLASS:
            case STAINED_GLASS_PANE:
            case ICE:
            case LEAVES:
            case LEAVES_2:

            case WEB:
            case LADDER:
            case SNOW:
            case FIRE:
            case PORTAL:
            case ENDER_PORTAL:
            case ENDER_PORTAL_FRAME:
            case SIGN_POST:
            case WALL_SIGN:
            case WOOD_PLATE:
            case STONE_PLATE:
            case IRON_PLATE:
            case GOLD_PLATE:
            case REDSTONE_WIRE:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON:
            case TRAP_DOOR:
            case IRON_TRAPDOOR:
                return true;

            default:
                return false;
        }
    }
}