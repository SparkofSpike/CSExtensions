package org.Spike.CSExtensions.Modifier.SpikeElements;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;


public class SpikeElementsManager implements Listener {
    private final CSExtensions plugin;
    private final SpikeElementsConfig config;
    private final SpikeElementsCalculator calculator;
    private final WeaponTagReader tagReader;

    public SpikeElementsManager(CSExtensions plugin) {
        this.plugin = plugin;
        this.config = new SpikeElementsConfig(plugin);
        this.calculator = new SpikeElementsCalculator(plugin);
        this.tagReader = new WeaponTagReader(plugin, plugin.getModifierManager());

        Bukkit.getPluginManager().registerEvents(this, plugin);

        config.reload();
        tagReader.reload();

        plugin.getLogger().info("SpikeElements系统已初始化");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        if (!plugin.getConfig().getBoolean("SpikeElements.Enable", true)) {
            return;
        }

        Entity victim = event.getVictim();
        if (!(victim instanceof LivingEntity)) {
            return;
        }

        if (!config.hasSpikeElements(victim)) {
            return;
        }

        String weaponId = event.getWeaponTitle();
        if (weaponId == null) {
            return;
        }

        Set<String> weaponTags = tagReader.getWeaponTags(weaponId);

        double baseDamage = event.getDamage();
        double finalDamage = calculator.calculateDamageFromEntity(
                baseDamage, weaponTags, victim, config);

        event.setDamage(finalDamage);

        if (plugin.getConfig().getBoolean("debug", false)) {
            logDamageApplication(event.getPlayer(), victim, weaponId, baseDamage, finalDamage, weaponTags);
        }
    }

    private void logDamageApplication(Player player, Entity victim, String weaponId,
                                      double baseDamage, double finalDamage, Set<String> weaponTags) {
        StringBuilder log = new StringBuilder();
        log.append("\n=== [SpikeElements] 伤害应用详情 ===\n");
        log.append(String.format("玩家: %s\n", player.getName()));
        log.append(String.format("目标: %s (%s)\n",
                victim.getType(), victim.getUniqueId()));
        log.append(String.format("武器: %s\n", weaponId));

        if (weaponTags.isEmpty()) {
            log.append("武器Tags: 无属性\n");
        } else {
            log.append(String.format("武器Tags: %s\n", String.join(", ", weaponTags)));
        }

        log.append(String.format("基础伤害: %.2f\n", baseDamage));
        log.append(String.format("最终伤害: %.2f\n", finalDamage));
        log.append(String.format("伤害倍率: %.2f\n", finalDamage / baseDamage));
        log.append("====================================\n");

        plugin.getLogger().info(log.toString());
    }

    public void reload() {
        if (config != null) {
            config.reload();
        }
        if (tagReader != null) {
            tagReader.reload();
        }
    }

    public void cleanup() {
        //todo
    }

    public WeaponTagReader getTagReader() {
        return tagReader;
    }
    public SpikeElementsConfig getConfig() {
        return config;
    }
    public SpikeElementsCalculator getCalculator() {return calculator;}
    public CSExtensions getPlugin() {return plugin;}
}