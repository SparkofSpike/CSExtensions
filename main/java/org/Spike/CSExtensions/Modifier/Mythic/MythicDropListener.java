package org.Spike.CSExtensions.Modifier.Mythic;

import com.shampaggon.crackshot.CSUtility;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicDropLoadEvent;
import io.lumine.xikage.mythicmobs.drops.Drop;
import io.lumine.xikage.mythicmobs.io.MythicLineConfig;
import io.lumine.xikage.mythicmobs.util.types.RandomDouble;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.Spike.CSExtensions.CSExtensions;

public class MythicDropListener implements Listener {
    private final CSExtensions plugin;
    private final CSUtility csUtility;
    private static final String PREFIX = "CS@";

    public MythicDropListener(CSExtensions plugin) {
        this.plugin = plugin;
        this.csUtility = plugin.getCSUtility();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicDropLoad(MythicDropLoadEvent event) {
        if (!(event.getContainer() instanceof Drop)) {
            return;
        }

        Drop container = (Drop) event.getContainer();
        String fullLine = container.getLine();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[CrackShotDrop] 完整配置行: " + fullLine);
        }

        if (fullLine != null && fullLine.toUpperCase().startsWith(PREFIX)) {
            String[] parts = fullLine.split(" ");
            if (parts.length < 1) return;

            String firstPart = parts[0];
            if (!firstPart.contains("@")) return;

            String weaponId = firstPart.substring(firstPart.indexOf("@") + 1);

            CrackShotDrop csDrop;
            if (parts.length >= 3 && RandomDouble.matches(parts[1])) {
                csDrop = new CrackShotDrop(plugin, fullLine, event.getConfig(), weaponId,
                        csUtility, new RandomDouble(parts[1]));
            } else if (parts.length >= 2 && RandomDouble.matches(parts[1])) {
                csDrop = new CrackShotDrop(plugin, fullLine, event.getConfig(), weaponId,
                        csUtility, new RandomDouble(parts[1]));
            } else {
                csDrop = new CrackShotDrop(plugin, fullLine, event.getConfig(), weaponId, csUtility);
            }

            event.register(csDrop);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[CrackShotDrop] 注册成功: " + weaponId);
            }
        }
    }

    private void handleCrackShotDrop(MythicDropLoadEvent event, String dropString) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[CrackShotDrop] 处理CS掉落: " + dropString);
        }

        try {
            String[] parts = dropString.split(" ");
            if (parts.length < 1) {
                plugin.getLogger().warning("无效的CS掉落格式: " + dropString);
                return;
            }

            String firstPart = parts[0];
            if (!firstPart.contains("@")) {
                plugin.getLogger().warning("CS掉落格式错误，缺少@分隔符: " + dropString);
                return;
            }

            String weaponId = firstPart.substring(firstPart.indexOf("@") + 1);
            if (weaponId.isEmpty()) {
                plugin.getLogger().warning("CS掉落缺少武器ID: " + dropString);
                return;
            }

            if (csUtility.generateWeapon(weaponId) == null) {
                plugin.getLogger().warning("CrackShot武器不存在: " + weaponId + " (配置: " + dropString + ")");
                return;
            }

            MythicLineConfig mlc = event.getConfig();
            CrackShotDrop csDrop = createCrackShotDrop(dropString, weaponId, mlc);

            if (csDrop == null) {
                return;
            }

            event.register(csDrop);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[CrackShotDrop] 成功注册: " + weaponId);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("解析CS掉落失败: " + dropString + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private CrackShotDrop createCrackShotDrop(String dropString, String weaponId, MythicLineConfig mlc) {
        String[] parts = dropString.split(" ");

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[CrackShotDrop 解析] 原始配置: " + dropString);
            plugin.getLogger().info("[CrackShotDrop 解析] 分割结果 (" + parts.length + "部分): " +
                    java.util.Arrays.toString(parts));
        }

        if (parts.length == 1) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[CrackShotDrop 解析] 格式1: 只有武器ID，数量默认1");
            }
            return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility);
        }

        if (parts.length == 2) {
            if (RandomDouble.matches(parts[1])) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[CrackShotDrop 解析] 格式2: 武器ID + 数量(" + parts[1] + ")");
                }
                return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility, new RandomDouble(parts[1]));
            } else {
                plugin.getLogger().warning("CS掉落配置错误: 第二个参数应该是数量(数字): " + dropString);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[CrackShotDrop 解析] 格式2错误: 第二个参数不是数字，使用默认数量1");
                }
                return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility);
            }
        }

        if (parts.length == 3) {
            if (RandomDouble.matches(parts[1])) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[CrackShotDrop 解析] 格式3: 武器ID + 数量(" + parts[1] + ") + 几率(" + parts[2] + ")");
                }
                return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility, new RandomDouble(parts[1]));
            } else {
                plugin.getLogger().warning("CS掉落配置错误: 第二个参数应该是数量(数字): " + dropString);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[CrackShotDrop 解析] 格式3错误: 第二个参数不是数字，尝试使用第三个参数作为数量");
                }

                if (parts.length > 2 && RandomDouble.matches(parts[2])) {
                    return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility, new RandomDouble(parts[2]));
                } else {
                    return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility);
                }
            }
        }

        plugin.getLogger().warning("CS掉落配置可能格式错误(超过3个参数): " + dropString);
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[CrackShotDrop 解析] 复杂格式，尝试使用第二个参数作为数量");
        }

        if (parts.length > 1 && RandomDouble.matches(parts[1])) {
            return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility, new RandomDouble(parts[1]));
        } else {
            return new CrackShotDrop(plugin, dropString, mlc, weaponId, csUtility);
        }
    }
}