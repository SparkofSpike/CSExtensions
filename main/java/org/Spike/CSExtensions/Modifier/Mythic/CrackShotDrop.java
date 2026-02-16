package org.Spike.CSExtensions.Modifier.Mythic;

import com.shampaggon.crackshot.CSUtility;
import io.lumine.xikage.mythicmobs.adapters.AbstractItemStack;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitItemStack;
import io.lumine.xikage.mythicmobs.drops.Drop;
import io.lumine.xikage.mythicmobs.drops.DropMetadata;
import io.lumine.xikage.mythicmobs.drops.IItemDrop;
import io.lumine.xikage.mythicmobs.io.MythicLineConfig;
import io.lumine.xikage.mythicmobs.util.types.RandomDouble;
import org.Spike.CSExtensions.CSExtensions;
import org.bukkit.inventory.ItemStack;

public class CrackShotDrop extends Drop implements IItemDrop {
    private final String weaponId;
    private final CSUtility csUtility;
    private final CSExtensions plugin;

    public CrackShotDrop(CSExtensions plugin, String line, MythicLineConfig mlc, String weaponId,
                         CSUtility csUtility) {
        super(line, mlc);
        this.weaponId = weaponId;
        this.csUtility = csUtility;
        this.plugin = plugin;
    }

    public CrackShotDrop(CSExtensions plugin, String line, MythicLineConfig mlc, String weaponId,
                         CSUtility csUtility, RandomDouble amount) {
        super(line, mlc, amount);
        this.weaponId = weaponId;
        this.csUtility = csUtility;
        this.plugin = plugin;
    }

    @Override
    public AbstractItemStack getDrop(DropMetadata metadata) {
        this.rollAmount();

        ItemStack bukkitItem = csUtility.generateWeapon(weaponId);
        if (bukkitItem == null) {
            plugin.getLogger().warning("CrackShot武器不存在: " + weaponId);
            return null;
        }

        double amount = getAmount();
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[CrackShotDrop] 生成武器: " + weaponId +
                    ", 计算出的数量: " + amount);
        }

        if (amount > 0) {
            bukkitItem.setAmount((int) amount);
        }

        return new BukkitItemStack(bukkitItem);
    }

    public String getWeaponId() {
        return weaponId;
    }

    @Override
    public String toString() {
        return "CrackShotDrop{weapon=" + weaponId + ", amount=" + getAmount() + "}";
    }
}