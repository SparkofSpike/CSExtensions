package org.Spike.CSExtensions.Modifier.Mythic.accessory;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponHitBlockEvent;
import com.shampaggon.crackshot.events.WeaponReloadCompleteEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Accessories.AccessoriesManager;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicEffect;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicSkillExecutor;
import org.Spike.CSExtensions.Modifier.Mythic.core.MythicTrigger;

import java.util.*;

public class AccessoryMythicHandler implements Listener {
    private final CSExtensions plugin;
    private final AccessoriesManager accessoriesManager;
    private final AccessoryMythicConfig config;
    private final MythicSkillExecutor executor;

    private final Map<UUID, Map<String, Integer>> playerTimers = new HashMap<>();

    private static class HitLocationTarget {
        final Location location;
        HitLocationTarget(Location location) { this.location = location; }
    }

    public AccessoryMythicHandler(CSExtensions plugin, AccessoriesManager accessoriesManager, AccessoryMythicConfig config) {
        this.plugin = plugin;
        this.accessoriesManager = accessoriesManager;
        this.config = config;
        this.executor = new MythicSkillExecutor(plugin);
        startTimerScheduler();
    }

    private void startTimerScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    handleTimerTrigger(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void handleTimerTrigger(Player player) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;

        Map<String, Integer> timers = playerTimers.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());

        for (String accessoryId : accessories) {
            Integer ticksLeft = timers.get(accessoryId);
            if (ticksLeft == null) {
                List<MythicEffect> effects = config.getEffects(accessoryId, MythicTrigger.TIMER);
                if (!effects.isEmpty()) {
                    timers.put(accessoryId, effects.get(0).getTimerTicks());
                }
                continue;
            }

            if (ticksLeft <= 0) {
                triggerTimerEffects(player, accessoryId);
                List<MythicEffect> effects = config.getEffects(accessoryId, MythicTrigger.TIMER);
                if (!effects.isEmpty()) {
                    timers.put(accessoryId, effects.get(0).getTimerTicks());
                }
            } else {
                timers.put(accessoryId, ticksLeft - 1);
            }
        }
    }

    private void triggerTimerEffects(Player player, String accessoryId) {
        List<MythicEffect> effects = config.getEffects(accessoryId, MythicTrigger.TIMER);
        Set<String> weaponTags = getCurrentWeaponTags(player);

        for (MythicEffect effect : effects) {
            if (effect.shouldTrigger(Math.random()) &&
                    effect.checkConditions(player, null, weaponTags)) {
                executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                        player.getLocation(), null, null);
            }
        }
    }

    @EventHandler
    public void onWeaponShoot(WeaponShootEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();
        Set<String> weaponTags = getWeaponTags(weaponId);
        triggerAccessoryEffects(player, MythicTrigger.SHOOT, null, null, weaponTags, null);
    }

    @EventHandler
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        Player player = event.getPlayer();
        String weaponId = event.getWeaponTitle();
        LivingEntity victim = (LivingEntity) event.getVictim();
        Set<String> weaponTags = getWeaponTags(weaponId);

        triggerAccessoryEffects(player, MythicTrigger.HIT, player, victim, weaponTags, null);

        if (event.isCritical()) {
            triggerAccessoryEffects(player, MythicTrigger.CRIT, player, victim, weaponTags, null);
        }

        if (event.isHeadshot()) {
            triggerAccessoryEffects(player, MythicTrigger.HEADSHOT, player, victim, weaponTags, null);
        }

        if (victim.getHealth() - event.getDamage() <= 0) {
            triggerAccessoryEffects(player, MythicTrigger.KILL, player, victim, weaponTags, null);
        }

        handleHitLocationTrigger(player, weaponTags, victim);
    }

    @EventHandler
    public void onWeaponHitBlock(WeaponHitBlockEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Set<String> weaponTags = getCurrentWeaponTags(player);

        if (block != null) {
            Location location = block.getLocation().add(0.5, 0.5, 0.5);
            HitLocationTarget target = new HitLocationTarget(location);
            triggerAccessoryEffects(player, MythicTrigger.HITBLOCK, null, null, weaponTags, target);
            handleHitLocationFromBlock(player, weaponTags, target);
        }
    }

    @EventHandler
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        Player player = event.getPlayer();
        Set<String> weaponTags = getCurrentWeaponTags(player);
        triggerAccessoryEffects(player, MythicTrigger.RELOAD, null, null, weaponTags, null);
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        LivingEntity attacker = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent ee = (EntityDamageByEntityEvent) event;
            if (ee.getDamager() instanceof LivingEntity) {
                attacker = (LivingEntity) ee.getDamager();
            }
        }

        Set<String> weaponTags = getCurrentWeaponTags(player);
        triggerAccessoryEffects(player, MythicTrigger.DAMAGED, attacker, null, weaponTags, null);
    }

    private void triggerAccessoryEffects(Player player, MythicTrigger trigger,
                                         LivingEntity triggerEntity, LivingEntity victim,
                                         Set<String> weaponTags, HitLocationTarget locationTarget) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;

        for (String accessoryId : accessories) {
            List<MythicEffect> effects = config.getEffects(accessoryId, trigger);
            if (effects.isEmpty()) continue;

            for (MythicEffect effect : effects) {
                if (!effect.shouldTrigger(Math.random())) continue;
                if (!effect.checkConditions(player, victim, weaponTags)) continue;

                Location loc = locationTarget != null ? locationTarget.location :
                        (victim != null ? victim.getLocation() :
                                (triggerEntity != null ? triggerEntity.getLocation() : player.getLocation()));

                executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                        loc, victim, triggerEntity);
            }
        }
    }

    private void handleHitLocationTrigger(Player player, Set<String> weaponTags, LivingEntity victim) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;

        HitLocationTarget target = new HitLocationTarget(victim.getLocation());

        for (String accessoryId : accessories) {
            List<MythicEffect> effects = config.getEffects(accessoryId, MythicTrigger.HIT);
            if (effects.isEmpty()) continue;

            for (MythicEffect effect : effects) {
                if (!"@hitlocation".equals(effect.getTargetSelector())) continue;
                if (!effect.shouldTrigger(Math.random())) continue;
                if (!effect.checkConditions(player, victim, weaponTags)) continue;

                executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                        target.location, victim, player);
            }
        }
    }

    private void handleHitLocationFromBlock(Player player, Set<String> weaponTags, HitLocationTarget target) {
        Set<String> accessories = accessoriesManager.getEquippedAccessoryIds(player);
        if (accessories.isEmpty()) return;

        for (String accessoryId : accessories) {
            List<MythicEffect> effects = config.getEffects(accessoryId, MythicTrigger.HIT);
            if (effects.isEmpty()) continue;

            for (MythicEffect effect : effects) {
                if (!"@hitlocation".equals(effect.getTargetSelector())) continue;
                if (!effect.shouldTrigger(Math.random())) continue;
                if (!effect.checkConditions(player, null, weaponTags)) continue;

                executor.executeSkill(player, effect.getSkillName(), effect.getTargetSelector(),
                        target.location, null, player);
            }
        }
    }

    private Set<String> getWeaponTags(String weaponId) {
        if (plugin.getSpikeElementsManager() != null) {
            return plugin.getSpikeElementsManager().getTagReader().getWeaponTags(weaponId);
        }
        return Collections.emptySet();
    }

    private Set<String> getCurrentWeaponTags(Player player) {
        String weaponId = plugin.getCSUtility().getWeaponTitle(player.getItemInHand());
        if (weaponId != null) {
            return getWeaponTags(weaponId);
        }
        return Collections.emptySet();
    }

    public void reload() {
        config.clear();
        playerTimers.clear();
    }

    public void cleanup() {
        playerTimers.clear();
    }

    public void cleanupPlayer(Player player) {
        playerTimers.remove(player.getUniqueId());
    }
}