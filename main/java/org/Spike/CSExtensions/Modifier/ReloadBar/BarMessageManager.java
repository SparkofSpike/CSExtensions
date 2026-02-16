package org.Spike.CSExtensions.Modifier.ReloadBar;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.ModifierManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BarMessageManager {

    private final CSExtensions plugin;
    private File barMessageFile;
    private FileConfiguration barMessageConfig;
    private File gunsFile;
    private FileConfiguration gunsConfig;
    private final Map<String, BarMessageGroup> messageGroups;
    private BarMessageGroup defaultGroup;

    public BarMessageManager(CSExtensions plugin) {
        this.plugin = plugin;
        this.messageGroups = new HashMap<>();
        createBarMessageConfig();
        createGunsConfig();
        loadMessageGroups();
    }

    private void createBarMessageConfig() {
        barMessageFile = new File(plugin.getDataFolder(), "barmessage.yml");
        if (!barMessageFile.exists()) {
            barMessageFile.getParentFile().mkdirs();
            plugin.saveResource("barmessage.yml", false);
        }
        barMessageConfig = YamlConfiguration.loadConfiguration(barMessageFile);
    }

    private void createGunsConfig() {
        gunsFile = new File(plugin.getDataFolder(), "cse_guns.yml");
        if (!gunsFile.exists()) {
            gunsFile.getParentFile().mkdirs();
            plugin.saveResource("cse_guns.yml", false);
        }
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
    }

    private void loadMessageGroups() {
        messageGroups.clear();

        if (barMessageConfig.contains("Group_Default")) {
            defaultGroup = loadMessageGroup("Group_Default");
            messageGroups.put("Group_Default", defaultGroup);
        }

        for (String key : barMessageConfig.getKeys(false)) {
            if (!key.equals("Group_Default") && barMessageConfig.contains(key)) {
                BarMessageGroup group = loadMessageGroup(key);
                messageGroups.put(key, group);
            }
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("已加载 " + messageGroups.size() + " 个消息分组");
        }
    }

    private BarMessageGroup loadMessageGroup(String groupName) {
        String path = groupName + ".";
        BarMessageGroup group = new BarMessageGroup();

        group.setGroupName(groupName);
        group.setReloadMessage(barMessageConfig.getString(path + "ReloadMessage", "&7&l武器装填中 %reloadbar% &7&l%timeleft%"));
        group.setReloadCompleteMessage(barMessageConfig.getString(path + "ReloadCompleteMessage", "&7&l装填完毕"));
        group.setReloadFailedMessage(barMessageConfig.getString(path + "ReloadFailedMessage", "&c&l装填失败"));
        group.setColorPGB(barMessageConfig.getString(path + "Color_PGB", "c"));
        group.setColorBGC(barMessageConfig.getString(path + "Color_BGC", "7"));
        group.setReloadSymbol(barMessageConfig.getString(path + "ReloadSymbol", "※"));
        group.setReloadSymbolAmount(barMessageConfig.getInt(path + "ReloadSymbolAmount", 20));

        return group;
    }

    public BarMessageGroup getMessageGroupForWeapon(String weaponTitle) {
        ModifierManager modifierManager = plugin.getModifierManager();
        if (modifierManager == null) {
            plugin.getLogger().warning("ModifierManager未初始化，无法获取武器配置");
            return defaultGroup;
        }

        org.bukkit.configuration.ConfigurationSection weaponConfig = modifierManager.getWeaponConfig(weaponTitle);
        if (weaponConfig == null) {
            weaponConfig = gunsConfig.getConfigurationSection(weaponTitle);
        }

        if (weaponConfig != null && weaponConfig.contains("BarMessageGroup")) {
            String groupName = weaponConfig.getString("BarMessageGroup");
            if (groupName != null && messageGroups.containsKey(groupName)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("武器 " + weaponTitle + " 使用分组: " + groupName);
                }
                return messageGroups.get(groupName);
            } else if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("武器 " + weaponTitle + " 配置的分组不存在: " + groupName);
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("武器 " + weaponTitle + " 使用默认分组");
        }
        return defaultGroup;
    }

    public void reloadConfig() {
        barMessageConfig = YamlConfiguration.loadConfiguration(barMessageFile);
        gunsConfig = YamlConfiguration.loadConfiguration(gunsFile);
        loadMessageGroups();

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("BarMessage配置重载完成");
        }
    }

    public Map<String, BarMessageGroup> getMessageGroups() {
        return messageGroups;
    }

    public static class BarMessageGroup {
        private String groupName;
        private String reloadMessage;
        private String reloadCompleteMessage;
        private String reloadFailedMessage;
        private String colorPGB;
        private String colorBGC;
        private String reloadSymbol;
        private int reloadSymbolAmount;

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }

        public String getReloadMessage() {
            return reloadMessage;
        }

        public void setReloadMessage(String reloadMessage) {
            this.reloadMessage = reloadMessage;
        }

        public String getReloadCompleteMessage() {
            return reloadCompleteMessage;
        }

        public void setReloadCompleteMessage(String reloadCompleteMessage) {
            this.reloadCompleteMessage = reloadCompleteMessage;
        }

        public String getReloadFailedMessage() {
            return reloadFailedMessage;
        }

        public void setReloadFailedMessage(String reloadFailedMessage) {
            this.reloadFailedMessage = reloadFailedMessage;
        }

        public String getColorPGB() {
            return colorPGB;
        }

        public void setColorPGB(String colorPGB) {
            this.colorPGB = colorPGB;
        }

        public String getColorBGC() {
            return colorBGC;
        }

        public void setColorBGC(String colorBGC) {
            this.colorBGC = colorBGC;
        }

        public String getReloadSymbol() {
            return reloadSymbol;
        }

        public void setReloadSymbol(String reloadSymbol) {
            this.reloadSymbol = reloadSymbol;
        }

        public int getReloadSymbolAmount() {
            return reloadSymbolAmount;
        }

        public void setReloadSymbolAmount(int reloadSymbolAmount) {
            this.reloadSymbolAmount = reloadSymbolAmount;
        }

        public String getFormattedReloadMessage(String reloadBar, String timeLeft) {
            if (reloadMessage == null) return "";
            return reloadMessage
                    .replace("%reloadbar%", reloadBar)
                    .replace("%timeleft%", timeLeft);
        }
    }
}