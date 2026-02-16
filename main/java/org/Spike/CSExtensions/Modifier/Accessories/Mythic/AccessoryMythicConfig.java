package org.Spike.CSExtensions.Modifier.Accessories.Mythic;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class AccessoryMythicConfig {
    private final CSExtensions plugin;
    private final Map<String, List<AccessoryMythicEffect>> accessoryEffects = new HashMap<>();

    public AccessoryMythicConfig(CSExtensions plugin) {
        this.plugin = plugin;
    }

        public void loadConfig(ConfigurationSection accessorySection, String accessoryId) {
            if (!accessorySection.contains("mythic")) {
                plugin.getLogger().info("[饰品Mythic] 饰品 " + accessoryId + " 没有Mythic配置");
                return;
            }

            List<String> mythicConfigs = accessorySection.getStringList("mythic");
            List<AccessoryMythicEffect> effects = new ArrayList<>();
            plugin.getLogger().info("[饰品Mythic] 饰品 " + accessoryId + " 找到 " + mythicConfigs.size() + " 个技能配置");

            for (String configLine : mythicConfigs) {
                plugin.getLogger().info("[饰品Mythic] 解析: " + configLine);
                AccessoryMythicEffect effect = parseEffect(configLine);
                if (effect != null) {
                    effects.add(effect);
                    plugin.getLogger().info("[饰品Mythic] 解析成功: " + effect);
                } else {
                    plugin.getLogger().warning("[饰品Mythic] 解析失败: " + configLine);
                }
            }
            if (!effects.isEmpty()) {
                accessoryEffects.put(accessoryId, effects);
                plugin.getLogger().info("[饰品Mythic] 饰品 " + accessoryId + " 存储了 " + effects.size() + " 个效果到accessoryEffects");
            } else {
                plugin.getLogger().warning("[饰品Mythic] 饰品 " + accessoryId + " 没有解析出任何有效效果");
            }
        }

    private AccessoryMythicEffect parseEffect(String configLine) {

        String[] parts = configLine.trim().split("\\s+", 4);
        if (parts.length < 3) {
            plugin.getLogger().warning("饰品Mythic配置格式错误: " + configLine);
            return null;
        }

        String skillName = parts[0];
        String selector = parts[1];
        String triggerStr = parts[2];
        String condition = parts.length > 3 ? parts[3] : null;

        if (!selector.equals("@shooter") &&!selector.equals("@self") && !selector.equals("@victim") && !selector.equals("@trigger")) {
            plugin.getLogger().warning("无效的选择器: " + selector + " in " + configLine);
            return null;
        }

        AccessoryMythicTrigger trigger = AccessoryMythicTrigger.fromString(triggerStr);
        if (trigger == null) {
            plugin.getLogger().warning("无效的触发类型: " + triggerStr + " in " + configLine);
            return null;
        }

        ConditionParser parser = null;
        if (condition != null && !condition.trim().isEmpty()) {
            parser = new ConditionParser(condition);
        }

        AccessoryMythicEffect effect = new AccessoryMythicEffect(plugin);
        effect.setSkillName(skillName);
        effect.setTargetSelector(selector);
        effect.setTrigger(trigger);
        effect.setCondition(condition);
        effect.setParser(parser);

        if (trigger == AccessoryMythicTrigger.ON_TIMER) {
            int ticks = AccessoryMythicTrigger.extractTimerTicks(triggerStr);
            effect.setTimerTicks(ticks > 0 ? ticks : 20);
        }

        return effect;
    }

    private AccessoryMythicTrigger parseTrigger(String triggerStr) {
        if (triggerStr.startsWith("~onTimer:")) {
            return AccessoryMythicTrigger.ON_TIMER;
        } else if (triggerStr.equals("~onDamaged")) {
            return AccessoryMythicTrigger.ON_DAMAGED;
        } else if (triggerStr.equals("~onAttack")) {
            return AccessoryMythicTrigger.ON_ATTACK;
        } else if (triggerStr.equals("~onShoot")) {
            return AccessoryMythicTrigger.ON_SHOOT;
        } else if (triggerStr.equals("~onCrit")) {
            return AccessoryMythicTrigger.ON_CRIT;
        } else if (triggerStr.equals("~onHeadshot")) {
            return AccessoryMythicTrigger.ON_HEADSHOT;
        } else if (triggerStr.equals("~onReload")) {
            return AccessoryMythicTrigger.ON_RELOAD;
        } else if (triggerStr.equals("~onHitblock")) {
            return AccessoryMythicTrigger.ON_HITBLOCK;
        }
        return null;
    }

    private int extractTimerTicks(String triggerStr) {
        try {
            String[] parts = triggerStr.split(":");
            if (parts.length == 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {}
        return 20;
    }

    public List<AccessoryMythicEffect> getEffects(String accessoryId, AccessoryMythicTrigger trigger) {
        List<AccessoryMythicEffect> allEffects = accessoryEffects.get(accessoryId);
        if (allEffects == null) return Collections.emptyList();

        List<AccessoryMythicEffect> result = new ArrayList<>();
        for (AccessoryMythicEffect effect : allEffects) {
            if (effect.getTrigger() == trigger) {
                result.add(effect);
            }
        }
        return result;
    }

    public void clear() {
        int size = accessoryEffects.size();
        accessoryEffects.clear();
        plugin.getLogger().info("[饰品Mythic配置] 清空了 " + size + " 个饰品的Mythic配置");
    }
}