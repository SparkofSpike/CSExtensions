package org.Spike.CSExtensions.Modifier.Mythic;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.*;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.bukkit.BukkitAPIHelper;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Services.HitLocationTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MythicEventHandler implements Listener {
    private final CSExtensions plugin;
    private final MythicConfigManager configManager;
    private final CSUtility csUtility;
    private final BukkitAPIHelper mmAPI;
    private final MythicDropListener dropListener;
    private final Map<UUID, String> activeWeapons = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> weaponTimers = new HashMap<>();
    private int schedulerTaskId = -1;
    private final Map<UUID, Map<String, Integer>> playerWeaponTimers = new HashMap<>();
    private final Map<Integer, String> taskIdToWeaponMap = new HashMap<>();

    public MythicEventHandler(CSExtensions plugin, MythicConfigManager configManager, CSUtility csUtility) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.csUtility = csUtility;
        this.mmAPI = MythicMobs.inst().getAPIHelper();

        this.dropListener = new MythicDropListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(dropListener, plugin);

        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            plugin.getLogger().info("CrackShot掉落监听器已注册");
        } else {
            plugin.getLogger().warning("MythicMobs未启用，CrackShot掉落支持已禁用");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTimerScheduler();
        plugin.getLogger().info("MythicEventHandler构造函数已加载");

    }

    private void startTimerScheduler() {
        schedulerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Map.Entry<UUID, Map<String, Integer>> playerEntry : weaponTimers.entrySet()) {
                UUID playerId = playerEntry.getKey();
                Player player = Bukkit.getPlayer(playerId);
                if (player == null || !player.isOnline()) {
                    continue;
                }

                String weaponId = activeWeapons.get(playerId);
                if (weaponId == null) {
                    continue;
                }

                Map<String, Integer> timers = playerEntry.getValue();
                for (Map.Entry<String, Integer> timerEntry : new HashMap<>(timers).entrySet()) {
                    String timerWeaponId = timerEntry.getKey();
                    int ticksLeft = timerEntry.getValue();

                    if (ticksLeft <= 0) {
                        List<MythicEffect> timerEffects = configManager.getEffects(timerWeaponId, TriggerType.TIMER);
                        for (MythicEffect effect : timerEffects) {
                            if (Math.random() < effect.getChance() && effect.checkHealthCondition(player)) {
                                executeSkill(effect, player, null);
                            }
                        }
                        timers.put(timerWeaponId, getTimerTicks(timerWeaponId));
                    } else {
                        timers.put(timerWeaponId, ticksLeft - 1);
                    }
                }
            }
        }, 1L, 1L);
    }

    private int getTimerTicks(String weaponId) {
        List<MythicEffect> effects = configManager.getEffects(weaponId, TriggerType.TIMER);
        if (effects.isEmpty()) return 0;
        return effects.get(0).getTimerTicks();
    }

    private void executeSkill(MythicEffect effect, Player shooter, Object targetOrLocation) {
        try {
            String command = effect.getSkillCommand();

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("=== [Mythic调试] 开始执行技能 ===");
                plugin.getLogger().info("[Mythic调试] 完整命令: " + command);
                plugin.getLogger().info("[Mythic调试] 触发类型: " + effect.getTrigger());
                plugin.getLogger().info("[Mythic调试] 选择器: " + effect.getTargetSelector());
                plugin.getLogger().info("[Mythic调试] 目标/位置类型: " +
                        (targetOrLocation == null ? "null" : targetOrLocation.getClass().getSimpleName()));

                if (targetOrLocation instanceof HitLocationTarget) {
                    plugin.getLogger().info("[Mythic调试] 命中位置: " +
                            ((HitLocationTarget) targetOrLocation).getLocation());
                } else if (targetOrLocation instanceof LivingEntity) {
                    plugin.getLogger().info("[Mythic调试] 目标实体: " +
                            ((LivingEntity) targetOrLocation).getName());
                }
            }

            String[] parts = command.split("\\s+", 2);
            if (parts.length < 1) return;

            String skillName = parts[0];

            String selector = effect.getTargetSelector();

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 使用选择器: " + selector);
            }

            if (selector.equals("@hitlocation")) {
                Location targetLocation = null;

                if (targetOrLocation instanceof HitLocationTarget) {
                    targetLocation = ((HitLocationTarget) targetOrLocation).getLocation();
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 使用HitLocationTarget位置");
                    }
                } else if (targetOrLocation instanceof LivingEntity) {
                    targetLocation = ((LivingEntity) targetOrLocation).getLocation();
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 使用实体位置（降级）");
                    }
                } else {
                    targetLocation = shooter.getLocation();
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 使用玩家位置（默认）");
                    }
                }

                boolean success = castSkillAtLocation(shooter, skillName, targetLocation);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 位置技能执行结果: " + success);
                }

            } else if (selector.equals("@victim")) {
                if (targetOrLocation instanceof LivingEntity &&
                        !(targetOrLocation instanceof HitLocationTarget)) {
                    boolean success = castSkill(shooter, skillName, (LivingEntity) targetOrLocation);
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 目标技能执行结果: " + success);
                    }
                } else {
                    boolean success = castSkill(shooter, skillName, shooter);
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 降级到自身技能执行结果: " + success);
                    }
                }

            } else if (selector.equals("@Self") || selector == null) {
                boolean success = castSkill(shooter, skillName, shooter);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 自身技能执行结果: " + success);
                }

            } else {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("[Mythic调试] 未知选择器: " + selector);
                }
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("=== [Mythic调试] 技能执行结束 ===\n");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("执行Mythic技能失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }



    private boolean castSkillAtLocation(Player caster, String skillName, Location location) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);

            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);

            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            java.util.Optional<?> maybeSkill = (java.util.Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                plugin.getLogger().warning("[Mythic] 技能不存在: " + skillName);
                return false;
            }

            Object skill = maybeSkill.get();

            Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);

            Object abstractLocation = adaptLocationMethod.invoke(null, location);

            Object abstractCaster = bukkitAdapterClass.getMethod("adapt", Entity.class).invoke(null, caster);

            Object mobManager = mythicMobsClass.getMethod("getMobManager").invoke(mythicMobsInstance);
            Class<?> mobManagerClass = Class.forName("io.lumine.xikage.mythicmobs.mobs.MobManager");

            Object fakeSkillCaster = createFakeSkillCaster(abstractCaster, caster,
                    Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger")
                            .getField("API").get(null));

            java.util.HashSet<Object> feTargets = new java.util.HashSet<>();
            java.util.HashSet<Object> flTargets = new java.util.HashSet<>();
            flTargets.add(abstractLocation);

            Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
            Object apiTrigger = skillTriggerClass.getField("API").get(null);

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                    skillTriggerClass,
                    skillCasterClass,
                    abstractEntityClass,
                    abstractLocationClass,
                    java.util.HashSet.class,
                    java.util.HashSet.class,
                    Float.TYPE
            );

            Object skillMetadata = constructor.newInstance(
                    apiTrigger,
                    fakeSkillCaster,
                    abstractCaster,
                    abstractLocation,
                    feTargets,
                    flTargets,
                    1.0F
            );

            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadataClass);
            executeMethod.invoke(skill, skillMetadata);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("[Mythic] 在位置执行技能失败: " + e.getMessage());
            return false;
        }
    }

    private boolean castSkill(Player caster, String skillName, LivingEntity target) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);

            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);

            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            java.util.Optional<?> maybeSkill = (java.util.Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                plugin.getLogger().warning("[Mythic] 技能不存在: " + skillName);
                return false;
            }

            Object skill = maybeSkill.get();

            Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptEntityMethod = bukkitAdapterClass.getMethod("adapt", Entity.class);

            Object abstractCaster = adaptEntityMethod.invoke(null, caster);
            Object abstractTarget = adaptEntityMethod.invoke(null, target);

            Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
            Object apiTrigger = skillTriggerClass.getField("API").get(null);

            Object fakeSkillCaster = createFakeSkillCaster(abstractCaster, caster, apiTrigger);

            java.util.HashSet<Object> feTargets = new java.util.HashSet<>();
            java.util.HashSet<Object> flTargets = new java.util.HashSet<>();
            feTargets.add(abstractTarget);

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                    skillTriggerClass,
                    skillCasterClass,
                    abstractEntityClass,
                    abstractLocationClass,
                    java.util.HashSet.class,
                    java.util.HashSet.class,
                    Float.TYPE
            );

            Object targetLocation = bukkitAdapterClass.getMethod("adapt", Location.class).invoke(null, target.getLocation());

            Object skillMetadata = constructor.newInstance(
                    apiTrigger,
                    fakeSkillCaster,
                    abstractTarget,
                    targetLocation,
                    feTargets,
                    flTargets,
                    1.0F
            );


            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadataClass);
            executeMethod.invoke(skill, skillMetadata);

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("[Mythic] 执行技能失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private boolean castSkillViaEvent(Player caster, String skillName, LivingEntity target) {
        try {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 尝试新方法：直接内部执行");
            }


            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);


            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);


            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            java.util.Optional<?> maybeSkill = (java.util.Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                plugin.getLogger().warning("[Mythic] 技能不存在: " + skillName);
                return false;
            }

            Object skill = maybeSkill.get();

            Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
            Object apiTrigger = skillTriggerClass.getField("API").get(null);

            Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptEntityMethod = bukkitAdapterClass.getMethod("adapt", Entity.class);
            java.lang.reflect.Method adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);

            Object abstractCaster = adaptEntityMethod.invoke(null, caster);
            Object abstractTrigger = target != null ? adaptEntityMethod.invoke(null, target) : abstractCaster;
            Object abstractOrigin = adaptLocationMethod.invoke(null, caster.getLocation());

            Object fakeSkillCaster = createFakeSkillCaster(abstractCaster, caster, apiTrigger);

            if (fakeSkillCaster == null) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 无法创建fakeSkillCaster");
                }
                return false;
            }

            java.util.HashSet<Object> feTargets = new java.util.HashSet<>();
            java.util.HashSet<Object> flTargets = new java.util.HashSet<>();

            feTargets.add(abstractTrigger);
            flTargets.add(abstractOrigin);

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                    skillTriggerClass,
                    skillCasterClass,
                    abstractEntityClass,
                    abstractLocationClass,
                    java.util.HashSet.class,
                    java.util.HashSet.class,
                    Float.TYPE
            );

            Object skillMetadata = constructor.newInstance(
                    apiTrigger,
                    fakeSkillCaster,
                    abstractTrigger,
                    abstractOrigin,
                    feTargets,
                    flTargets,
                    1.0F
            );

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] SkillMetadata创建成功，尝试执行技能");
            }

            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadataClass);
            executeMethod.invoke(skill, skillMetadata);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 技能执行调用完成");
            }

            return true;

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 新方法失败: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    private Object createFakeSkillCaster(Object abstractEntity, Entity realEntity, Object skillTrigger) {
        try {
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");

            return java.lang.reflect.Proxy.newProxyInstance(
                    skillCasterClass.getClassLoader(),
                    new Class<?>[]{skillCasterClass},
                    new java.lang.reflect.InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                            String methodName = method.getName();

                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[Mythic调试] 代理调用: " + methodName);
                            }

                            if (methodName.equals("getEntity")) {
                                return abstractEntity;
                            } else if (methodName.equals("getLivingEntity")) {
                                return abstractEntity;
                            } else if (methodName.equals("getBukkitEntity")) {
                                return realEntity;
                            } else if (methodName.equals("getLocation")) {
                                java.lang.reflect.Method getLocationMethod = abstractEntityClass.getMethod("getLocation");
                                return getLocationMethod.invoke(abstractEntity);
                            } else if (methodName.equals("getUniqueId")) {
                                return realEntity.getUniqueId();
                            } else if (methodName.equals("getName")) {
                                return realEntity.getName();
                            } else if (methodName.equals("isDead")) {
                                return !realEntity.isValid();
                            } else if (methodName.equals("getSkillTrigger")) {
                                return skillTrigger;
                            } else if (methodName.equals("toString")) {
                                return "FakeSkillCaster[" + realEntity.getName() + "]";
                            } else if (methodName.equals("hashCode")) {
                                return realEntity.hashCode();
                            } else if (methodName.equals("equals")) {
                                if (args != null && args.length > 0) {
                                    return proxy == args[0];
                                }
                                return false;
                            } else if (methodName.equals("getHealth") || methodName.equals("getMaxHealth")) {
                                if (realEntity instanceof LivingEntity) {
                                    LivingEntity living = (LivingEntity) realEntity;
                                    if (methodName.equals("getHealth")) {
                                        return living.getHealth();
                                    } else {
                                        return living.getMaxHealth();
                                    }
                                }
                                return 20.0;
                            } else if (methodName.equals("getLevel") || methodName.equals("getMythicLevel")) {
                                return 1;
                            } else if (methodName.equals("getFaction") || methodName.equals("getThreatTable")) {
                                return null;
                            } else if (methodName.equals("hasTarget") || methodName.equals("isInCombat")) {
                                return false;
                            } else if (methodName.equals("getTarget") || methodName.equals("getTopThreat")) {
                                return null;
                            } else if (methodName.equals("getLastDamageCause") || methodName.equals("getLastDamager")) {
                                return null;
                            } else if (methodName.equals("getVariables") || methodName.equals("getSignal")) {
                                return new java.util.HashMap<>();
                            }

                            Class<?> returnType = method.getReturnType();

                            if (returnType == Void.TYPE || returnType == void.class) {
                                return null;
                            } else if (returnType == Boolean.TYPE || returnType == boolean.class) {
                                return false;
                            } else if (returnType == Integer.TYPE || returnType == int.class) {
                                return 0;
                            } else if (returnType == Double.TYPE || returnType == double.class) {
                                return 0.0;
                            } else if (returnType == Float.TYPE || returnType == float.class) {
                                return 0.0f;
                            } else if (returnType == Long.TYPE || returnType == long.class) {
                                return 0L;
                            } else if (returnType.isPrimitive()) {
                                return 0;
                            } else {
                                return null;
                            }
                        }
                    }
            );

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 创建fakeSkillCaster失败: " + e.getMessage());
            }
            return null;
        }
    }

    private boolean simulateMythicEvent(Player caster, Object skill, LivingEntity target) {
        try {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 模拟事件，技能类型: " + skill.getClass().getName());
            }

            Object skillMetadata = createSimpleSkillMetadata(caster, target);
            if (skillMetadata == null) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] SkillMetadata为null");
                }
                return false;
            }

            try {
                Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
                java.lang.reflect.Method getCasterMethod = skillMetadataClass.getMethod("getCaster");
                Object casterObj = getCasterMethod.invoke(skillMetadata);

                if (casterObj == null) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] SkillMetadata中的caster为null");
                    }
                    return false;
                }
            } catch (Exception e) {
            }

            Class<?> skillClass = skill.getClass();

            try {
                java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadata.getClass());
                executeMethod.invoke(skill, skillMetadata);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 直接execute成功");
                }
                return true;
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 直接execute失败: " + e.getMessage());
                }
            }

            try {
                Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
                Object apiTrigger = skillTriggerClass.getField("API").get(null);

                java.lang.reflect.Method usableMethod = skillClass.getMethod("usable", skillMetadata.getClass(), skillTriggerClass);
                boolean usable = (boolean) usableMethod.invoke(skill, skillMetadata, apiTrigger);

                if (usable) {
                    java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadata.getClass());
                    executeMethod.invoke(skill, skillMetadata);

                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] usable检查后execute成功");
                    }
                    return true;
                } else {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 技能不可用");
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] usable检查失败: " + e.getMessage());
                }
            }

            return false;

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 模拟事件失败: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    private Object createSimpleSkillMetadata(Player caster, LivingEntity target) {
        try {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 创建SkillMetadata, caster: " + caster.getName());
            }

            Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptEntityMethod = bukkitAdapterClass.getMethod("adapt", Entity.class);
            java.lang.reflect.Method adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);

            Object abstractCaster = adaptEntityMethod.invoke(null, caster);
            Object abstractTrigger = target != null ? adaptEntityMethod.invoke(null, target) : abstractCaster;
            Object abstractOrigin = adaptLocationMethod.invoke(null, caster.getLocation());

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 适配对象创建完成");
            }

            Object apiTrigger = skillTriggerClass.getField("API").get(null);

            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);
            Object mobManager = mythicMobsClass.getMethod("getMobManager").invoke(mythicMobsInstance);
            Class<?> mobManagerClass = mobManager.getClass();

            Object activeMob = null;

            try {
                java.lang.reflect.Method getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", Entity.class);
                activeMob = getMythicMobInstanceMethod.invoke(mobManager, caster);

                if (activeMob != null && plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] getMythicMobInstance返回: " + activeMob.getClass().getName());
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] getMythicMobInstance失败: " + e.getMessage());
                }
            }

            if (activeMob == null) {
                try {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 尝试生成临时MM生物");
                    }

                    java.lang.reflect.Method spawnMobMethod = null;
                    for (java.lang.reflect.Method method : mobManagerClass.getMethods()) {
                        if (method.getName().equals("spawnMob") && method.getParameterCount() >= 2) {
                            spawnMobMethod = method;
                            break;
                        }
                    }

                    if (spawnMobMethod != null) {
                        Class<?>[] paramTypes = spawnMobMethod.getParameterTypes();

                        if (paramTypes[0].equals(String.class)) {
                            activeMob = spawnMobMethod.invoke(mobManager, "Player", abstractOrigin);
                        } else if (paramTypes[0].getName().contains("EntityType")) {
                            Class<?> entityTypeClass = Class.forName("io.lumine.xikage.mythicmobs.mobs.EntityType");
                            java.lang.reflect.Method valueOfMethod = entityTypeClass.getMethod("valueOf", String.class);
                            Object playerType = valueOfMethod.invoke(null, "PLAYER");
                            activeMob = spawnMobMethod.invoke(mobManager, playerType, abstractOrigin);
                        }

                        if (activeMob != null) {
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[Mythic调试] 生成临时MM生物成功: " + activeMob.getClass().getName());
                            }

                            scheduleMobRemoval(activeMob);
                        }
                    }
                } catch (Exception e) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 生成临时MM生物失败: " + e.getMessage());
                    }
                }
            }

            if (activeMob == null) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 使用abstractCaster作为备用SkillCaster");
                }

                activeMob = abstractCaster;
            }

            if (!skillCasterClass.isInstance(activeMob)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] activeMob不是SkillCaster，尝试转换");
                    plugin.getLogger().info("[Mythic调试] activeMob实际类型: " + activeMob.getClass().getName());
                }

                return null;
            }

            java.util.HashSet<Object> feTargets = new java.util.HashSet<>();
            java.util.HashSet<Object> flTargets = new java.util.HashSet<>();

            feTargets.add(abstractTrigger);
            flTargets.add(abstractOrigin);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 准备创建SkillMetadata:");
                plugin.getLogger().info("[Mythic调试]   trigger: " + apiTrigger.getClass().getName());
                plugin.getLogger().info("[Mythic调试]   caster类型: " + activeMob.getClass().getName());
                plugin.getLogger().info("[Mythic调试]   是否是SkillCaster: " + skillCasterClass.isInstance(activeMob));
                plugin.getLogger().info("[Mythic调试]   triggerEntity类型: " + abstractTrigger.getClass().getName());
                plugin.getLogger().info("[Mythic调试]   origin类型: " + abstractOrigin.getClass().getName());
            }

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");

            try {
                java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                        skillTriggerClass,
                        skillCasterClass,
                        abstractEntityClass,
                        abstractLocationClass,
                        java.util.HashSet.class,
                        java.util.HashSet.class,
                        Float.TYPE
                );

                Object skillMetadata = constructor.newInstance(
                        apiTrigger,
                        activeMob,
                        abstractTrigger,
                        abstractOrigin,
                        feTargets,
                        flTargets,
                        1.0F
                );

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] SkillMetadata创建成功!");
                }

                return skillMetadata;

            } catch (NoSuchMethodException e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 构造方法不存在，尝试其他");
                }

                for (java.lang.reflect.Constructor<?> constructor : skillMetadataClass.getConstructors()) {
                    try {
                        if (constructor.getParameterCount() == 7) {
                            Object skillMetadata = constructor.newInstance(
                                    apiTrigger,
                                    activeMob,
                                    abstractTrigger,
                                    abstractOrigin,
                                    feTargets,
                                    flTargets,
                                    1.0F
                            );

                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("[Mythic调试] 使用构造方法成功: " + constructor);
                            }
                            return skillMetadata;
                        }
                    } catch (Exception e2) {
                    }
                }
            }

            return null;

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 创建SkillMetadata失败: " + e.getMessage());
            }
            return null;
        }
    }

    private void scheduleMobRemoval(Object mob) {
        try {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    java.lang.reflect.Method removeMethod = mob.getClass().getMethod("remove");
                    removeMethod.invoke(mob);
                } catch (Exception e) {
                }
            }, 1L);
        } catch (Exception e) {
        }
    }

    private boolean castSkillViaTargeterTrick(Entity caster, String skillName, Entity trigger, Location origin) {
        try {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 尝试备用方法：目标器技巧");
            }


            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);

            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);
            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            java.util.Optional<?> maybeSkill = (java.util.Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                return false;
            }

            Object skill = maybeSkill.get();

            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method getMechanicMethod = skillClass.getMethod("getMechanic");
            Object mechanic = getMechanicMethod.invoke(skill);

            if (mechanic == null) {
                return false;
            }

            Class<?> mechanicClass = mechanic.getClass();

            for (java.lang.reflect.Method method : mechanicClass.getMethods()) {
                if (method.getName().equals("execute") && method.getParameterCount() >= 1) {
                    try {
                        if (method.getParameterCount() == 6) {
                            Collection<Entity> eTargets = trigger != null ? Collections.singleton(trigger) : null;
                            Collection<Location> lTargets = origin != null ? Collections.singleton(origin) : null;
                            Location finalOrigin = origin != null ? origin : caster.getLocation();

                            method.invoke(mechanic, caster, trigger, finalOrigin, eTargets, lTargets, 1.0F);
                            return true;

                        } else if (method.getParameterCount() == 3) {
                            Location finalOrigin = origin != null ? origin : caster.getLocation();
                            method.invoke(mechanic, caster, finalOrigin, 1.0F);
                            return true;
                        }
                    } catch (Exception e) {
                    }
                }
            }

            return false;

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic调试] 备用方法失败: " + e.getMessage());
            }
            return false;
        }
    }

    private java.util.HashSet<?> convertSetTypes(java.util.HashSet<Object> originalSet, Class<?> targetType) {
        java.util.HashSet<Object> result = new java.util.HashSet<>();

        for (Object obj : originalSet) {
            if (targetType.isInstance(obj)) {
                result.add(targetType.cast(obj));
            } else {
                result.add(obj);
            }
        }

        return result;
    }

    private boolean castSkillViaAPIWrapper(Entity caster, String skillName, Entity trigger, Location origin) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicMobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);

            Object mobManager = mythicMobsClass.getMethod("getMobManager").invoke(mythicMobsInstance);

            Class<?> mobManagerClass = mobManager.getClass();
            java.lang.reflect.Method registerActiveMobMethod = null;

            try {
                for (java.lang.reflect.Method method : mobManagerClass.getMethods()) {
                    if (method.getName().equals("registerActiveMob") && method.getParameterCount() >= 1) {
                        registerActiveMobMethod = method;
                        break;
                    }
                }

                if (registerActiveMobMethod != null) {
                    Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
                    Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
                    java.lang.reflect.Method adaptEntityMethod = bukkitAdapterClass.getMethod("adapt", org.bukkit.entity.Entity.class);
                    Object abstractEntity = adaptEntityMethod.invoke(null, caster);

                    Object activeMob = registerActiveMobMethod.invoke(mobManager, abstractEntity);

                    if (activeMob != null) {
                        Collection<Entity> eTargets = trigger != null ? Collections.singleton(trigger) : null;
                        Collection<Location> lTargets = origin != null ? Collections.singleton(origin) : null;

                        return mmAPI.castSkill(caster, skillName, origin != null ? origin : caster.getLocation(),
                                eTargets, lTargets, 1.0F);
                    }
                }
            } catch (Exception e) {
            }
            return mmAPI.castSkill(caster, skillName);

        } catch (Exception e) {
            plugin.getLogger().warning("[Mythic] API包装方法失败: " + e.getMessage());
            return false;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponShoot(WeaponShootEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, TriggerType.SHOOT)) {
            return;
        }

        handleTrigger(event.getPlayer(), weaponTitle, TriggerType.SHOOT, null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle)) {
            return;
        }

        boolean hasHitTrigger = configManager.hasTriggerType(weaponTitle, TriggerType.HIT);
        boolean hasCritTrigger = configManager.hasTriggerType(weaponTitle, TriggerType.CRIT);
        boolean hasHeadshotTrigger = configManager.hasTriggerType(weaponTitle, TriggerType.HEADSHOT);
        boolean hasKillTrigger = configManager.hasTriggerType(weaponTitle, TriggerType.KILL);

        if (!hasHitTrigger && !hasCritTrigger && !hasHeadshotTrigger && !hasKillTrigger) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || !(event.getVictim() instanceof LivingEntity)) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getVictim();

        if (hasHitTrigger) {
            handleTrigger(player, weaponTitle, TriggerType.HIT, victim);
        }

        if (event.isCritical() && hasCritTrigger) {
            handleTrigger(player, weaponTitle, TriggerType.CRIT, victim);
        }

        if (event.isHeadshot() && hasHeadshotTrigger) {
            handleTrigger(player, weaponTitle, TriggerType.HEADSHOT, victim);
        }

        if (hasKillTrigger && victim.getHealth() - event.getDamage() <= 0) {
            handleTrigger(player, weaponTitle, TriggerType.KILL, victim);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponHitBlock(WeaponHitBlockEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, TriggerType.HITBLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) return;

        Block hitBlock = event.getBlock();
        if (hitBlock == null) return;

        Location hitLocation = hitBlock.getLocation().add(0.5, 0.5, 0.5);
        HitLocationTarget virtualTarget = new HitLocationTarget(hitLocation);

        handleTrigger(player, weaponTitle, TriggerType.HITBLOCK, virtualTarget);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, TriggerType.RELOAD)) {
            return;
        }

        handleTrigger(event.getPlayer(), event.getWeaponTitle(), TriggerType.RELOAD, null);
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        stopPlayerTimers(playerId);

        if (item != null) {
            String weaponTitle = csUtility.getWeaponTitle(item);

            if (weaponTitle != null) {
                startWeaponTimers(player, weaponTitle);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        stopPlayerTimers(playerId);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        stopPlayerTimers(playerId);
    }

    private void startWeaponTimers(Player player, String weaponTitle) {
        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, TriggerType.TIMER)) {
            return;
        }

        List<MythicEffect> timerEffects = configManager.getEffects(weaponTitle, TriggerType.TIMER);
        if (timerEffects.isEmpty()) return;
        UUID playerId = player.getUniqueId();
        Map<String, Integer> timers = playerWeaponTimers.computeIfAbsent(playerId, k -> new HashMap<>());

        for (MythicEffect effect : timerEffects) {
            int timerTicks = effect.getTimerTicks();
            if (timerTicks <= 0) continue;

            final String finalWeaponTitle = weaponTitle;
            final UUID finalPlayerId = playerId;

            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                Player currentPlayer = Bukkit.getPlayer(finalPlayerId);
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    stopWeaponTimer(finalPlayerId, finalWeaponTitle);
                    return;
                }

                ItemStack currentItem = currentPlayer.getItemInHand();
                String currentWeapon = csUtility.getWeaponTitle(currentItem);

                if (!finalWeaponTitle.equals(currentWeapon)) {
                    stopWeaponTimer(finalPlayerId, finalWeaponTitle);
                    return;
                }

                if (Math.random() >= effect.getChance()) return;
                if (!effect.checkHealthCondition(currentPlayer)) return;

                executeSkill(effect, currentPlayer, null);

            }, timerTicks, timerTicks);

            timers.put(weaponTitle, taskId);
            taskIdToWeaponMap.put(taskId, weaponTitle + ":" + playerId);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic] 启动定时器: 玩家=" + player.getName() +
                        ", 武器=" + weaponTitle + ", 间隔=" + timerTicks + "tick");
            }
        }
    }

    private void stopPlayerTimers(UUID playerId) {
        Map<String, Integer> timers = playerWeaponTimers.remove(playerId);
        if (timers == null) return;

        for (int taskId : timers.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskIdToWeaponMap.remove(taskId);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Mythic] 停止玩家所有定时器: " + playerId);
        }
    }

    private void handleTrigger(Player player, String weaponTitle, TriggerType trigger, Object targetOrLocation) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, trigger);
        if (effects.isEmpty()) return;

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Mythic调试] 触发处理: " + weaponTitle +
                    " 触发类型: " + trigger +
                    " 效果数量: " + effects.size());
        }

        for (MythicEffect effect : effects) {
            if (Math.random() >= effect.getChance()) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Mythic调试] 几率检查失败: " + effect.getChance());
                }
                continue;
            }

            boolean skipHealthCheck = false;
            if (trigger == TriggerType.HITBLOCK ||
                    trigger == TriggerType.SHOOT ||
                    trigger == TriggerType.RELOAD ||
                    trigger == TriggerType.TIMER) {
                skipHealthCheck = true;
            }

            if (!skipHealthCheck && targetOrLocation instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) targetOrLocation;
                LivingEntity conditionTarget = effect.getTargetSelector().equals("@victim") ? target : player;
                if (conditionTarget != null && !effect.checkHealthCondition(conditionTarget)) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("[Mythic调试] 血量条件检查失败");
                    }
                    continue;
                }
            }

            executeSkill(effect, player, targetOrLocation);
        }
    }

    private void stopWeaponTimer(UUID playerId, String weaponTitle) {
        Map<String, Integer> timers = playerWeaponTimers.get(playerId);
        if (timers == null) return;

        Integer taskId = timers.remove(weaponTitle);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskIdToWeaponMap.remove(taskId);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic] 停止武器定时器: 玩家=" + playerId + ", 武器=" + weaponTitle);
            }
        }
    }

    public void reload() {
        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
        }

        configManager.reload();
        activeWeapons.clear();
        weaponTimers.clear();

        startTimerScheduler();
    }

    public void cleanup() {
        for (Map<String, Integer> timers : playerWeaponTimers.values()) {
            for (int taskId : timers.values()) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
        playerWeaponTimers.clear();
        taskIdToWeaponMap.clear();

        if (schedulerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(schedulerTaskId);
        }

        if (dropListener != null) {
            HandlerList.unregisterAll(dropListener);
        }
    }
}