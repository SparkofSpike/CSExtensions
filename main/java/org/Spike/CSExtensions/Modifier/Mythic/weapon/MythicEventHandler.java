package org.Spike.CSExtensions.Modifier.Mythic.weapon;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.*;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Mythic.MythicDropListener;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicEffect;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicSkillExecutor;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicTrigger;
import org.Spike.CSExtensions.Modifier.Services.HitLocationTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
    private final WeaponMythicConfig configManager;
    private final CSUtility csUtility;
    private final MythicDropListener dropListener;
    private final MythicSkillExecutor executor;

    private final Map<UUID, String> activeWeapons = new HashMap<>();
    private final Map<UUID, Map<String, List<Integer>>> playerWeaponTimers = new HashMap<>();
    private final Map<Integer, String> taskIdToWeaponMap = new HashMap<>();

    public MythicEventHandler(CSExtensions plugin, WeaponMythicConfig configManager, CSUtility csUtility) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.csUtility = csUtility;
        this.executor = new MythicSkillExecutor(plugin);

        this.dropListener = new MythicDropListener(plugin);
        plugin.getServer().getPluginManager().registerEvents(dropListener, plugin);

        if (Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) {
            plugin.getLogger().info("CrackShot掉落监听器已注册");
        } else {
            plugin.getLogger().warning("MythicMobs未启用，CrackShot掉落支持已禁用");
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("MythicEventHandler构造函数已加载");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponShoot(WeaponShootEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, MythicTrigger.SHOOT)) {
            return;
        }

        handleTrigger(event.getPlayer(), weaponTitle, MythicTrigger.SHOOT, null, null, null,null);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle)) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || !(event.getVictim() instanceof LivingEntity)) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getVictim();
        Set<String> weaponTags = getWeaponTags(weaponTitle);

        handleHitLocationForHitTrigger(player, weaponTitle, victim, weaponTags);

        handleHitLocationForHitBlockTrigger(player, weaponTitle, victim, weaponTags);

        if (configManager.hasTriggerType(weaponTitle, MythicTrigger.HIT)) {
            handleTrigger(player, weaponTitle, MythicTrigger.HIT, victim, player, weaponTags, null);
        }

        if (event.isCritical() && configManager.hasTriggerType(weaponTitle, MythicTrigger.CRIT)) {
            handleTrigger(player, weaponTitle, MythicTrigger.CRIT, victim, player, weaponTags, null);
        }

        if (event.isHeadshot() && configManager.hasTriggerType(weaponTitle, MythicTrigger.HEADSHOT)) {
            handleTrigger(player, weaponTitle, MythicTrigger.HEADSHOT, victim, player, weaponTags, null);
        }

        if (configManager.hasTriggerType(weaponTitle, MythicTrigger.KILL) &&
                victim.getHealth() - event.getDamage() <= 0) {
            handleTrigger(player, weaponTitle, MythicTrigger.KILL, victim, player, weaponTags, null);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponHitBlock(WeaponHitBlockEvent event) {
        Player player = event.getPlayer();
        String weaponTitle = event.getWeaponTitle();

        if (player == null) return;

        Block hitBlock = event.getBlock();
        if (hitBlock == null) return;

        Location hitLocation = hitBlock.getLocation().add(0.5, 0.5, 0.5);
        Set<String> weaponTags = getWeaponTags(weaponTitle);

        handleHitBlockTriggerWithLocation(player, weaponTitle, hitLocation, weaponTags);

        handleHitLocationForHitTriggerFromBlock(player, weaponTitle, hitLocation, weaponTags);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Mythic] 方块命中触发: " + weaponTitle + " 位置: " + hitLocation);
        }
    }

    private void handleHitBlockTriggerWithLocation(Player player, String weaponTitle,
                                                   Location hitLocation, Set<String> weaponTags) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, MythicTrigger.HITBLOCK);
        if (effects.isEmpty()) return;

        for (MythicEffect effect : effects) {
            if (!effect.shouldTrigger(Math.random())) continue;
            if (!effect.checkConditions(player, null, weaponTags)) continue;

            executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                    hitLocation, null, player);
        }
    }

    private void handleHitLocationForHitTrigger(Player player, String weaponTitle,
                                                LivingEntity victim, Set<String> weaponTags) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, MythicTrigger.HIT);
        if (effects.isEmpty()) return;

        for (MythicEffect effect : effects) {
            if (!"@hitlocation".equals(effect.getTargetSelector())) continue;
            if (!effect.shouldTrigger(Math.random())) continue;
            if (!effect.checkConditions(player, victim, weaponTags)) continue;

            executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                    victim.getLocation(), victim, player);
        }
    }

    private void handleHitLocationForHitTriggerFromBlock(Player player, String weaponTitle,
                                                         Location hitLocation, Set<String> weaponTags) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, MythicTrigger.HIT);
        if (effects.isEmpty()) return;

        for (MythicEffect effect : effects) {
            if (!"@hitlocation".equals(effect.getTargetSelector())) continue;
            if (!effect.shouldTrigger(Math.random())) continue;
            if (!effect.checkConditions(player, null, weaponTags)) continue;

            executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                    hitLocation, null, player);
        }
    }

    private void handleHitLocationForHitBlockTrigger(Player player, String weaponTitle,
                                                     LivingEntity victim, Set<String> weaponTags) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, MythicTrigger.HITBLOCK);
        if (effects.isEmpty()) return;

        for (MythicEffect effect : effects) {
            if (!"@hitlocation".equals(effect.getTargetSelector())) continue;
            if (!effect.shouldTrigger(Math.random())) continue;
            if (!effect.checkConditions(player, victim, weaponTags)) continue;

            executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                    victim.getLocation(), victim, player);
        }
    }

    private void handleTrigger(Player player, String weaponTitle, MythicTrigger trigger,
                               LivingEntity target, LivingEntity triggerEntity,
                               Set<String> weaponTags, Location forcedLocation) {
        List<MythicEffect> effects = configManager.getEffects(weaponTitle, trigger);
        if (effects.isEmpty()) return;

        for (MythicEffect effect : effects) {
            if ("@hitlocation".equals(effect.getTargetSelector())) continue;

            if (!effect.shouldTrigger(Math.random())) continue;
            if (!effect.checkConditions(player, target, weaponTags)) continue;

            Location location = forcedLocation;
            if (location == null) {
                location = target != null ? target.getLocation() : player.getLocation();
            }

            executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                    location, target, triggerEntity);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        String weaponTitle = event.getWeaponTitle();

        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, MythicTrigger.RELOAD)) {
            return;
        }

        handleTrigger(event.getPlayer(), weaponTitle, MythicTrigger.RELOAD, null, null, null,null);
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
                activeWeapons.put(playerId, weaponTitle);
                startWeaponTimers(player, weaponTitle);
            } else {
                activeWeapons.remove(playerId);
            }
        } else {
            activeWeapons.remove(playerId);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        stopPlayerTimers(playerId);
        activeWeapons.remove(playerId);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID playerId = event.getEntity().getUniqueId();
        stopPlayerTimers(playerId);
        activeWeapons.remove(playerId);
    }

    private void startWeaponTimers(Player player, String weaponTitle) {
        if (!configManager.hasMythicConfig(weaponTitle) ||
                !configManager.hasTriggerType(weaponTitle, MythicTrigger.TIMER)) {
            return;
        }

        List<MythicEffect> timerEffects = configManager.getEffects(weaponTitle, MythicTrigger.TIMER);
        if (timerEffects.isEmpty()) return;

        UUID playerId = player.getUniqueId();
        Map<String, List<Integer>> timers = playerWeaponTimers.computeIfAbsent(playerId, k -> new HashMap<>());

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

                if (!effect.shouldTrigger(Math.random())) return;
                if (!effect.checkHealthCondition(currentPlayer)) return;

                Set<String> weaponTags = getWeaponTags(finalWeaponTitle);
                executor.executeSkill(currentPlayer, effect.getSkillName(), effect.getTargetSelector(),
                        currentPlayer.getLocation(), null, currentPlayer);

            }, timerTicks, timerTicks);

            timers.computeIfAbsent(weaponTitle, k -> new ArrayList<>()).add(taskId);
            taskIdToWeaponMap.put(taskId, weaponTitle + ":" + playerId);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic] 启动定时器: 玩家=" + player.getName() +
                        ", 武器=" + weaponTitle + ", 间隔=" + timerTicks + "tick");
            }
        }
    }

    private void stopPlayerTimers(UUID playerId) {
        Map<String, List<Integer>> timers = playerWeaponTimers.remove(playerId);
        if (timers == null) return;

        for (List<Integer> taskIds : timers.values()) {
            for (int taskId : taskIds) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskIdToWeaponMap.remove(taskId);
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Mythic] 停止玩家所有定时器: " + playerId);
        }
    }

    private void stopWeaponTimer(UUID playerId, String weaponTitle) {
        Map<String, List<Integer>> timers = playerWeaponTimers.get(playerId);
        if (timers == null) return;

        List<Integer> taskIds = timers.remove(weaponTitle);
        if (taskIds != null) {
            for (int taskId : taskIds) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskIdToWeaponMap.remove(taskId);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Mythic] 停止武器定时器: 玩家=" + playerId + ", 武器=" + weaponTitle);
            }
        }
    }

    private Set<String> getWeaponTags(String weaponId) {
        if (plugin.getSpikeElementsManager() != null) {
            return plugin.getSpikeElementsManager().getTagReader().getWeaponTags(weaponId);
        }
        return Collections.emptySet();
    }

    private void stopAllTimers() {
        for (Map<String, List<Integer>> timers : playerWeaponTimers.values()) {
            for (List<Integer> taskIds : timers.values()) {
                for (int taskId : taskIds) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }
        playerWeaponTimers.clear();
        taskIdToWeaponMap.clear();
    }

    public void reload() {
        configManager.reload();
        stopAllTimers();
        activeWeapons.clear();
    }

    public void cleanup() {
        stopAllTimers();

        if (dropListener != null) {
            HandlerList.unregisterAll(dropListener);
        }
    }
}