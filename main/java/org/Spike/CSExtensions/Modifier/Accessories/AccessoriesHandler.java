package org.Spike.CSExtensions.Modifier.Accessories;

import com.shampaggon.crackshot.events.WeaponPreShootEvent;
import com.shampaggon.crackshot.events.WeaponReloadEvent;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.SpikeElements.SpikeElementsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.Set;

public class AccessoriesHandler implements Listener {
    private final CSExtensions plugin;
    private final AccessoriesManager accessoriesManager;

    public AccessoriesHandler(CSExtensions plugin, AccessoriesManager accessoriesManager) {
        this.plugin = plugin;
        this.accessoriesManager = accessoriesManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponReload(WeaponReloadEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();

        SpikeElementsManager spikeElementsManager = plugin.getSpikeElementsManager();
        Set<String> weaponTags = spikeElementsManager != null
                ? spikeElementsManager.getTagReader().getWeaponTags(weaponId)
                : Collections.emptySet();

        double totalMultiplier = 1.0;

        totalMultiplier *= accessoriesManager.getReloadMultiplier(player, weaponTags);

        int originalDuration = event.getReloadDuration();
        int newDuration = (int) (originalDuration * totalMultiplier);

        if (plugin.getConfig().getBoolean("debug") && Math.abs(totalMultiplier - 1.0) > 0.001) {
            plugin.getLogger().info(String.format(
                    "[AcessoriesHandler监听] 玩家 %s 武器 %s 参数0 %.2f 参数1 %d 参数2 %d",
                    player.getName(), weaponId, totalMultiplier, originalDuration, newDuration
            ));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWeaponPreShoot(WeaponPreShootEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();

        SpikeElementsManager spikeElementsManager = plugin.getSpikeElementsManager();
        Set<String> weaponTags = spikeElementsManager != null
                ? spikeElementsManager.getTagReader().getWeaponTags(weaponId)
                : Collections.emptySet();

        double totalMultiplier = 1.0;

        totalMultiplier *= accessoriesManager.getSpreadMultiplier(player, weaponTags);

        double originalSpread = event.getBulletSpread();
        double newSpread = originalSpread * totalMultiplier;

        if (plugin.getConfig().getBoolean("debug") && Math.abs(totalMultiplier - 1.0) > 0.001) {
            plugin.getLogger().info(String.format(
                    "[饰品扩散监听器] 玩家 %s 武器 %s 扩散倍率: %.2f 扩散: %.2f ",
                    player.getName(), weaponId, totalMultiplier, originalSpread
            ));
        }
    }
}