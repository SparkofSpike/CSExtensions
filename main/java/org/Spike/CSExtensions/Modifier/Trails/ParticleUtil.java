package org.Spike.CSExtensions.Modifier.Trails;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.Color;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

public class ParticleUtil {

    private static String version;
    private static Constructor<?> packetConstructor;
    private static Class<?> enumParticleClass;
    private static Object[] enumParticleValues;

    static {
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Bukkit.getLogger().info("[ParticleUtil] 检测到版本: " + version);

            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutWorldParticles");
            enumParticleClass = Class.forName("net.minecraft.server." + version + ".EnumParticle");

            Bukkit.getLogger().info("[ParticleUtil] 找到Packet类: " + packetClass.getName());
            Bukkit.getLogger().info("[ParticleUtil] 找到EnumParticle类: " + enumParticleClass.getName());

            Method valuesMethod = enumParticleClass.getMethod("values");
            enumParticleValues = (Object[]) valuesMethod.invoke(null);
            Bukkit.getLogger().info("[ParticleUtil] 找到 " + enumParticleValues.length + " 个EnumParticle值");

            packetConstructor = packetClass.getDeclaredConstructor(
                    enumParticleClass, boolean.class, float.class, float.class, float.class, float.class, float.class, float.class, float.class, int.class, int[].class);

            packetConstructor.setAccessible(true);
            Bukkit.getLogger().info("[ParticleUtil] 成功获取数据包构造函数");

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ParticleUtil] 初始化失败: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Object getEnumParticle(String particleName) {
        try {
            String upperName = particleName.toUpperCase();

            for (Object enumValue : enumParticleValues) {
                String enumName = enumValue.toString();
                if (enumName.equalsIgnoreCase(upperName)) {
                    return enumValue;
                }
            }

            String mappedName = mapParticleName(particleName);
            for (Object enumValue : enumParticleValues) {
                String enumName = enumValue.toString();
                if (enumName.equalsIgnoreCase(mappedName)) {
                    return enumValue;
                }
            }

            Bukkit.getLogger().warning("[ParticleUtil] 未找到EnumParticle: " + particleName + "，使用默认值");

            for (Object enumValue : enumParticleValues) {
                if (enumValue.toString().equalsIgnoreCase("FLAME")) {
                    return enumValue;
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ParticleUtil] 获取EnumParticle失败: " + e.getMessage());
        }

        return null;
    }

    private static String mapParticleName(String particleName) {
        String lowerName = particleName.toLowerCase();

        if (lowerName.contains("flame") || lowerName.contains("fire")) {
            return "FLAME";
        } else if (lowerName.contains("smoke")) {
            return "SMOKE";
        } else if (lowerName.contains("crit")) {
            if (lowerName.contains("magic")) {
                return "CRIT_MAGIC";
            }
            return "CRIT";
        } else if (lowerName.contains("redstone") || lowerName.contains("dust") || lowerName.contains("coloured")) {
            return "REDSTONE";
        } else if (lowerName.contains("portal")) {
            return "PORTAL";
        } else if (lowerName.contains("enchant")) {
            return "ENCHANTMENT_TABLE";
        } else if (lowerName.contains("potion") || lowerName.contains("spell")) {
            if (lowerName.contains("ambient")) {
                return "MOB_SPELL_AMBIENT";
            }
            return "MOB_SPELL";
        } else if (lowerName.contains("note")) {
            return "NOTE";
        } else if (lowerName.contains("heart")) {
            return "HEART";
        } else if (lowerName.contains("happy")) {
            return "VILLAGER_HAPPY";
        } else if (lowerName.contains("angry")) {
            return "VILLAGER_ANGRY";
        } else if (lowerName.contains("town")) {
            return "TOWN_AURA";
        } else if (lowerName.contains("lava")) {
            return "LAVA";
        } else if (lowerName.contains("water") || lowerName.contains("splash")) {
            return "WATER_SPLASH";
        } else if (lowerName.contains("snowball")) {
            return "SNOWBALL";
        } else if (lowerName.contains("slime")) {
            return "SLIME";
        } else if (lowerName.contains("explosion")) {
            if (lowerName.contains("large")) {
                return "EXPLOSION_LARGE";
            } else if (lowerName.contains("normal")) {
                return "EXPLOSION_NORMAL";
            }
            return "EXPLOSION_HUGE";
        } else if (lowerName.contains("firework")) {
            return "FIREWORKS_SPARK";
        }

        return particleName.toUpperCase();
    }

    public static void spawnParticle(Location location, String particleName,
                                     float offsetX, float offsetY, float offsetZ,
                                     float speed, int count, int... extraData) {
        try {
            if (packetConstructor == null) {
                Bukkit.getLogger().warning("[ParticleUtil] 数据包构造函数未初始化");
                return;
            }

            Object enumParticle = getEnumParticle(particleName);
            if (enumParticle == null) {
                Bukkit.getLogger().warning("[ParticleUtil] 无法获取EnumParticle: " + particleName);
                return;
            }

            Object packet = packetConstructor.newInstance(
                    enumParticle, true, (float) location.getX(),
                    (float) location.getY(),
                    (float) location.getZ(),
                    offsetX, offsetY, offsetZ,
                    speed,
                    count,
                    extraData
            );

            sendPacketToNearbyPlayers(location, packet);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ParticleUtil] 生成粒子失败 " + particleName + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void sendPacketToNearbyPlayers(Location location, Object packet) {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            Class<?> playerConnectionClass = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".Packet");

            Method getHandle = craftPlayerClass.getMethod("getHandle");
            Field playerConnectionField = entityPlayerClass.getField("playerConnection");
            Method sendPacket = playerConnectionClass.getMethod("sendPacket", packetClass);

            Collection<Player> players = location.getWorld().getPlayers();
            for (Player player : players) {
                double maxDistance = 128.0;
                if (player.getLocation().distanceSquared(location) <= maxDistance * maxDistance) {
                    Object craftPlayer = craftPlayerClass.cast(player);
                    Object entityPlayer = getHandle.invoke(craftPlayer);
                    Object connection = playerConnectionField.get(entityPlayer);
                    sendPacket.invoke(connection, packet);
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[ParticleUtil] 发送数据包失败: " + e.getMessage());
        }
    }

    public static void spawnFlame(Location location, int count) {
        spawnParticle(location, "flame", 0.05f, 0.05f, 0.05f, 0.0f, count);
    }

    public static void spawnSmoke(Location location, int count) {
        spawnParticle(location, "smoke", 0.1f, 0.1f, 0.1f, 0.0f, count);
    }

    public static void spawnParticleByName(Location location, String particleName,
                                           Color color, float offset, float speed, int count) {

        int[] extraData = null;

        if (color != null) {
            String lowerName = particleName.toLowerCase();
            if (color != null && (lowerName.contains("redstone") || lowerName.contains("dust"))) {
                spawnRedstoneParticleNative(location, color, offset, speed, count);
                return;
            } else if (lowerName.contains("note")) {
                int noteColor = (color.getRed() + color.getGreen() + color.getBlue()) / 32;
                noteColor = Math.min(24, Math.max(0, noteColor));
                extraData = new int[]{noteColor};
            }
        }

        if (extraData != null) {
            spawnParticle(location, particleName, offset, offset, offset, speed, count, extraData);
        } else {
            spawnParticle(location, particleName, offset, offset, offset, speed, count);
        }
    }

    private static void spawnRedstoneParticleNative(Location location, Color color,
                                                    float offset, float speed, int count) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles");
            Class<?> enumParticleClass = Class.forName("net.minecraft.server.v1_8_R3.EnumParticle");

            Object particleType;
            try {
                particleType = enumParticleClass.getField("COLOURED_DUST").get(null);
            } catch (NoSuchFieldException e) {
                particleType = enumParticleClass.getField("REDSTONE").get(null);
            }

            float red = color.getRed() / 255.0f;
            float green = color.getGreen() / 255.0f;
            float blue = color.getBlue() / 255.0f;

            int[] extraData = new int[]{
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue()
            };

            Random random = new Random();

            for (int i = 0; i < count; i++) {
                double offsetX = (random.nextDouble() - 0.5) * offset * 2;
                double offsetY = (random.nextDouble() - 0.5) * offset * 2;
                double offsetZ = (random.nextDouble() - 0.5) * offset * 2;

                float x = (float) (location.getX() + offsetX);
                float y = (float) (location.getY() + offsetY);
                float z = (float) (location.getZ() + offsetZ);

                Object packet = packetClass.getConstructor(
                        enumParticleClass, boolean.class, float.class, float.class, float.class,
                        float.class, float.class, float.class, float.class, int.class, int[].class
                ).newInstance(
                        particleType, true, x, y, z, red, green, blue, 1.0f, 0, extraData);

                sendPacketToNearbyPlayers(location, packet);
            }

        } catch (Exception e) {
            Bukkit.getLogger().severe("生成粒子失败: " + e.getMessage());
            e.printStackTrace();

            try {
                org.bukkit.World world = location.getWorld();
                if (world == null) return;

                Random random = new Random();

                int rgb = (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();

                int red = color.getRed() / 4;
                int green = color.getGreen() / 4;
                int blue = color.getBlue() / 4;
                int adjustedRgb = (red << 16) | (green << 8) | blue;

                for (int i = 0; i < count; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * offset * 2;
                    double offsetY = (random.nextDouble() - 0.5) * offset * 2;
                    double offsetZ = (random.nextDouble() - 0.5) * offset * 2;
                    Location particleLoc = location.clone().add(offsetX, offsetY, offsetZ);

                    world.playEffect(particleLoc, Effect.COLOURED_DUST, rgb);

                    world.playEffect(particleLoc, org.bukkit.Effect.COLOURED_DUST, adjustedRgb);

                    if (speed == 0) {
                        world.playEffect(particleLoc, org.bukkit.Effect.COLOURED_DUST, rgb | (1 << 24));
                        world.playEffect(particleLoc, org.bukkit.Effect.COLOURED_DUST, adjustedRgb | (1 << 24));
                    }
                }

            } catch (Exception e2) {
                Bukkit.getLogger().severe("生成粒子失败: " + e.getMessage());
                e2.printStackTrace();
            }
        }
    }

    public static boolean isInitialized() {
        return packetConstructor != null && enumParticleValues != null;
    }

    public static void listAvailableParticles() {
        if (enumParticleValues != null) {
            Bukkit.getLogger().info("[ParticleUtil] 可用的粒子类型:");
            for (Object enumValue : enumParticleValues) {
                Bukkit.getLogger().info("  - " + enumValue.toString());
            }
        }
    }
}