package org.Spike.CSExtensions.Modifier.Accessories;

import org.Spike.CSExtensions.CSExtensions;
import org.Spike.CSExtensions.Modifier.Accessories.Mythic.AccessoryMythicHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AccessoriesManager implements Listener {
    private final CSExtensions plugin;
    private final AccessoriesConfig config;
    private final AccessoriesCalculator calculator;
    private final AccessoryMythicHandler mythicHandler;
    private final AccessoriesHandler accessoriesHandler;

    private final Map<UUID, List<AccessoriesData>> playerAccessories = new HashMap<>();
    private final Map<UUID, Map<AttributeType, Double>> playerAttributes = new HashMap<>();
    private final Map<UUID, Float> originalWalkSpeeds = new HashMap<>();

    public AccessoriesManager(CSExtensions plugin) {
        this.plugin = plugin;
        this.config = new AccessoriesConfig(plugin);
        this.calculator = new AccessoriesCalculator();
        this.mythicHandler = new AccessoryMythicHandler(plugin, this);
        this.accessoriesHandler = new AccessoriesHandler(plugin, this);
        Bukkit.getPluginManager().registerEvents(accessoriesHandler, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerAccessories(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updatePlayerAccessories(Player player) {
        List<AccessoriesData> equippedAccessories = scanEquippedAccessories(player);
        List<AccessoriesData> oldAccessories = playerAccessories.get(player.getUniqueId());

        if (!areAccessoriesEqual(equippedAccessories, oldAccessories)) {
            playerAccessories.put(player.getUniqueId(), equippedAccessories);
            recalculatePlayerAttributes(player, equippedAccessories);
            applyEffects(player);

            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info(String.format(
                        "[饰品] 玩家 %s 饰品更新: %d个饰品, 总重量: %.1f",
                        player.getName(), equippedAccessories.size(),
                        calculator.calculateTotalWeight(equippedAccessories)
                ));
            }
        }
    }

    private List<AccessoriesData> scanEquippedAccessories(Player player) {
        List<AccessoriesData> accessories = new ArrayList<>();
        Set<String> equippedIds = new HashSet<>();

        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            AccessoriesData accessory = identifyAccessory(item, false);
            if (accessory != null) {
                if (!equippedIds.contains(accessory.getId())) {
                    accessories.add(accessory);
                    equippedIds.add(accessory.getId());
                } else {
                    if (plugin.getConfig().getBoolean("debug")) {
                        plugin.getLogger().info(String.format(
                                "玩家 %s 尝试装备重复饰品: %s (槽位: %d)",
                                player.getName(), accessory.getId(), i
                        ));
                    }
                }
            }
        }

        int[] armorSlots = {36, 37, 38, 39};
        for (int slot : armorSlots) {
            ItemStack item = player.getInventory().getItem(slot);
            AccessoriesData accessory = identifyAccessory(item, true);
            if (accessory != null) {
                if (!equippedIds.contains(accessory.getId())) {
                    accessories.add(accessory);
                    equippedIds.add(accessory.getId());
                }
            }
        }
        Map<String, Set<String>> conflicts = checkConflicts(equippedIds);
        if (!conflicts.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : conflicts.entrySet()) {
                ConflictGroup group = config.getConflictGroups().get(entry.getKey());
                if (group != null) {
                    String conflictNames = String.join("、", entry.getValue());
                    player.sendMessage(group.getExceededMessage() + " §7(冲突: " + conflictNames + ")");

                    if (plugin.getConfig().getBoolean("debug")) {
                        plugin.getLogger().info(String.format(
                                "[饰品冲突] 玩家 %s 冲突组 %s: %s",
                                player.getName(), entry.getKey(), conflictNames
                        ));
                    }
                }
            }
        }
        return accessories;
    }

    private AccessoriesData identifyAccessory(ItemStack item, boolean isArmorSlot) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) {
            return null;
        }

        String displayName = meta.getDisplayName();
        Material material = item.getType();
        short data = item.getDurability();

        for (String accessoryId : config.getAllAccessoryIds()) {
            AccessoriesData accessory = config.getAccessory(accessoryId);
            if (accessory == null) continue;

            String configName = accessory.getDisplayName();
            if (!configName.equals(displayName)) {
                continue;
            }

            if (accessory.getMaterial() != material) {
                continue;
            }

            if (accessory.getData() != 0 && accessory.getData() != data) {
                continue;
            }

            if (accessory.isArmor() && !isArmorSlot) {
                continue;
            }

            return accessory;
        }

        return null;
    }

    private void recalculatePlayerAttributes(Player player, List<AccessoriesData> accessories) {
        Map<AttributeType, Double> attributes = new EnumMap<>(AttributeType.class);

        double totalWeight = calculator.calculateTotalWeight(accessories);
        attributes.put(AttributeType.WEIGHT, totalWeight);

        double maxHealth = calculator.calculateMaxHealth(accessories);
        attributes.put(AttributeType.HEALTH, maxHealth);

        playerAttributes.put(player.getUniqueId(), attributes);
    }

    private void applyEffects(Player player) {
        Map<AttributeType, Double> attributes = playerAttributes.get(player.getUniqueId());
        if (attributes == null) return;

        UUID playerId = player.getUniqueId();

        Double totalWeight = attributes.get(AttributeType.WEIGHT);
        float newSpeed = player.getWalkSpeed();
        if (totalWeight != null) {
            newSpeed = calculator.calculateWalkSpeed(totalWeight);

            if (!originalWalkSpeeds.containsKey(playerId)) {
                originalWalkSpeeds.put(playerId, player.getWalkSpeed());
            }

            player.setWalkSpeed(newSpeed);
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[饰品重量] 玩家 %s 总重量: %.1f, 移动速度: %.3f",
                    player.getName(), totalWeight, newSpeed
            ));
        }

        Double maxHealth = attributes.get(AttributeType.HEALTH);
        if (maxHealth != null) {
            double currentMax = player.getMaxHealth();
            if (Math.abs(currentMax - maxHealth) > 0.1) {
                player.setMaxHealth(maxHealth);

                if (player.getHealth() > maxHealth) {
                    player.setHealth(maxHealth);
                }
            }
        }
    }

    public double getDamageMultiplier(Player player, Set<String> weaponElement) {
        List<AccessoriesData> accessories = playerAccessories.get(player.getUniqueId());
        if (accessories == null || accessories.isEmpty()) {
            return 1.0;
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[饰品伤害计算] 玩家 %s 元素 %s 饰品数 %d",
                    player.getName(), weaponElement, accessories.size()
            ));
        }

        List<AccessoriesData> effectiveAccessories = calculator.filterByArmorRequirement(
                accessories, isInArmorSlot(player)
        );

        double result = calculator.calculateEffect(effectiveAccessories, AttributeType.DAMAGE, weaponElement);

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info(String.format(
                    "[饰品伤害计算] 结果: %.2f (有效饰品: %d)",
                    result, effectiveAccessories.size()
            ));
        }

        return result;
    }

    public double getReloadMultiplier(Player player, Set<String> weaponElement) {
        List<AccessoriesData> accessories = playerAccessories.get(player.getUniqueId());
        if (accessories == null || accessories.isEmpty()) {
            return 1.0;
        }

        List<AccessoriesData> effectiveAccessories = calculator.filterByArmorRequirement(
                accessories, isInArmorSlot(player)
        );

        return calculator.calculateEffect(effectiveAccessories, AttributeType.RELOAD, weaponElement);
    }

    public double getSpreadMultiplier(Player player, Set<String> weaponElement) {
        List<AccessoriesData> accessories = playerAccessories.get(player.getUniqueId());
        if (accessories == null || accessories.isEmpty()) {
            return 1.0;
        }

        List<AccessoriesData> effectiveAccessories = calculator.filterByArmorRequirement(
                accessories, isInArmorSlot(player)
        );

        return calculator.calculateEffect(effectiveAccessories, AttributeType.SPREAD, weaponElement);
    }

    private boolean isInArmorSlot(Player player) {
        int[] armorSlots = {36, 37, 38, 39};

        for (int slot : armorSlots) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();
                    for (AccessoriesData accessory : playerAccessories.getOrDefault(
                            player.getUniqueId(), Collections.emptyList())) {
                        if (accessory.getDisplayName().equals(displayName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Set<String> getEquippedAccessoryIds(Player player) {
        List<AccessoriesData> accessories = playerAccessories.get(player.getUniqueId());
        if (accessories == null) {
            return Collections.emptySet();
        }

        Set<String> ids = new HashSet<>();
        for (AccessoriesData data : accessories) {
            ids.add(data.getId());
        }
        return ids;
    }

    public List<AccessoriesData> getEquippedAccessories(Player player) {
        return playerAccessories.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void cleanupPlayer(Player player) {
        UUID playerId = player.getUniqueId();

        if (originalWalkSpeeds.containsKey(playerId)) {
            player.setWalkSpeed(originalWalkSpeeds.get(playerId));
            originalWalkSpeeds.remove(playerId);
        }

        player.setMaxHealth(20.0);

        playerAccessories.remove(playerId);
        playerAttributes.remove(playerId);
        mythicHandler.cleanupPlayer(player);

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("玩家 " + player.getName() + " 饰品数据已清理");
        }
    }

    public void cleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanupPlayer(player);
        }

        playerAccessories.clear();
        playerAttributes.clear();
        originalWalkSpeeds.clear();
        mythicHandler.cleanup();
    }

    public void reload() {
        plugin.getLogger().info("[饰品] 开始重载...");

        if (config == null) {
            plugin.getLogger().warning("[饰品] config为null!");
            return;
        }

        try {
            config.reload();
            plugin.getLogger().info("[饰品] config.reload()调用完成");
        } catch (Exception e) {
            plugin.getLogger().severe("[饰品] 重载失败: " + e.getMessage());
            e.printStackTrace();
        }

        int playerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<AccessoriesData> accessories = scanEquippedAccessories(player);
            playerAccessories.put(player.getUniqueId(), accessories);
            recalculatePlayerAttributes(player, accessories);
            applyEffects(player);
            playerCount++;
        }

        plugin.getLogger().info("[饰品] 重载完成，更新了 " + playerCount + " 个玩家");
    }

    private boolean areAccessoriesEqual(List<AccessoriesData> list1, List<AccessoriesData> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        Set<String> ids1 = new HashSet<>();
        Set<String> ids2 = new HashSet<>();

        for (AccessoriesData data : list1) ids1.add(data.getId());
        for (AccessoriesData data : list2) ids2.add(data.getId());

        return ids1.equals(ids2);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        updatePlayerAccessories(event.getPlayer());
    }

    public boolean accessoryExists(String accessoryId) {
        return config.accessoryExists(accessoryId);
    }

    public boolean giveAccessory(Player player, String accessoryId, int amount) {
        if (!accessoryExists(accessoryId)) {
            return false;
        }

        AccessoriesData data = config.getAccessory(accessoryId);
        if (data == null) {
            return false;
        }

        ItemStack item = createAccessoryItem(accessoryId, amount);
        if (item != null) {
            player.getInventory().addItem(item);
            return true;
        }
        return false;
    }

    public ItemStack createAccessoryItem(String accessoryId, int amount) {
        AccessoriesData data = config.getAccessory(accessoryId);
        if (data == null) return null;

        ItemStack item = new ItemStack(data.getMaterial(), amount, data.getData());
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(data.getDisplayName());

        List<String> displayLore = data.getDisplayLore();
        if (!displayLore.isEmpty()) {
            meta.setLore(displayLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    public Map<String, Set<String>> checkConflicts(Set<String> accessoryIds) {
        Map<String, Set<String>> conflicts = new HashMap<>();

        for (ConflictGroup group : config.getConflictGroups().values()) {
            Set<String> conflictIds = group.checkConflict(accessoryIds);
            if (!conflictIds.isEmpty()) {
                conflicts.put(group.getName(), conflictIds);
            }
        }

        return conflicts;
    }

    public ConflictResult canEquipAccessory(Set<String> equippedIds, String newAccessoryId) {
        Set<String> testIds = new HashSet<>(equippedIds);
        testIds.add(newAccessoryId);

        Map<String, Set<String>> conflicts = checkConflicts(testIds);

        if (conflicts.isEmpty()) {
            return new ConflictResult(true, null, null);
        } else {
            Map.Entry<String, Set<String>> firstConflict = conflicts.entrySet().iterator().next();
            ConflictGroup group = config.getConflictGroups().get(firstConflict.getKey());
            return new ConflictResult(false, group, firstConflict.getValue());
        }
    }

    public String getAccessoryDisplayName(String accessoryId) {
        AccessoriesData data = config.getAccessory(accessoryId);
        return data != null ? data.getDisplayName() : accessoryId;
    }

    public List<String> getAvailableAccessoryIds() {
        return new ArrayList<>(config.getAllAccessoryIds());
    }

    public void checkPlayerAccessories(Player player) {
        updatePlayerAccessories(player);
    }

    public AccessoriesConfig getConfig() {
        return config;
    }

    public static class ConflictResult {
        private final boolean canEquip;
        private final ConflictGroup conflictGroup;
        private final Set<String> conflictIds;

        public ConflictResult(boolean canEquip, ConflictGroup conflictGroup, Set<String> conflictIds) {
            this.canEquip = canEquip;
            this.conflictGroup = conflictGroup;
            this.conflictIds = conflictIds;
        }

        public boolean canEquip() {
            return canEquip;
        }

        public ConflictGroup getConflictGroup() {
            return conflictGroup;
        }

        public Set<String> getConflictIds() {
            return conflictIds;
        }

        public String getMessage() {
            if (canEquip || conflictGroup == null) {
                return "§a可以装备";
            }
            return conflictGroup.getExceededMessage();
        }
    }
}