package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponHitBlockEvent;
import com.shampaggon.crackshot.events.WeaponReloadCompleteEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Accessories.AccessoriesManager;
import org.Spike.CSExtensions.Modifier.Services.HitLocationTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.*;

public class AccessoryMythicHandler implements Listener {
    private final CSExtensions plugin;
    private final AccessoriesManager accessoriesManager;
    private final AccessoryMythicConfig config;
    private final Map<UUID, Map<String, Integer>> playerTimers = new HashMap<>();
    private final Map<Integer, String> taskIdToPlayerMap = new HashMap<>();

    public AccessoryMythicHandler(CSExtensions plugin, AccessoriesManager accessoriesManager) {
        this.plugin = plugin;
        this.accessoriesManager = accessoriesManager;
        this.config = accessoriesManager.getConfig().getMythicConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTimerScheduler();
    }

    private void startTimerScheduler() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                handleTimerTrigger(player);
            }
        }, 1L, 1L);
    }

    private void handleTimerTrigger(Player player) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;
        for (String accessoryId : accessories) {
            Map<String, Integer> timers = playerTimers.computeIfAbsent(
                    player.getUniqueId(), k -> new HashMap<>());
            Integer ticksLeft = timers.get(accessoryId);
            if (ticksLeft == null) {
                List<AccessoryMythicEffect> timerEffects = config.getEffects(
                        accessoryId, AccessoryMythicTrigger.ON_TIMER);
                if (!timerEffects.isEmpty()) {
                    int timerTicks = timerEffects.get(0).getTimerTicks();
                    timers.put(accessoryId, timerTicks);
                }
                continue;
            }
            if (ticksLeft <= 0) {
                triggerTimerEffects(player, accessoryId);
                List<AccessoryMythicEffect> timerEffects = config.getEffects(
                        accessoryId, AccessoryMythicTrigger.ON_TIMER);
                if (!timerEffects.isEmpty()) {
                    timers.put(accessoryId, timerEffects.get(0).getTimerTicks());
                }
            } else {
                timers.put(accessoryId, ticksLeft - 1);
            }
        }
    }

    private void triggerTimerEffects(Player player, String accessoryId) {
        List<AccessoryMythicEffect> effects = config.getEffects(
                accessoryId, AccessoryMythicTrigger.ON_TIMER);
        for (AccessoryMythicEffect effect : effects) {
            executeSkill(effect, player, null, null, null,null);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponShoot(WeaponShootEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();
        Set<String> weaponTags = getWeaponTags(weaponId);
        triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_SHOOT, player, null, weaponTags,
                player.getLocation());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponAttack(WeaponDamageEntityEvent event) {
        Player player = event.getPlayer();
        LivingEntity victim = (LivingEntity) event.getVictim();
        String weaponId = event.getWeaponTitle();
        Set<String> weaponTags = getWeaponTags(weaponId);
        triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_ATTACK, player, victim,
                weaponTags, victim.getLocation());
        if (event.isCritical()) {
            triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_CRIT, player, victim,
                    weaponTags,victim.getLocation());
        }
        if (event.isHeadshot()) {
            triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_HEADSHOT, player, victim, weaponTags,victim.getLocation());
        }
        if (victim.getHealth() - event.getDamage() <= 0) {
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();
        Set<String> weaponTags = getWeaponTags(weaponId);
        triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_RELOAD, null, null, weaponTags,null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponHitBlock(WeaponHitBlockEvent event) {
        Player player = event.getPlayer();
        Block hitBlock = event.getBlock();
        String weaponId = event.getWeaponTitle();
        Set<String> weaponTags = getWeaponTags(weaponId);
        if (hitBlock != null) {
            Location hitLocation = hitBlock.getLocation().add(0.5, 0.5, 0.5);
            triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_HITBLOCK, player, null,
                    weaponTags,
                    new HitLocationTarget(hitLocation));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        LivingEntity attacker = getAttacker(event);
        triggerAccessoryEffects(player, AccessoryMythicTrigger.ON_DAMAGED, attacker, null, null,null);
    }
    private Set<String> getWeaponTags(String weaponId) {
        if (plugin.getSpikeElementsManager() != null) {
            return plugin.getSpikeElementsManager().getTagReader().getWeaponTags(weaponId);
        }
        return Collections.emptySet();
    }

    private void triggerAccessoryEffects(Player player, AccessoryMythicTrigger trigger,
                                         LivingEntity triggerEntity, LivingEntity victim,
                                         Set<String> weaponTags,Object locationTarget) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;
        for (String accessoryId : accessories) {
            List<AccessoryMythicEffect> effects = config.getEffects(accessoryId, trigger);
            if (effects.isEmpty()) continue;
            for (AccessoryMythicEffect effect : effects) {
                if (effect.checkCondition(weaponTags)) {
                    executeSkill(effect, player, triggerEntity, victim, weaponTags, locationTarget);
                }
            }
        }
    }

    private void executeSkill(AccessoryMythicEffect effect, Player caster,
                              LivingEntity trigger, LivingEntity victim,
                              Set<String> weaponTags, Object locationTarget) {
        try {
            String skillName = effect.getSkillName();
            String selector = effect.getTargetSelector();
            if (weaponTags == null) {
                weaponTags = Collections.emptySet();
            }
            if (plugin.getConfig().getBoolean("debug")) {
                String triggerName = trigger != null ? trigger.getName() : "null";
                String victimName = victim != null ? victim.getName() : "null";
                String tagsStr = weaponTags.isEmpty() ? "无属性" : String.join(", ", weaponTags);
                plugin.getLogger().info("[饰品Mythic] 执行技能: " + skillName +
                        ", 选择器: " + selector +
                        ", 触发者: " + triggerName +
                        ", 受害者: " + victimName +
                        ", 武器标签: " + tagsStr);
            }
            if (plugin.getConfig().getBoolean("debug") && weaponTags != null) {
                plugin.getLogger().info("[饰品Mythic] 武器标签: " +
                        (weaponTags.isEmpty() ? "无属性" : String.join(", ", weaponTags)));
            }
            Entity target = null;
            Location targetLocation = null;
            if ("@self".equals(selector) ||"@shooter".equals(selector) ) {
                target = caster;
            } else if ("@victim".equals(selector) && victim != null) {
                target = victim;
            } else if ("@trigger".equals(selector) && trigger != null) {
                target = trigger;
            } else if ("@hitlocation".equals(selector)) {
                if (locationTarget instanceof HitLocationTarget) {
                    targetLocation = ((HitLocationTarget) locationTarget).getLocation();
                } else if (locationTarget instanceof Location) {
                    targetLocation = (Location) locationTarget;
                } else if (victim != null) {
                    targetLocation = victim.getLocation();
                } else {
                    targetLocation = caster.getLocation();
                }
            }
            if (target != null) {
                boolean success = castSkill(caster,skillName, (LivingEntity) target);
                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().info(String.format(
                            "[饰品Mythic] 执行实体技能: %s -> %s, 结果: %s",
                            caster.getName(), target.getName(), success ? "成功" : "失败"
                    ));
                }
            } else if (targetLocation != null) {
                boolean success = castSkillAtLocation(caster, skillName, targetLocation);
                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().info(String.format(
                            "[饰品Mythic] 执行位置技能: %s -> 位置(%s), 结果: %s",
                            caster.getName(),
                            String.format("%.1f,%.1f,%.1f",
                                    targetLocation.getX(), targetLocation.getY(), targetLocation.getZ()),
                            success ? "成功" : "失败"
                    ));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("执行饰品Mythic技能失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug")) {
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
                plugin.getLogger().warning("[饰品Mythic] 技能不存在: " + skillName);
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
            plugin.getLogger().warning("[饰品Mythic] 在位置执行技能失败: " + e.getMessage());
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
                plugin.getLogger().warning("[饰品Mythic] 技能不存在: " + skillName);
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
            plugin.getLogger().warning("[饰品Mythic] 执行技能失败: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
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
                                plugin.getLogger().info("[饰品Mythic] 代理调用: " + methodName);
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
                plugin.getLogger().info("[饰品Mythic] 创建fakeSkillCaster失败: " + e.getMessage());
            }
            return null;
        }
    }

    private LivingEntity getAttacker(EntityDamageEvent event) {
        Entity damager = event.getEntity();
        if (event instanceof EntityDamageByEntityEvent) {
            Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
            if (attacker instanceof LivingEntity) {
                return (LivingEntity) attacker;
            }
        }
        return (LivingEntity)damager;
    }

    public void cleanup() {
        playerTimers.clear();
        for (Integer taskId : taskIdToPlayerMap.keySet()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskIdToPlayerMap.clear();
        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[饰品Mythic] 所有数据已清理");
        }
    }

    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        playerTimers.remove(playerId);
        Iterator<Map.Entry<Integer, String>> iterator = taskIdToPlayerMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            if (entry.getValue().equals(playerId.toString())) {
                Bukkit.getScheduler().cancelTask(entry.getKey());
                iterator.remove();
            }
        }
    }

    public void reload() {
        config.clear();
    }
}