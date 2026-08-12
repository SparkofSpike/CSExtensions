package org.Spike.CSExtensions.Modifier.ReloadBar;

import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.shampaggon.crackshot.events.WeaponReloadEvent;

import java.util.*;

public class ReloadBarManager {

    private final CSExtensions plugin;
    private final Map<UUID, ReloadSession> activeReloads;
    private final BarMessageManager barMessageManager;

    public ReloadBarManager(CSExtensions plugin) {
        this.plugin = plugin;
        this.activeReloads = new HashMap<>();
        this.barMessageManager = new BarMessageManager(plugin);
    }

    private static class ReloadSession {
        int originalReloadDuration;
        double reloadSpeedMultiplier;
        int adjustedReloadDuration;
        String weaponTitle;
        BukkitTask reloadTask;
        int counter;
        boolean isActive;
        long startTime;
        BarMessageManager.BarMessageGroup messageGroup;

        ReloadSession(int originalReloadDuration, double reloadSpeedMultiplier, String weaponTitle, BarMessageManager.BarMessageGroup messageGroup) {
            this.originalReloadDuration = originalReloadDuration;
            this.reloadSpeedMultiplier = reloadSpeedMultiplier;
            this.adjustedReloadDuration = (int) (originalReloadDuration * reloadSpeedMultiplier);
            this.weaponTitle = weaponTitle;
            this.counter = 0;
            this.isActive = true;
            this.startTime = System.currentTimeMillis();
            this.messageGroup = messageGroup;
        }
    }

    public void startReload(WeaponReloadEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        cancelReload(player);

        int originalReloadDuration = event.getReloadDuration();

        double reloadSpeedMultiplier = 1.0;
        if (plugin.getAccessoriesManager() != null) {
            String weaponId = event.getWeaponTitle();
            Set<String> weaponTags = Collections.emptySet();

            if (plugin.getSpikeElementsManager() != null) {
                weaponTags = plugin.getSpikeElementsManager().getTagReader().getWeaponTags(weaponId);
            }

            reloadSpeedMultiplier *= plugin.getAccessoriesManager().getReloadMultiplier(player,
                    weaponTags);
        }

        BarMessageManager.BarMessageGroup messageGroup = barMessageManager.getMessageGroupForWeapon(event.getWeaponTitle());

        ReloadSession session = new ReloadSession(
                originalReloadDuration,
                reloadSpeedMultiplier,
                event.getWeaponTitle(),
                messageGroup
        );

        activeReloads.put(playerId, session);
        startReloadBarTask(player, session);

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[ReloadBarManager监听器] 玩家 %s 开始装弹: 武器=%s, 最终时间=%dtick, 倍率=%.2f, 消息分组=%s",
                    player.getName(), event.getWeaponTitle(), originalReloadDuration,
                    reloadSpeedMultiplier, getGroupNameForWeapon(event.getWeaponTitle())
            ));
        }
    }

    private String getGroupNameForWeapon(String weaponTitle) {
        BarMessageManager.BarMessageGroup group = barMessageManager.getMessageGroupForWeapon(weaponTitle);
        for (Map.Entry<String, BarMessageManager.BarMessageGroup> entry : barMessageManager.getMessageGroups().entrySet()) {
            if (entry.getValue() == group) {
                return entry.getKey();
            }
        }
        return "Default";
    }

    private void startReloadBarTask(Player player, ReloadSession session) {
        UUID playerId = player.getUniqueId();

        long updateInterval = plugin.getConfig().getLong("CSEReloadBar.UpdateInterval", 2L);

        session.reloadTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeReloads.containsKey(playerId) || !player.isOnline() || !session.isActive) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                long elapsedMillis = currentTime - session.startTime;

                double totalSeconds = session.adjustedReloadDuration / 20.0;
                double elapsedSeconds = elapsedMillis / 1000.0;

                double progress = Math.min(elapsedSeconds / totalSeconds, 1.0);

                session.counter = (int) (progress * session.adjustedReloadDuration);

                updateReloadBar(player, session, progress);

                if (progress >= 1.0) {
                    completeReload(player);
                }
            }
        }.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void updateReloadBar(Player player, ReloadSession session, double progress) {
        String reloadBar = generateReloadBar(session, progress);

        String timeLeftText = generateTimeLeftText(session, progress);

        String message = buildReloadMessage(session, reloadBar, timeLeftText);

        sendActionBar(player, message);
    }

    private String generateReloadBar(ReloadSession session, double progress) {
        BarMessageManager.BarMessageGroup group = session.messageGroup;

        int symbolAmount = group.getReloadSymbolAmount();
        String symbol = group.getReloadSymbol();
        String colorPGB = "&" + group.getColorPGB();
        String colorBGC = "&" + group.getColorBGC();

        int completed = (int) (progress * symbolAmount);
        int remaining = symbolAmount - completed;

        StringBuilder bar = new StringBuilder();
        bar.append(colorPGB);
        for (int i = 0; i < completed; i++) {
            bar.append(symbol);
        }
        bar.append(colorBGC);
        for (int i = 0; i < remaining; i++) {
            bar.append(symbol);
        }

        return bar.toString();
    }

    private String generateTimeLeftText(ReloadSession session, double progress) {
        double totalSeconds = session.adjustedReloadDuration / 20.0;
        double elapsedSeconds = progress * totalSeconds;
        double timeLeftSeconds = Math.max(totalSeconds - elapsedSeconds, 0);

        String timeText = String.format("%.1f", timeLeftSeconds);
        String totalTimeText = String.format("%.1f", totalSeconds);

        FileConfiguration config = plugin.getConfig();
        boolean showReloadTime = config.getBoolean("CSEReloadBar.ShowReloadTime", false);
        boolean showTimeAdjustment = config.getBoolean("CSEReloadBar.ShowTimeAdjustment", true);

        String baseTimeText;
        if (showReloadTime) {
            baseTimeText = timeText + "s/" + totalTimeText + "s";
        } else {
            baseTimeText = timeText + "s";
        }

        if (showTimeAdjustment) {
            double timeAdjustPercent = (session.reloadSpeedMultiplier - 1) * 100;
            String adjustmentText;

            if (timeAdjustPercent > 0) {
                adjustmentText = String.format("(已增加%.0f%%)", Math.abs(timeAdjustPercent));
            } else if (timeAdjustPercent < 0) {
                adjustmentText = String.format("(已减少%.0f%%)", Math.abs(timeAdjustPercent));
            } else {
                adjustmentText = "";
            }

            return baseTimeText + adjustmentText;
        }

        return baseTimeText;
    }

    private String buildReloadMessage(ReloadSession session, String reloadBar, String timeLeft) {
        String messageFormat = session.messageGroup.getReloadMessage();

        return ChatColor.translateAlternateColorCodes('&',
                messageFormat.replace("%reloadbar%", reloadBar).replace("%timeleft%", timeLeft));
    }

    private void sendActionBar(Player player, String message) {
        try {
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);

            java.lang.reflect.Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(craftPlayer);

            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);

            Class<?> chatComponentTextClass = Class.forName("net.minecraft.server.v1_8_R3.ChatComponentText");
            Object chatComponent = chatComponentTextClass.getConstructor(String.class).newInstance(
                    ChatColor.translateAlternateColorCodes('&', message)
            );

            Class<?> packetPlayOutChatClass = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat");
            Object packet = packetPlayOutChatClass.getConstructor(
                    Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent"),
                    byte.class
            ).newInstance(chatComponent, (byte) 2);

            java.lang.reflect.Method sendPacketMethod = playerConnection.getClass().getMethod("sendPacket",
                    Class.forName("net.minecraft.server.v1_8_R3.Packet"));
            sendPacketMethod.invoke(playerConnection, packet);

        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().warning("发送ActionBar失败: " + e.getMessage());
            }
        }
    }

    public void completeReload(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeReloads.containsKey(playerId)) {
            ReloadSession session = activeReloads.get(playerId);

            String completeMessage = session.messageGroup.getReloadCompleteMessage();
            sendActionBar(player, ChatColor.translateAlternateColorCodes('&', completeMessage));

            int completeDuration = plugin.getConfig().getInt("CSEReloadBar.CompleteMessageDuration", 40);
            if (completeDuration > 0) {
                final int totalTicks = completeDuration;
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= totalTicks || !player.isOnline()) {
                            this.cancel();
                            return;
                        }
                        sendActionBar(player, ChatColor.translateAlternateColorCodes('&', completeMessage));
                        ticks += 2;
                    }
                }.runTaskTimer(plugin, 0L, 2L);
            }

            cleanupPlayer(playerId);

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("玩家 " + player.getName() + " 装弹完成");
            }
        }
    }

    public void failReload(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeReloads.containsKey(playerId)) {
            ReloadSession session = activeReloads.get(playerId);

            String failMessage = session.messageGroup.getReloadFailedMessage();
            sendActionBar(player, ChatColor.translateAlternateColorCodes('&', failMessage));

            cleanupPlayer(playerId);

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("玩家 " + player.getName() + " 装弹失败");
            }
        }
    }

    public void cancelReload(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeReloads.containsKey(playerId)) {
            ReloadSession session = activeReloads.get(playerId);

            String failMessage = session.messageGroup.getReloadFailedMessage();
            sendActionBar(player, ChatColor.translateAlternateColorCodes('&', failMessage));

            cleanupPlayer(playerId);

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("玩家 " + player.getName() + " 装弹取消");
            }
        }
    }

    private void cleanupPlayer(UUID playerId) {
        if (activeReloads.containsKey(playerId)) {
            ReloadSession session = activeReloads.get(playerId);
            session.isActive = false;
            if (session.reloadTask != null) {
                try {
                    session.reloadTask.cancel();
                } catch (Exception e) {
                }
            }
            activeReloads.remove(playerId);
        }
    }

    public void cleanup() {
        for (UUID playerId : activeReloads.keySet()) {
            ReloadSession session = activeReloads.get(playerId);
            session.isActive = false;
            if (session.reloadTask != null) {
                try {
                    session.reloadTask.cancel();
                } catch (Exception e) {
                }
            }
        }
        activeReloads.clear();
    }

    public void cleanupOnQuit(Player player) {
        cleanupPlayer(player.getUniqueId());
    }

    public void reloadConfig() {
        barMessageManager.reloadConfig();
    }

    public boolean isReloading(Player player) {
        return activeReloads.containsKey(player.getUniqueId());
    }

    public BarMessageManager getBarMessageManager() {
        return barMessageManager;
    }
}