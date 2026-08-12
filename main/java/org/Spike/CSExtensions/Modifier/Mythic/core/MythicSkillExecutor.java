package org.Spike.CSExtensions.Modifier.Mythic.core;

import io.lumine.xikage.mythicmobs.MythicMobs;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.Spike.CSExtensions.CSExtensions;

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Optional;

public class MythicSkillExecutor {
    private final CSExtensions plugin;

    public MythicSkillExecutor(CSExtensions plugin) {
        this.plugin = plugin;
    }

    public boolean executeSkill(Player caster, String skillName, String selector,
                                Location location, LivingEntity target, LivingEntity trigger) {
        try {
            if ("@hitlocation".equals(selector)) {
                if (location != null) {
                    return castSkillAtLocation(caster, skillName, location);
                } else if(target != null) {
                    return castSkillAtLocation(caster, skillName, target.getLocation());
                } else {
                    return castSkillAtLocation(caster, skillName, caster.getLocation());
                }
            } else if ("@victim".equals(selector) && target != null) {
                return castSkill(caster, skillName, target);
            } else if ("@trigger".equals(selector) && trigger != null) {
                return castSkill(caster, skillName, trigger);
            } else {
                return castSkill(caster, skillName, caster);
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().warning("执行Mythic技能失败: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean castSkill(Player caster, String skillName, LivingEntity target) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);
            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);

            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            Optional<?> maybeSkill = (Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                if (plugin.getConfig().getBoolean("debug")) {
                    plugin.getLogger().warning("技能不存在: " + skillName);
                }
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

            HashSet<Object> feTargets = new HashSet<>();
            HashSet<Object> flTargets = new HashSet<>();
            feTargets.add(abstractTarget);

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                    skillTriggerClass, skillCasterClass, abstractEntityClass, abstractLocationClass,
                    HashSet.class, HashSet.class, Float.TYPE);

            Object targetLocation = bukkitAdapterClass.getMethod("adapt", Location.class)
                    .invoke(null, target.getLocation());

            Object skillMetadata = constructor.newInstance(
                    apiTrigger, fakeSkillCaster, abstractTarget, targetLocation,
                    feTargets, flTargets, 1.0F);

            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadataClass);
            executeMethod.invoke(skill, skillMetadata);

            return true;

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug")) {
                e.printStackTrace();
            }
            return false;
        }
    }

    public boolean castSkillAtLocation(Player caster, String skillName, Location location) {
        try {
            Class<?> mythicMobsClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mythicMobsInstance = mythicMobsClass.getMethod("inst").invoke(null);
            Object skillManager = mythicMobsClass.getMethod("getSkillManager").invoke(mythicMobsInstance);

            java.lang.reflect.Method getSkillMethod = skillManager.getClass().getMethod("getSkill", String.class);
            Optional<?> maybeSkill = (Optional<?>) getSkillMethod.invoke(skillManager, skillName);

            if (!maybeSkill.isPresent()) {
                return false;
            }

            Object skill = maybeSkill.get();

            Class<?> bukkitAdapterClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter");
            java.lang.reflect.Method adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);

            Object abstractLocation = adaptLocationMethod.invoke(null, location);
            Object abstractCaster = bukkitAdapterClass.getMethod("adapt", Entity.class).invoke(null, caster);

            Class<?> skillTriggerClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillTrigger");
            Object apiTrigger = skillTriggerClass.getField("API").get(null);

            Object fakeSkillCaster = createFakeSkillCaster(abstractCaster, caster, apiTrigger);

            HashSet<Object> feTargets = new HashSet<>();
            HashSet<Object> flTargets = new HashSet<>();
            flTargets.add(abstractLocation);

            Class<?> skillMetadataClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillMetadata");
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");
            Class<?> abstractLocationClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractLocation");

            java.lang.reflect.Constructor<?> constructor = skillMetadataClass.getConstructor(
                    skillTriggerClass, skillCasterClass, abstractEntityClass, abstractLocationClass,
                    HashSet.class, HashSet.class, Float.TYPE);

            Object skillMetadata = constructor.newInstance(
                    apiTrigger, fakeSkillCaster, abstractCaster, abstractLocation,
                    feTargets, flTargets, 1.0F);

            Class<?> skillClass = skill.getClass();
            java.lang.reflect.Method executeMethod = skillClass.getMethod("execute", skillMetadataClass);
            executeMethod.invoke(skill, skillMetadata);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private Object createFakeSkillCaster(Object abstractEntity, Entity realEntity, Object skillTrigger) {
        try {
            Class<?> skillCasterClass = Class.forName("io.lumine.xikage.mythicmobs.skills.SkillCaster");
            Class<?> abstractEntityClass = Class.forName("io.lumine.xikage.mythicmobs.adapters.AbstractEntity");

            return Proxy.newProxyInstance(
                    skillCasterClass.getClassLoader(),
                    new Class<?>[]{skillCasterClass},
                    (proxy, method, args) -> {
                        String methodName = method.getName();

                        if (methodName.equals("getEntity") || methodName.equals("getLivingEntity")) {
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
                        } else if (methodName.equals("getHealth") && realEntity instanceof LivingEntity) {
                            return ((LivingEntity) realEntity).getHealth();
                        } else if (methodName.equals("getMaxHealth") && realEntity instanceof LivingEntity) {
                            return ((LivingEntity) realEntity).getMaxHealth();
                        }

                        Class<?> returnType = method.getReturnType();
                        if (returnType == Boolean.TYPE) return false;
                        if (returnType == Integer.TYPE) return 0;
                        if (returnType == Double.TYPE) return 0.0;
                        if (returnType == Float.TYPE) return 0.0f;
                        return null;
                    }
            );
        } catch (Exception e) {
            return null;
        }
    }
}