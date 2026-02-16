package org.Spike.CSExtensions;

import com.shampaggon.crackshot.CSUtility;
import org.Spike.CSExtensions.Modifier.Accessories.AccessoriesData;
import org.Spike.CSExtensions.Modifier.Accessories.AccessoriesHandler;
import org.Spike.CSExtensions.Modifier.Accessories.AccessoriesManager;
import org.Spike.CSExtensions.Modifier.HealthAdjust.HealthAdjustHandler;
import org.Spike.CSExtensions.Modifier.Mythic.MythicConfigManager;
import org.Spike.CSExtensions.Modifier.Mythic.MythicEventHandler;
import org.Spike.CSExtensions.Modifier.Projectiles.*;
import org.Spike.CSExtensions.Modifier.ReloadBar.ReloadBarManager;
import org.Spike.CSExtensions.Modifier.Services.ProjectileEffectCoordinator;
import org.Spike.CSExtensions.Modifier.SpikeElements.SpikeElementsManager;
import org.Spike.CSExtensions.Modifier.Trails.TrailConfig;
import org.Spike.CSExtensions.Modifier.Trails.TrailConfigManager;
import org.Spike.CSExtensions.Modifier.Trails.TrailEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import com.shampaggon.crackshot.events.WeaponReloadEvent;
import com.shampaggon.crackshot.events.WeaponPreShootEvent;
import com.shampaggon.crackshot.events.WeaponReloadCompleteEvent;
import com.shampaggon.crackshot.events.WeaponShootEvent;

import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.Spike.CSExtensions.Modifier.Trails.TrailsController;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CSExtensions extends JavaPlugin implements Listener {
    private CSUtility csUtility;

    private File configFile;
    private FileConfiguration config;
    private ReloadBarManager reloadBarManager;

    private ModifierManager modifierManager;

    private ProjectileEffectCoordinator projectileEffectCoordinator;

    private TrailsController trailsController;
    private TrailEffectManager trailEffectManager;
    private TrailConfigManager trailConfigManager;

    private ProjectilesManager projectilesManager;
    private ProjectilesController projectilesController;
    private ProjectileTracker projectileTracker;
    private ProjectileEventHandler projectileEventHandler;

    private HealthAdjustHandler healthAdjustHandler;

    private MythicConfigManager mythicConfigManager;
    private MythicEventHandler mythicEventHandler;

    private SpikeElementsManager spikeElementsManager;

    private AccessoriesManager accessoriesManager;
    private AccessoriesHandler accessoriesHandler;


    @Override
    public void onEnable() {
        csUtility = new CSUtility();
        createConfig();

        reloadBarManager = new ReloadBarManager(this);

        modifierManager = new ModifierManager(this);
        projectileEffectCoordinator = new ProjectileEffectCoordinator(this);

        if (getConfig().getBoolean("Trails.Enable", true)) {
            trailsController = new TrailsController(this, modifierManager);
            trailConfigManager = new TrailConfigManager(this, modifierManager);
            trailEffectManager = new TrailEffectManager(this, trailConfigManager);
            getServer().getPluginManager().registerEvents(trailsController, this);
            getLogger().info("Trails系统已启用");
        } else {
            getLogger().info("Trails系统未启用，请在config.yml中设置 Trails.Enable: true");
        }

        if (getConfig().getBoolean("Projectiles.Enable", true)) {

            projectilesManager = new ProjectilesManager(this, modifierManager);
            projectilesController = new ProjectilesController(this, projectilesManager);
            getServer().getPluginManager().registerEvents(projectilesController, this);

            projectileTracker = projectilesController.getProjectileTracker();
            projectileEventHandler = new ProjectileEventHandler(this, projectileTracker, projectilesManager, projectileEffectCoordinator);

            getLogger().info("Projectiles系统已启用");
            getLogger().info("抛射物特性: 隐藏/穿透/返回/弹跳/制导");
        } else {
            getLogger().info("Projectiles系统未启用，请在config.yml中设置 Projectiles.Enable: true");
        }

        if (getConfig().getBoolean("Mythic.Enable", true) && Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            mythicConfigManager = new MythicConfigManager(this, modifierManager);
            mythicEventHandler = new MythicEventHandler(this, mythicConfigManager, csUtility);
            getLogger().info("Mythic系统已启用");
        } else if (getConfig().getBoolean("Mythic.Enable", true)) {
            getLogger().warning("Mythic系统未启用：需要MythicMobs插件");
        }

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("cse").setExecutor(this);

        this.healthAdjustHandler = new HealthAdjustHandler(this, projectilesManager);

        if (getConfig().getBoolean("SpikeElements.Enable", true)) {
            spikeElementsManager = new SpikeElementsManager(this);
            getLogger().info("SpikeElements系统已启用");
        }
        accessoriesManager = new AccessoriesManager(this);
        accessoriesHandler = new AccessoriesHandler(this, accessoriesManager);
        Bukkit.getPluginManager().registerEvents(accessoriesHandler, this);

        getLogger().info("CSExtensions 已开启!");
        getLogger().info("Template系统已加载 " + modifierManager.getLoadedWeaponIds().size() + " 个武器配置");
    }

    @Override
    public void onDisable() {
        if (reloadBarManager != null) {
            reloadBarManager.cleanup();
        }
        if (trailsController != null) {
            trailsController.cleanup();
        }
        if (projectilesController != null) {
            projectilesController.cleanup();
        }
        if (projectileEffectCoordinator != null) {
            projectileEffectCoordinator.cleanup();
        }
        if (healthAdjustHandler != null) {
            healthAdjustHandler.cleanup();
        }
        if (mythicEventHandler != null) {
            mythicEventHandler.cleanup();
        }
        if (spikeElementsManager != null) {
            spikeElementsManager.cleanup();
        }
        if (accessoriesManager != null) {
            accessoriesManager.cleanup();
        }

        getLogger().info("CSExtensions 已关闭!");
    }

    private void createConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Could not save config to " + configFile);
            e.printStackTrace();
        }
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void reloadPluginConfig() {
        reloadConfig();

        if (accessoriesManager != null) {
            accessoriesManager.reload();
        }
        if (reloadBarManager != null) {
            reloadBarManager.reloadConfig();
        }

        if (modifierManager != null) {
            modifierManager.reload();
        }

        if (trailsController != null) {
            trailsController.reload();
        }

        if (projectilesController != null) {
            projectilesController.reload();
        }

        if (healthAdjustHandler != null) {
            healthAdjustHandler.reload();
        }

        if (mythicEventHandler != null) {
            mythicEventHandler.reload();
        }

        if (spikeElementsManager != null) {
            spikeElementsManager.reload();
        }

        getLogger().info("配置重载成功!");

        if (getConfig().getBoolean("debug")) {
            getLogger().info("调试模式已开启");
            getLogger().info("装弹条功能: " + getConfig().getBoolean("CSEReloadBar.Enable", true));
            getLogger().info("Trails功能: " + getConfig().getBoolean("Trails.Enable", true));

            if (modifierManager != null) {
                List<String> weaponIds = modifierManager.getLoadedWeaponIds();
                getLogger().info("已加载 " + weaponIds.size() + " 个武器配置");
                for (String weaponId : weaponIds) {
                    getLogger().info("  - 武器: " + weaponId);
                }
            }

            if (trailsController != null && trailsController.getConfigManager() != null) {
                TrailConfigManager trailManager = trailsController.getConfigManager();
                List<String> trailsWeapons = trailManager.getLoadedWeaponIds();
                getLogger().info("已加载 " + trailsWeapons.size() + " 个Trails配置");
            }

            if (healthAdjustHandler != null) {
                getLogger().info("HealthAdjust系统已重载");
            }

            if (mythicEventHandler != null) {
                getLogger().info("Mythic系统已重载");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWeaponDamage(WeaponDamageEntityEvent event) {
        Player player = event.getPlayer();
        double damageMultiplier = 1.0;
        StringBuilder effectInfo = new StringBuilder();
        boolean hasEffect = false;

        if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
            int strengthLevel = getPotionEffectLevel(player, PotionEffectType.INCREASE_DAMAGE);
            double strengthMultiplier = getConfig().getDouble("CSPotionEffect.strength-damage-multiplier." + strengthLevel);
            double strengthBonus = strengthMultiplier - 1.0;
            damageMultiplier += strengthBonus;

            if (effectInfo.length() > 0) {
                effectInfo.append(" + ");
            }
            effectInfo.append("力量 ").append(strengthLevel).append("(x").append(String.format("%.1f", strengthMultiplier)).append(")");
            hasEffect = true;
        }

        if (player.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            int weaknessLevel = getPotionEffectLevel(player, PotionEffectType.WEAKNESS);
            double weaknessMultiplier = getConfig().getDouble("CSPotionEffect.weakness-damage-multiplier." + weaknessLevel);
            double weaknessPenalty = weaknessMultiplier - 1.0;
            damageMultiplier += weaknessPenalty;

            if (effectInfo.length() > 0) {
                effectInfo.append(" + ");
            }
            effectInfo.append("虚弱 ").append(weaknessLevel).append("(x").append(String.format("%.1f", weaknessMultiplier)).append(")");
            hasEffect = true;
        }

        if (accessoriesManager != null) {
            String weaponId = event.getWeaponTitle();
            Set<String> weaponTags = Collections.emptySet();

            if (spikeElementsManager != null) {
                weaponTags = spikeElementsManager.getTagReader().getWeaponTags(weaponId);
            }

            double accessoryDamageMultiplier = accessoriesManager.getDamageMultiplier(player, weaponTags);

            if (Math.abs(accessoryDamageMultiplier - 1.0) > 0.001) {
                damageMultiplier *= accessoryDamageMultiplier;
                hasEffect = true;

                if (getConfig().getBoolean("debug")) {
                    getLogger().info(String.format(
                            "玩家 %s 饰品攻击加成: x%.2f (武器: %s, 元素: %s)",
                            player.getName(), accessoryDamageMultiplier, weaponId,
                            weaponTags.isEmpty() ? "无属性" : String.join(",", weaponTags)
                    ));
                }
            }
        }


        Entity damager = event.getDamager();
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileTracker tracker = this.projectilesController.getProjectileTracker();
            ProjectileData data = tracker.getProjectileData(projectile.getEntityId());

            if (getConfig().getBoolean("debug")) {
                getLogger().info("=== [DEBUG] 开始处理伤害事件 ===");
                getLogger().info("使用的tracker: " + projectileTracker);
                getLogger().info("projectilesController: " + projectilesController);

                if (projectilesController != null) {
                    getLogger().info("controller的tracker: " + projectilesController.getProjectileTracker());
                }
            }

            if (data == null && getConfig().getBoolean("debug")) {
                getLogger().warning("数据为空，检查所有tracker实例...");

                if (projectilesController != null) {
                    ProjectileData data2 = projectilesController.getProjectileTracker().getProjectileData(projectile.getEntityId());
                    getLogger().info("通过controller获取数据: " + (data2 != null ? "成功" : "失败"));
                }
            }


            if (data != null) {
                double projectileMultiplier = data.getProjectileMultiplierForMain();

                if (getConfig().getBoolean("debug")) {
                    getLogger().info("=== [DEBUG] 主类处理抛射物伤害 ===");
                    getLogger().info("抛射物ID: " + projectile.getEntityId());
                    getLogger().info("武器: " + event.getWeaponTitle());
                    getLogger().info("获取到的伤害系数: " + projectileMultiplier);
                    getLogger().info("当前伤害: " + event.getDamage());
                }

                if (projectileMultiplier != 1.0) {
                    hasEffect = true;
                    damageMultiplier *= projectileMultiplier;

                    if (getConfig().getBoolean("debug")) {
                        getLogger().info("应用系数后总倍率: " + damageMultiplier);
                    }
                }

                tracker.markForRemoval(projectile.getEntityId());
            } else {
                if (getConfig().getBoolean("debug")) {
                    getLogger().warning("抛射物数据为空! ID: " + projectile.getEntityId());
                }
            }
        }

        if (hasEffect && damageMultiplier != 1.0) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * damageMultiplier;

            if (newDamage < 0) {
                newDamage = 0;
            }

            event.setDamage(newDamage);

            if (getConfig().getBoolean("debug")) {
                getLogger().info(String.format("玩家 %s 效果: %s | 总倍率: x%.2f | 伤害从 %.1f 变为 %.1f", player.getName(), effectInfo.toString(), damageMultiplier, originalDamage, newDamage));
            }
        }
    }

    @EventHandler
    public void onWeaponReload(WeaponReloadEvent event) {
        if (!getConfig().getBoolean("CSEReloadBar.Enable", true)) {
            return;
        }
        if (getConfig().getBoolean("debug")) {
            getLogger().info("[主类] 进入事件，原始时间: " + event.getReloadDuration());
        }

        Player player = event.getPlayer();

        reloadBarManager.startReload(event);

        if (accessoriesManager != null) {
            String weaponId = event.getWeaponTitle();
            Set<String> weaponTags = Collections.emptySet();

            if (spikeElementsManager != null) {
                weaponTags = spikeElementsManager.getTagReader().getWeaponTags(weaponId);
            }
            double reloadSpeedMultiplier = 1.0;
            reloadSpeedMultiplier *= accessoriesManager.getReloadMultiplier(player, weaponTags);

            if (reloadSpeedMultiplier != 1.0) {
                int originalDuration = event.getReloadDuration();
                int newDuration = (int) (originalDuration * reloadSpeedMultiplier);
                event.setReloadDuration(newDuration);

                if (getConfig().getBoolean("debug")) {
                    getLogger().info(String.format("[主类监听器] 玩家 %s 装弹时间从 %dtick 变为 %dtick (饰品倍率: " +
                                    "x%.2f)",
                            player.getName(), originalDuration, newDuration, reloadSpeedMultiplier));
                }
            }
        }
        if (getConfig().getBoolean("debug")) {
            getLogger().info("[主类] 事件结束，最终时间: " + event.getReloadDuration());
        }
    }

    @EventHandler
    public void onWeaponReloadComplete(WeaponReloadCompleteEvent event) {
        if (!getConfig().getBoolean("CSEReloadBar.Enable", true)) {
            return;
        }

        Player player = event.getPlayer();
        reloadBarManager.completeReload(player);
    }

    @EventHandler
    public void onWeaponPreShoot(WeaponPreShootEvent event) {
        Player player = event.getPlayer();

        if (accessoriesManager != null) {
            String weaponId = event.getWeaponTitle();
            Set<String> weaponTags = Collections.emptySet();

            if (spikeElementsManager != null) {
                weaponTags = spikeElementsManager.getTagReader().getWeaponTags(weaponId);
            }

            double spreadMultiplier = 1.0;
            spreadMultiplier *= accessoriesManager.getSpreadMultiplier(player, weaponTags);

            if (spreadMultiplier != 1.0) {
                double originalSpread = event.getBulletSpread();
                double newSpread = originalSpread * spreadMultiplier;
                event.setBulletSpread(newSpread);

                if (config.getBoolean("debug", false)) {
                    getLogger().info(String.format("玩家 %s 子弹扩散从 %.2f 变为 %.2f (饰品倍率: x%.2f)",
                            player.getName(), originalSpread, newSpread, spreadMultiplier));
                }
            }
        }
    }

    @EventHandler
    public void onWeaponShoot(WeaponShootEvent event) {
        if (!getConfig().getBoolean("CSEReloadBar.Enable", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (reloadBarManager.isReloading(player)) {
            reloadBarManager.failReload(player);

            if (getConfig().getBoolean("debug")) {
                getLogger().info("玩家 " + player.getName() + " 射击中断装弹");
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (!getConfig().getBoolean("CSEReloadBar.Enable", true)) {
            return;
        }

        Player player = event.getPlayer();

        if (reloadBarManager.isReloading(player)) {
            reloadBarManager.failReload(player);

            if (getConfig().getBoolean("debug")) {
                getLogger().info("玩家 " + player.getName() + " 切换武器，取消装弹");
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!getConfig().getBoolean("CSEReloadBar.Enable", true)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        if (reloadBarManager.isReloading(player)) {
            reloadBarManager.failReload(player);

            if (getConfig().getBoolean("debug")) {
                getLogger().info("玩家 " + player.getName() + " 打开背包，取消装弹");
            }
        }
    }

    private int getPotionEffectLevel(Player player, PotionEffectType effectType) {
        Collection<org.bukkit.potion.PotionEffect> effects = player.getActivePotionEffects();
        for (org.bukkit.potion.PotionEffect effect : effects) {
            if (effect.getType().equals(effectType)) {
                return effect.getAmplifier() + 1;
            }
        }
        return 0;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cse")) {
            if (args.length == 0) {
                sender.sendMessage("§6[CSExtensions] 可用命令:");
                sender.sendMessage("§e/cse reload §7- 重载配置");
                sender.sendMessage("§e/cse give <饰品ID> [玩家] [数量] §7- 获取饰品");
                sender.sendMessage("§e/cse list §7- 查看所有饰品ID");
                sender.sendMessage("§e/cse debug §7- 切换调试模式");
                sender.sendMessage("§e/cse trails <武器ID> §7- 查看武器的Trails配置");
                sender.sendMessage("§e/cse weapons §7- 查看所有配置的武器");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    if (sender.hasPermission("csextensions.reload") || sender.isOp()) {
                        reloadPluginConfig();
                        sender.sendMessage("§a[CSExtensions] 配置重载成功！");
                    } else {
                        sender.sendMessage("§c你没有权限使用此命令。");
                    }
                    return true;

                case "give":
                    if (!sender.hasPermission("csextensions.give") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage("§c用法: /cse give <饰品ID> [玩家] [数量]");
                        return true;
                    }

                    String accessoryId = args[1];
                    Player targetPlayer;
                    int amount = 1;

                    if (args.length >= 3) {
                        targetPlayer = getServer().getPlayer(args[2]);
                        if (targetPlayer == null) {
                            sender.sendMessage("§c玩家 " + args[2] + " 不在线或不存在。");
                            return true;
                        }
                    } else {
                        if (sender instanceof Player) {
                            targetPlayer = (Player) sender;
                        } else {
                            sender.sendMessage("§c控制台使用时必须指定玩家。");
                            return true;
                        }
                    }

                    if (args.length >= 4) {
                        try {
                            amount = Integer.parseInt(args[3]);
                            if (amount < 1) amount = 1;
                            if (amount > 64) amount = 64;
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c数量必须是有效的数字。");
                            return true;
                        }
                    }

                    if (accessoriesManager.accessoryExists(accessoryId)) {
                        boolean success = accessoriesManager.giveAccessory(targetPlayer, accessoryId, amount);
                        if (success) {
                            sender.sendMessage("§a成功给予 " + targetPlayer.getName() + " " + amount + " 个 " + accessoryId);
                            if (!sender.equals(targetPlayer)) {
                                targetPlayer.sendMessage("§a你获得了饰品: " + accessoriesManager.getAccessoryDisplayName(accessoryId));
                            }
                        } else {
                            sender.sendMessage("§c给予饰品失败。");
                        }
                    } else {
                        sender.sendMessage("§c饰品ID '" + accessoryId + "' 不存在。");
                        sender.sendMessage("§c使用 /cse list 查看所有可用饰品。");
                    }
                    return true;

                case "list":
                    if (!sender.hasPermission("csextensions.list") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    List<String> accessoryIds = accessoriesManager.getAvailableAccessoryIds();
                    if (accessoryIds.isEmpty()) {
                        sender.sendMessage("§c没有可用的饰品。");
                    } else {
                        sender.sendMessage("§6可用饰品列表:");
                        for (String id : accessoryIds) {
                            String name = accessoriesManager.getAccessoryDisplayName(id);
                            sender.sendMessage("§e- " + id + " §7: " + name);
                        }
                    }
                    return true;

                case "debug":
                    if (!sender.hasPermission("csextensions.debug") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    boolean currentDebug = getConfig().getBoolean("debug", false);
                    getConfig().set("debug", !currentDebug);
                    saveConfig();

                    sender.sendMessage("§a调试模式 " + (!currentDebug ? "§a开启" : "§c关闭"));
                    return true;

                case "trails":
                    if (!sender.hasPermission("csextensions.trails") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    if (args.length < 2) {
                        sender.sendMessage("§c用法: /cse trails <武器ID>");
                        return true;
                    }

                    String weaponId = args[1];
                    if (modifierManager != null) {
                        if (modifierManager.hasModifier(weaponId, "Trails")) {
                            sender.sendMessage("§a武器 " + weaponId + " 配置了Trails效果");
                        } else {
                            sender.sendMessage("§c武器 " + weaponId + " 没有配置Trails效果");
                        }
                    } else {
                        sender.sendMessage("§cModifier系统未初始化");
                    }
                    return true;

                case "weapons":
                    if (!sender.hasPermission("csextensions.weapons") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    if (modifierManager != null) {
                        List<String> weaponIds = modifierManager.getLoadedWeaponIds();
                        if (weaponIds.isEmpty()) {
                            sender.sendMessage("§c没有配置任何武器");
                        } else {
                            sender.sendMessage("§6已配置的武器列表 (" + weaponIds.size() + " 个):");
                            for (String id : weaponIds) {
                                boolean hasTrails = modifierManager.hasModifier(id, "Trails");
                                sender.sendMessage("§e- " + id + " §7" + (hasTrails ? "§a[有Trails]" : "§c[无Trails]"));
                            }
                        }
                    } else {
                        sender.sendMessage("§cModifier系统未初始化");
                    }
                    return true;

                case "debugtrails":
                    if (!sender.hasPermission("csextensions.debug") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    if (trailsController != null && trailsController.getConfigManager() != null) {
                        TrailConfigManager trailManager = trailsController.getConfigManager();
                        List<String> trailsWeapons = trailManager.getLoadedWeaponIds();

                        sender.sendMessage("§6=== Trails调试信息 ===");
                        sender.sendMessage("§e已加载武器: " + trailsWeapons.size() + " 个");

                        for (String weaponId1 : trailsWeapons) {
                            TrailConfig config = trailManager.getTrailConfig(weaponId1);
                            if (config != null) {
                                sender.sendMessage("§7- " + weaponId1 + ": " + config.getEffects().size() + "种特效, " + "长度" + config.getLength() + ", " + "额外粒子" + config.getExtraParticlesAhead());
                            }
                        }
                    } else {
                        sender.sendMessage("§cTrails系统未初始化");
                    }
                    return true;
                case "particles":
                    if (!sender.hasPermission("csextensions.debug") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    try {
                        Class<?> particleUtil = Class.forName("org.Spike.CSExtensions.Modifier.Trails.ParticleUtil");
                        java.lang.reflect.Method method = particleUtil.getMethod("listAvailableParticles");
                        method.invoke(null);
                        sender.sendMessage("§a粒子列表已输出到控制台");
                    } catch (Exception e) {
                        sender.sendMessage("§c获取粒子列表失败: " + e.getMessage());
                    }
                    return true;
                case "debugknockback":
                    if (!sender.hasPermission("csextensions.debug") && !sender.isOp()) {
                        sender.sendMessage("§c你没有权限使用此命令。");
                        return true;
                    }

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        try {
                            Class<?> knockbackClass = Class.forName("org.Spike.CSExtensions.Modifier.Projectiles.ProjectileKnockbackCanceller");
                            java.lang.reflect.Method scanMethod = knockbackClass.getMethod("scanKnockbackFields", LivingEntity.class);
                            scanMethod.invoke(null, player);
                            sender.sendMessage("§a字段扫描完成，查看控制台输出");
                        } catch (Exception e) {
                            sender.sendMessage("§c扫描失败: " + e.getMessage());
                        }
                    }
                    return true;
                case "test":
                    if (!sender.hasPermission("csextensions.debug")) return true;

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        double damageMultiplier = accessoriesManager.getDamageMultiplier(player,
                                Collections.singleton("null"));
                        double reloadMultiplier = accessoriesManager.getReloadMultiplier(player, Collections.singleton("null"));
                        double spreadMultiplier = accessoriesManager.getSpreadMultiplier(player, Collections.singleton("null"));

                        sender.sendMessage("§6=== 饰品测试 ===");
                        sender.sendMessage(String.format("§7伤害倍率: §e%.2f", damageMultiplier));
                        sender.sendMessage(String.format("§7装弹倍率: §e%.2f", reloadMultiplier));
                        sender.sendMessage(String.format("§7扩散倍率: §e%.2f", spreadMultiplier));

                        List<AccessoriesData> accessories = accessoriesManager.getEquippedAccessories(player);
                        sender.sendMessage(String.format("§7装备饰品: §e%d个", accessories.size()));
                        for (AccessoriesData data : accessories) {
                            sender.sendMessage(String.format("  §8- §7%s §8(重量: %.1f)",
                                    data.getId(), data.getWeight()));
                        }
                    }
                    return true;


                default:
                    sender.sendMessage("§c未知命令。使用 /cse 查看帮助。");
                    return true;
            }
        }
        return false;
    }

    public AccessoriesManager getAccessoriesManager() {
        return accessoriesManager;
    }

    public ModifierManager getModifierManager() {
        return modifierManager;
    }

    public TrailsController getTrailsController() {
        return trailsController;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (accessoriesManager != null) {
                accessoriesManager.checkPlayerAccessories(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (accessoriesManager != null) {
            accessoriesManager.cleanupPlayer(player);
        }
    }

    private void testParticleSystem() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (Bukkit.getWorlds().isEmpty()) return;

            org.bukkit.World world = Bukkit.getWorlds().get(0);
            if (world != null) {
                Location testLoc = new Location(world, 0, 100, 0);

                try {
                    Class.forName("org.Spike.CSExtensions.Modifier.Trails.ParticleUtil");
                    Bukkit.getLogger().info("[测试] 尝试生成测试粒子...");

                    Class<?> particleUtil = Class.forName("org.Spike.CSExtensions.Modifier.Trails.ParticleUtil");
                    java.lang.reflect.Method method = particleUtil.getMethod("spawnFlame", Location.class, int.class);
                    method.invoke(null, testLoc, 5);

                    Bukkit.getLogger().info("[测试] 粒子测试完成");
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[测试] 粒子测试失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 100L);
    }

    public CSUtility getCSUtility() {
        return csUtility;
    }
    public SpikeElementsManager getSpikeElementsManager() {
        return spikeElementsManager;
    }

}