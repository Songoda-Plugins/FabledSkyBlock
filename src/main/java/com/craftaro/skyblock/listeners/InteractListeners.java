package com.craftaro.skyblock.listeners;

import com.craftaro.core.compatibility.CompatibleHand;
import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.hooks.LogManager;
import com.craftaro.skyblock.playerdata.PlayerData;
import com.craftaro.third_party.com.cryptomorin.xseries.XBlock;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.core.utils.ItemUtils;
import com.craftaro.core.utils.NumberUtils;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.config.FileManager;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.island.IslandLevel;
import com.craftaro.skyblock.island.IslandManager;
import com.craftaro.skyblock.island.IslandWorld;
import com.craftaro.skyblock.levelling.IslandLevelManager;
import com.craftaro.skyblock.limit.impl.BlockLimitation;
import com.craftaro.skyblock.message.MessageManager;
import com.craftaro.skyblock.sound.SoundManager;
import com.craftaro.skyblock.stackable.Stackable;
import com.craftaro.skyblock.stackable.StackableManager;
import com.craftaro.skyblock.utils.structure.StructureUtil;
import com.craftaro.skyblock.utils.world.LocationUtil;
import com.craftaro.skyblock.world.WorldManager;
import org.apache.commons.lang.WordUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.util.HashMap;
import java.util.Optional;

public class InteractListeners implements Listener {
    private final SkyBlock plugin;

    public InteractListeners(SkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onWaterPlace(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());

        Optional<XMaterial> material = CompatibleMaterial.getMaterial(block.getType());
        IslandManager islandManager = this.plugin.getIslandManager();
        WorldManager worldManager = this.plugin.getWorldManager();
        IslandLevelManager levellingManager = this.plugin.getLevellingManager();
        if (!worldManager.isIslandWorld(block.getWorld())) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && worldManager.getIslandWorld(block.getWorld()) == IslandWorld.NETHER
                && (XMaterial.WATER_BUCKET.isSimilar(event.getItem())
                || XMaterial.TROPICAL_FISH_BUCKET.isSimilar(event.getItem())
                || XMaterial.COD_BUCKET.isSimilar(event.getItem())
                || XMaterial.SALMON_BUCKET.isSimilar(event.getItem())
                || XMaterial.PUFFERFISH_BUCKET.isSimilar(event.getItem()))) {
            Location blockLoc = block.getLocation();

            Island island = islandManager.getIslandAtLocation(blockLoc);

            // Check permissions.
            if (!this.plugin.getPermissionManager().processPermission(event, player, island)) {
                return;
            }

            if (island == null) {
                event.setCancelled(true);
                return;
            }

            if (levellingManager.isScanning(island)) {
                this.plugin.getMessageManager().sendMessage(player,
                        this.plugin.getLanguage().getString("Command.Island.Level.Scanning.BlockPlacing.Message"));
                event.setCancelled(true);
                return;
            }

            Optional<XMaterial> type = CompatibleMaterial.getMaterial(block.getType());

            if (type.get().name().contains("SLAB")
                    || type.get() == XMaterial.BROWN_MUSHROOM
                    || type.get() == XMaterial.RED_MUSHROOM
                    || type.get() == XMaterial.CHEST
                    || type.get() == XMaterial.ENDER_CHEST
                    || type.get() == XMaterial.TRAPPED_CHEST
                    || type.get() == XMaterial.END_PORTAL
                    || type.get() == XMaterial.ENCHANTING_TABLE) {
                event.setCancelled(true);
                return;
            }

            FileManager.Config config = this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "config.yml"));
            FileConfiguration configLoad = config.getFileConfiguration();
            IslandWorld world = worldManager.getIslandWorld(block.getWorld());

            // Check spawn protection
            if (configLoad.getBoolean("Island.Spawn.Protection")) {
                boolean isObstructing = LocationUtil.isLocationAffectingIslandSpawn(blockLoc, island, world);   // Directly on the block

                if (isObstructing) {
                    this.plugin.getMessageManager().sendMessage(player, this.plugin.getLanguage().getString("Island.SpawnProtection.Place.Message"));
                    this.plugin.getSoundManager().playSound(player, XSound.ENTITY_VILLAGER_NO);

                    event.setCancelled(true);
                    return;
                }
            }

            BlockLimitation limits = this.plugin.getLimitationHandler().getInstance(BlockLimitation.class);

            long limit = limits.getBlockLimit(player, Material.WATER);

            if (limits.isBlockLimitExceeded(CompatibleMaterial.getMaterial(event.getItem().getType()).get(), block.getLocation(), limit)) {

                this.plugin.getMessageManager().sendMessage(player, this.plugin.getLanguage().getString("Island.Limit.Block.Exceeded.Message")
                        .replace("%type", WordUtils.capitalizeFully(event.getItem().getType().name().replace("_", " "))).replace("%limit", NumberUtils.formatNumber(limit)));
                this.plugin.getSoundManager().playSound(player, XSound.ENTITY_VILLAGER_NO);

                event.setCancelled(true);
                return;
            }

            if (configLoad.getBoolean("Island.Nether.AllowNetherWater", false)) {
                block.setType(Material.WATER, true);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        org.bukkit.block.Block block = event.getClickedBlock();

        if (block != null && !this.plugin.getWorldManager().isIslandWorld(block.getWorld())) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();
        StackableManager stackableManager = this.plugin.getStackableManager();
        IslandLevelManager levellingManager = this.plugin.getLevellingManager();

        Island island = (block != null) ?
                islandManager.getIslandAtLocation(block.getLocation()) :
                islandManager.getIslandAtLocation(player.getLocation());
        if (island == null) {
            event.setCancelled(true);
            return;
        }

        // Check if the player is previewing an island and cancel interactions.
        PlayerData data = this.plugin.getPlayerDataManager().getPlayerData(player);
        if (data != null && data.isPreview()) {
                // Cancel the interaction
                event.setCancelled(true);
                this.plugin.getMessageManager().sendMessage(player, "You cannot interact with blocks while previewing an island.");
        }

        // Check permissions.
        if (!this.plugin.getPermissionManager().processPermission(event, player, island)) {
            return;
        }

        Optional<XMaterial> material = block == null ? Optional.empty() : CompatibleMaterial.getMaterial(block.getType());

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Optional<XMaterial> blockType = CompatibleMaterial.getMaterial(event.getClickedBlock().getType());
            final XMaterial heldType;
            final ItemStack item = event.getItem();

            if (item != null && !XMaterial.AIR.isSimilar(item)) {
                heldType = CompatibleMaterial.getMaterial(event.getItem().getType()).get();
            } else {
                heldType = XMaterial.AIR;
            }

            if (stackableManager != null && block != null && stackableManager.isStacked(block.getLocation())) {
                if (blockType.get() == XMaterial.DRAGON_EGG) {
                    event.setCancelled(true);
                }
            }

            if (stackableManager != null && stackableManager.isStackableMaterial(heldType) && blockType.get() == heldType
                    && !player.isSneaking() && this.plugin.getPermissionManager().hasPermission(player, island, "Place")
                    && (!this.plugin.getConfiguration().getBoolean("Island.Stackable.RequirePermission")
                    || player.hasPermission("fabledskyblock.stackable"))) {

                if (levellingManager.isScanning(island)) {
                    this.plugin.getMessageManager().sendMessage(player,
                            this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "language.yml")).getFileConfiguration().getString("Command.Island.Level.Scanning.BlockPlacing.Message"));
                    event.setCancelled(true);
                    return;
                }

                BlockLimitation limits = this.plugin.getLimitationHandler().getInstance(BlockLimitation.class);

                long limit = limits.getBlockLimit(player, block.getType());

                if (limits.isBlockLimitExceeded(block, limit)) {
                    this.plugin.getMessageManager().sendMessage(player,
                            this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "language.yml")).getFileConfiguration().getString("Island.Limit.Block.Exceeded.Message")
                                    .replace("%type", WordUtils.capitalizeFully(material.get().name().replace("_", " "))).replace("%limit", NumberUtils.formatNumber(limit)));
                    this.plugin.getSoundManager().playSound(player, XSound.ENTITY_VILLAGER_NO);

                    event.setCancelled(true);
                    return;
                }

                Location location = event.getClickedBlock().getLocation();
                Stackable stackable = stackableManager.getStack(location, blockType.get());
                int itemAmount = event.getItem().getAmount();

                FileManager.Config config = this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "config.yml"));
                FileConfiguration configLoad = config.getFileConfiguration();

                if (configLoad.getBoolean("Island.Stackable.Limit.Enable")) {
                    // Add block to stackable
                    int maxStackSize = getStackLimit(player, material.get());

                    //Fixing Dupe that was found when stack hits the limit and player gets whole (applied stack) back.
                    if (stackable == null) {
                        stackable = new Stackable(location, blockType.get(), maxStackSize);
                        stackableManager.addStack(stackable);

                        int spaceAvailable = stackable.getMaxSize() - stackable.getSize();
                        if (itemAmount <= spaceAvailable) {
                            stackable.setSize(stackable.getSize() + itemAmount);
                        } else {
                            stackable.setSize(stackable.getMaxSize());
                            int excessAmount = itemAmount - spaceAvailable;
                            // Return the excess items to the player
                            player.getInventory().addItem(new ItemStack(blockType.get().parseMaterial(), excessAmount));
                            event.setCancelled(true);
                        }
                    } else {
                        stackable.setMaxSize(maxStackSize);

                        int spaceAvailable = stackable.getMaxSize() - stackable.getSize();
                        if (itemAmount <= spaceAvailable) {
                            stackable.setSize(stackable.getSize() + itemAmount);
                        } else {
                            stackable.setSize(stackable.getMaxSize());
                            int excessAmount = itemAmount - spaceAvailable;
                            // Return the excess items to the player
                            player.getInventory().addItem(new ItemStack(blockType.get().parseMaterial(), excessAmount));
                            event.setCancelled(true);
                        }
                    }


                    // Disables interaction
                    event.setCancelled(true);

                } else {
                    if (stackable == null) {
                        stackableManager.addStack(stackable = new Stackable(location, blockType.get()));
                        stackable.setSize(itemAmount + 1);
                    } else {
                        stackable.setSize(stackable.getSize() + itemAmount);
                    }

                    event.setCancelled(true);
                }

                if (LogManager.isEnabled() && material != null) {
                    LogManager.logPlacement(player, block);
                }

                if (player.getGameMode() != GameMode.CREATIVE) {
                    ItemUtils.takeActiveItem(player, CompatibleHand.getHand(event), itemAmount);
                }

                if (!configLoad.getBoolean("Island.Block.Level.Enable")) {
                    return;
                }

                long materialAmmount = 0;
                IslandLevel level = island.getLevel();

                if (material == null) {
                    return;
                }

                if (level.hasMaterial(material.get().name())) {
                    materialAmmount = level.getMaterialAmount(material.get().name());
                }

                level.setMaterialAmount(material.get().name(), materialAmmount + itemAmount);
                return;

            }

            // Check if the clicked block is outside the border.
            WorldManager worldManager = this.plugin.getWorldManager();
            org.bukkit.block.Block clickedBlock = event.getClickedBlock();
            IslandWorld world = worldManager.getIslandWorld(clickedBlock.getWorld());
            if (!islandManager.isLocationAtIsland(island, clickedBlock.getLocation(), world)) {
                event.setCancelled(true);
                return;
            }

            if (player.getGameMode() == GameMode.SURVIVAL
                    && material.get() == XMaterial.OBSIDIAN
                    && event.getItem() != null
                    && !XMaterial.AIR.isSimilar(event.getItem())
                    && XMaterial.BUCKET.isSimilar(event.getItem())) {
                if (this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "config.yml"))
                        .getFileConfiguration().getBoolean("Island.Block.Obsidian.Enable")) {

                    this.plugin.getSoundManager().playSound(block.getLocation(), XSound.BLOCK_FIRE_EXTINGUISH);
                    XBlock.setType(block, XMaterial.AIR);

                    ItemUtils.takeActiveItem(player, CompatibleHand.getHand(event));
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(XMaterial.LAVA_BUCKET.parseItem());
                    for (ItemStack i : overflow.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), i);
                    }

                    event.setCancelled(true);
                    return;
                }
            } else if (material.get() == XMaterial.END_PORTAL_FRAME) {
                if (this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "config.yml"))
                        .getFileConfiguration().getBoolean("Island.Block.EndFrame.Enable")) {

                    if (CompatibleHand.getHand(event) == CompatibleHand.OFF_HAND) {
                        return;
                    }

                    ItemStack is = event.getPlayer().getItemInHand();
                    boolean hasEye = ((block.getData() >> 2) & 1) == 1;

                    if (XMaterial.AIR.isSimilar(is)) {
                        int size = 1;

                        if (stackableManager != null && stackableManager.isStacked(block.getLocation())) {
                            Stackable stackable = stackableManager.getStack(block.getLocation(), XMaterial.END_PORTAL_FRAME);
                            stackable.takeOne();

                            if (stackable.getSize() <= 1) {
                                stackableManager.removeStack(stackable);
                            }

                            size = stackable.getSize();
                        } else {
                            XBlock.setType(block, XMaterial.AIR);
                        }

                        player.getInventory().addItem(new ItemStack(XMaterial.END_PORTAL_FRAME.parseMaterial(), 1));
                        if (hasEye && size == 1) {
                            player.getInventory().addItem(new ItemStack(XMaterial.ENDER_EYE.parseMaterial(), 1));
                        }
                        player.updateInventory();

                        FileManager.Config config = this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "config.yml"));
                        FileConfiguration configLoad = config.getFileConfiguration();

                        if (configLoad.getBoolean("Island.Block.Level.Enable")) {
                            XMaterial materials = XMaterial.END_PORTAL_FRAME;
                            IslandLevel level = island.getLevel();

                            if (level.hasMaterial(materials.name())) {
                                long materialAmount = level.getMaterialAmount(materials.name());

                                if (materialAmount - 1 <= 0) {
                                    level.removeMaterial(materials.name());
                                } else {
                                    level.setMaterialAmount(materials.name(), materialAmount - 1);
                                }
                            }
                        }

                        this.plugin.getSoundManager().playSound(player, XSound.ENTITY_CHICKEN_EGG, 10, 10);

                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private int getStackLimit(Player player, XMaterial materials) {
        String maxSizePermission = "fabledskyblock.stackable." + materials.name().toLowerCase() + ".maxsize.";
        for (PermissionAttachmentInfo attachmentInfo : player.getEffectivePermissions()) {
            if (attachmentInfo.getPermission().startsWith(maxSizePermission)) {
                String permission = attachmentInfo.getPermission();
                return Integer.parseInt(permission.substring(permission.lastIndexOf(".") + 1));
            }
        }

        return 5000;
    }

    @EventHandler
    public void onPlayerInteractStructure(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        MessageManager messageManager = this.plugin.getMessageManager();
        SoundManager soundManager = this.plugin.getSoundManager();

        if (event.getItem() != null) {
            try {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    ItemStack structureTool = StructureUtil.getTool();

                    if ((event.getItem().getType() == structureTool.getType()) && (event.getItem().hasItemMeta()) && (event.getItem().getItemMeta().getDisplayName()
                            .equals(structureTool.getItemMeta().getDisplayName()))) {
                        if (player.hasPermission("fabledskyblock.admin.structure.selection") || player.hasPermission("fabledskyblock.admin.structure.*") || player
                                .hasPermission("fabledskyblock.admin.*")
                                || player.hasPermission("fabledskyblock.*")) {
                            event.setCancelled(true);

                            this.plugin.getPlayerDataManager().getPlayerData(player).getArea().setPosition(1, event.getClickedBlock().getLocation());

                            messageManager.sendMessage(player,
                                    this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "language.yml")).getFileConfiguration().getString("Island.Structure.Tool.Position.Message")
                                            .replace("%position", "1"));
                            soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);
                        }
                    }
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    ItemStack structureTool = StructureUtil.getTool();

                    if ((event.getItem().getType() == structureTool.getType()) && (event.getItem().hasItemMeta()) && (event.getItem().getItemMeta().getDisplayName()
                            .equals(structureTool.getItemMeta().getDisplayName()))) {
                        if (player.hasPermission("fabledskyblock.admin.structure.selection") || player.hasPermission("fabledskyblock.admin.structure.*") || player
                                .hasPermission("fabledskyblock.admin.*")
                                || player.hasPermission("fabledskyblock.*")) {
                            event.setCancelled(true);

                            this.plugin.getPlayerDataManager().getPlayerData(player).getArea().setPosition(2, event.getClickedBlock().getLocation());

                            messageManager.sendMessage(player,
                                    this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "language.yml")).getFileConfiguration().getString("Island.Structure.Tool.Position.Message")
                                            .replace("%position", "2"));
                            soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        org.bukkit.entity.Entity entity = event.getRightClicked();

        IslandManager islandManager = this.plugin.getIslandManager();

        if (!this.plugin.getWorldManager().isIslandWorld(entity.getWorld())) {
            return;
        }

        Island island = islandManager.getIslandAtLocation(entity.getLocation());

        // Check permissions.
        if (!this.plugin.getPermissionManager().processPermission(event, player, island)) {
            return;
        }
    }

    @EventHandler
    public void onPlayerDamageVehicle(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();

        Player player = (Player) event.getAttacker();

        if (!this.plugin.getWorldManager().isIslandWorld(event.getVehicle().getWorld())) {
            return;
        }

        Island island = islandManager.getIslandAtLocation(event.getVehicle().getLocation());

        // Check permissions.
        if (!this.plugin.getPermissionManager().processPermission(event, player, island)) {
            return;
        }
    }

    @EventHandler
    public void onPlayerDestroyVehicle(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();

        Player player = (Player) event.getAttacker();

        if (!this.plugin.getWorldManager().isIslandWorld(event.getVehicle().getWorld())) {
            return;
        }

        Island island = islandManager.getIslandAtLocation(event.getVehicle().getLocation());

        // Check permissions.
        if (!this.plugin.getPermissionManager().processPermission(event, player, island)) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (this.plugin.getStackableManager() != null && this.plugin.getStackableManager().isStacked(event.getRightClicked().getLocation().getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
