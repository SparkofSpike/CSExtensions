package org.Spike.CSExtensions.Modifier.Projectiles;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

public class ProjectileHider {
    private static String version;
    private static Class<?> packetDestroyClass;
    private static Constructor<?> destroyConstructor;

    static {
        try {

            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];


            packetDestroyClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");
            destroyConstructor = packetDestroyClass.getConstructor(int[].class);

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ProjectileHider] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void hideProjectile(org.bukkit.entity.Projectile projectile) {
        try {
            if (destroyConstructor == null) {
                return;
            }


            int[] entityIds = new int[]{projectile.getEntityId()};
            Object packet = destroyConstructor.newInstance(new Object[]{entityIds});


            sendPacketToNearbyPlayers(projectile.getLocation(), packet);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ProjectileHider] 隐藏抛射物失败: " + e.getMessage());
        }
    }

    private static void sendPacketToNearbyPlayers(Location location, Object packet) {
        try {

            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".Packet");

            Method getHandle = craftPlayerClass.getMethod("getHandle");
            java.lang.reflect.Field playerConnectionField = entityPlayerClass.getField("playerConnection");
            Method sendPacket = playerConnectionClass.getMethod("sendPacket", packetClass);

            Collection<Player> players = location.getWorld().getPlayers();
            for (Player player : players) {
                if (player.getLocation().distanceSquared(location) <= 16384) {
                    Object craftPlayer = craftPlayerClass.cast(player);
                    Object entityPlayer = getHandle.invoke(craftPlayer);
                    Object connection = playerConnectionField.get(entityPlayer);
                    sendPacket.invoke(connection, packet);
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ProjectileHider] 发送数据包失败: " + e.getMessage());
        }
    }


    public static boolean isInitialized() {
        return destroyConstructor != null;
    }
}