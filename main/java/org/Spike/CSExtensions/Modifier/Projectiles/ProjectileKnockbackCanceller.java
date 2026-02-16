package org.Spike.CSExtensions.Modifier.Projectiles;

import org.Spike.CSExtensions.CSExtensions;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectileKnockbackCanceller implements Listener {
    private final CSExtensions plugin;
    private final ProjectilesManager projectilesManager;

    private static String version;
    private static Class<?> entityLivingClass;
    private static Class<?> genericAttributesClass;
    private static Class<?> attributeInstanceClass;
    private static Object knockbackResistanceAttribute;
    private static Class<?> iAttributeClass;
    private static boolean initialized = false;


    private static final Map<Integer, Double> originalNormalResistance = new ConcurrentHashMap<>();

    private static final Map<Integer, TaskInfo> activeTasks = new ConcurrentHashMap<>();

    static class TaskInfo {
        final int taskId;
        final int version;

        TaskInfo(int taskId, int version) {
            this.taskId = taskId;
            this.version = version;
        }
    }


    private static final AtomicInteger versionCounter = new AtomicInteger(0);

    public ProjectileKnockbackCanceller(CSExtensions plugin, ProjectilesManager projectilesManager) {
        this.plugin = plugin;
        this.projectilesManager = projectilesManager;
        initializeNMS();
    }

    private void initializeNMS() {
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

            entityLivingClass = Class.forName("net.minecraft.server." + version + ".EntityLiving");
            genericAttributesClass = Class.forName("net.minecraft.server." + version + ".GenericAttributes");

            try {
                attributeInstanceClass = Class.forName("net.minecraft.server." + version + ".AttributeInstance");
            } catch (ClassNotFoundException e) {
                attributeInstanceClass = Class.forName("net.minecraft.server." + version + ".AttributeModifiable");
            }

            iAttributeClass = findIAttributeClass();

            Field cField = genericAttributesClass.getDeclaredField("c");
            knockbackResistanceAttribute = cField.get(null);

            initialized = true;
            Bukkit.getLogger().info("[KnockbackCanceller] 初始化完成");

        } catch (Exception e) {
            Bukkit.getLogger().severe("[KnockbackCanceller] 初始化失败: " + e.getMessage());
        }
    }

    private Class<?> findIAttributeClass() {
        try {
            return Class.forName("net.minecraft.server." + version + ".IAttribute");
        } catch (ClassNotFoundException e) {
            for (Class<?> iface : genericAttributesClass.getInterfaces()) {
                if (iface.getSimpleName().equals("IAttribute")) {
                    return iface;
                }
            }
            return Object.class;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        if (!initialized) return;

        Player player = event.getPlayer();
        if (player == null) return;

        String weaponTitle = event.getWeaponTitle();
        if (!projectilesManager.hasProjectilesConfig(weaponTitle)) {
            return;
        }

        ProjectilesConfig config = projectilesManager.getProjectilesConfig(weaponTitle);
        if (config == null) return;

        if (!(event.getVictim() instanceof LivingEntity)) return;

        LivingEntity victim = (LivingEntity) event.getVictim();
        ProjectilesConfig.KnockbackType knockbackType = config.getNoknock();

        if (!shouldCancelKnockback(victim, knockbackType)) {
            return;
        }

        int entityId = victim.getEntityId();
        int currentVersion = versionCounter.incrementAndGet();

        try {

            if (!originalNormalResistance.containsKey(entityId)) {
                double normalResistance = getKnockbackResistance(victim);
                originalNormalResistance.put(entityId, normalResistance);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info(String.format(
                            "记录原始正常抗性: %s (ID: %d) = %.3f",
                            victim.getName(), entityId, normalResistance
                    ));
                }
            }


            TaskInfo oldTask = activeTasks.get(entityId);
            if (oldTask != null) {
                Bukkit.getScheduler().cancelTask(oldTask.taskId);


                Double originalValue = originalNormalResistance.get(entityId);
                if (originalValue != null) {
                    setKnockbackResistance(victim, originalValue);
                }
            }


            setKnockbackResistance(victim, 1.0);


            Double originalValue = originalNormalResistance.get(entityId);
            final double restoreValue = originalValue != null ? originalValue : 0.0;
            final int taskVersion = currentVersion;

            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                restoreToOriginal(victim, taskVersion, restoreValue);
            }, 2L).getTaskId();

            activeTasks.put(entityId, new TaskInfo(taskId, currentVersion));

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "设置100%%抗性: %s (ID: %d), 版本: %d, 将恢复到: %.3f",
                        victim.getName(), entityId, currentVersion, restoreValue
                ));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("设置击退抗性失败: " + e.getMessage());
        }
    }

    private boolean shouldCancelKnockback(LivingEntity victim, ProjectilesConfig.KnockbackType type) {
        if (type == ProjectilesConfig.KnockbackType.NONE) return false;
        if (type == ProjectilesConfig.KnockbackType.ALL) return true;
        if (type == ProjectilesConfig.KnockbackType.PLAYERS) return victim instanceof Player;
        if (type == ProjectilesConfig.KnockbackType.MOBS) return !(victim instanceof Player);
        return false;
    }

    private void restoreToOriginal(LivingEntity entity, int expectedVersion, double originalValue) {
        int entityId = entity.getEntityId();
        TaskInfo currentTask = activeTasks.get(entityId);

        if (currentTask == null || currentTask.version != expectedVersion) {
            return;
        }

        try {

            setKnockbackResistance(entity, originalValue);
            activeTasks.remove(entityId);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format(
                        "恢复到原始: %s (ID: %d) = %.3f, 版本: %d",
                        entity.getName(), entityId, originalValue, expectedVersion
                ));
            }

        } catch (Exception e) {
            activeTasks.remove(entityId);
        }
    }

    private void setKnockbackResistance(LivingEntity entity, double resistance) throws Exception {
        Class<?> craftLivingEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftLivingEntity");
        Method getHandle = craftLivingEntity.getMethod("getHandle");
        Object nmsEntity = getHandle.invoke(entity);

        Method getAttributeMap = entityLivingClass.getMethod("getAttributeMap");
        Object attributeMap = getAttributeMap.invoke(nmsEntity);


        Method getAttributeMethod = findGetAttributeMethod(attributeMap);
        if (getAttributeMethod == null) {
            throw new Exception("找不到获取属性的方法");
        }

        Object resistanceAttribute = getAttributeMethod.invoke(attributeMap, knockbackResistanceAttribute);

        if (resistanceAttribute != null) {
            Method setValueMethod = findSetValueMethod(resistanceAttribute);
            if (setValueMethod == null) {
                throw new Exception("找不到setValue方法");
            }

            setValueMethod.invoke(resistanceAttribute, resistance);
        } else {
            throw new Exception("击退抗性属性不存在");
        }
    }

    private double getKnockbackResistance(LivingEntity entity) throws Exception {
        Class<?> craftLivingEntity = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftLivingEntity");
        Method getHandle = craftLivingEntity.getMethod("getHandle");
        Object nmsEntity = getHandle.invoke(entity);

        Method getAttributeMap = entityLivingClass.getMethod("getAttributeMap");
        Object attributeMap = getAttributeMap.invoke(nmsEntity);

        Method getAttributeMethod = findGetAttributeMethod(attributeMap);
        if (getAttributeMethod == null) {
            return 0.0;
        }

        Object resistanceAttribute = getAttributeMethod.invoke(attributeMap, knockbackResistanceAttribute);

        if (resistanceAttribute != null) {
            Method getValueMethod = findGetValueMethod(resistanceAttribute);
            if (getValueMethod != null) {
                return (double) getValueMethod.invoke(resistanceAttribute);
            }
        }

        return 0.0;
    }

    private Method findGetAttributeMethod(Object attributeMap) {

        String[] methodNames = {"a", "getAttribute", "getInstance", "b"};

        for (String methodName : methodNames) {
            try {
                return attributeMap.getClass().getMethod(methodName, iAttributeClass);
            } catch (NoSuchMethodException e) {

            }
        }


        for (Method method : attributeMap.getClass().getMethods()) {
            if (method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(knockbackResistanceAttribute.getClass())) {
                return method;
            }
        }

        return null;
    }

    private Method findSetValueMethod(Object attribute) {
        for (Method method : attribute.getClass().getMethods()) {
            if (method.getName().equals("setValue") && method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0] == double.class) {
                return method;
            }
        }
        return null;
    }

    private Method findGetValueMethod(Object attribute) {
        for (Method method : attribute.getClass().getMethods()) {
            if (method.getName().equals("getValue") && method.getParameterCount() == 0) {
                return method;
            }
        }
        return null;
    }


    public boolean isInitialized() {
        return initialized;
    }
}