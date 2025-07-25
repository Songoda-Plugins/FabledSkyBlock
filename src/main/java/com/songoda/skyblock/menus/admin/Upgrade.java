package com.songoda.skyblock.menus.admin;

import com.songoda.core.compatibility.MajorServerVersion;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.gui.AnvilGui;
import com.songoda.skyblock.utils.version.PotionUtils;
import com.songoda.third_party.com.cryptomorin.xseries.XItemFlag;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.third_party.com.cryptomorin.xseries.XPotion;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.core.utils.NumberUtils;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.config.FileManager;
import com.songoda.skyblock.config.FileManager.Config;
import com.songoda.skyblock.message.MessageManager;
import com.songoda.skyblock.placeholder.Placeholder;
import com.songoda.skyblock.playerdata.PlayerData;
import com.songoda.skyblock.playerdata.PlayerDataManager;
import com.songoda.skyblock.sound.SoundManager;
import com.songoda.skyblock.upgrade.UpgradeManager;
import com.songoda.skyblock.utils.item.nInventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Upgrade {
    private static Upgrade instance;

    public static Upgrade getInstance() {
        if (instance == null) {
            instance = new Upgrade();
        }

        return instance;
    }

    @SuppressWarnings("deprecation")
    public void open(Player player) {
        SkyBlock plugin = SkyBlock.getInstance();

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        MessageManager messageManager = plugin.getMessageManager();
        UpgradeManager upgradeManager = plugin.getUpgradeManager();
        SoundManager soundManager = plugin.getSoundManager();
        FileManager fileManager = plugin.getFileManager();

        if (playerDataManager.hasPlayerData(player) && playerDataManager.getPlayerData(player).getViewer() != null) {
            FileConfiguration configLoad = plugin.getLanguage();
            Viewer viewer = (Upgrade.Viewer) playerDataManager.getPlayerData(player).getViewer();

            if (viewer == null || viewer.getType() == Upgrade.Viewer.Type.UPGRADES) {
                nInventoryUtil nInv = new nInventoryUtil(player, event -> {
                    if (!(player.hasPermission("fabledskyblock.admin.upgrade") || player.hasPermission("fabledskyblock.admin.*")
                            || player.hasPermission("fabledskyblock.*"))) {
                        messageManager.sendMessage(player,
                                configLoad.getString("Island.Admin.Upgrade.Permission.Message"));
                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                        return;
                    }

                    ItemStack is = event.getItem();
                    com.songoda.skyblock.upgrade.Upgrade upgrade = null;

                    if ((is.getType() == XMaterial.OAK_FENCE_GATE.parseMaterial()) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Exit.Displayname"))))) {
                        soundManager.playSound(player, XSound.BLOCK_CHEST_CLOSE);

                        return;
                    } else if ((is.getType() == Material.POTION) && (is.hasItemMeta())) {
                        if (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Speed.Displayname")))) {
                            upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SPEED)
                                    .get(0);
                            viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.SPEED);
                        } else if (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Jump.Displayname")))) {
                            upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.JUMP)
                                    .get(0);
                            viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.JUMP);
                        }
                    } else if ((is.getType() == XMaterial.WHEAT_SEEDS.parseMaterial()) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Crop.Displayname"))))) {
                        upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.CROP)
                                .get(0);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.CROP);
                    } else if ((is.getType() == Material.FEATHER) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Fly.Displayname"))))) {
                        upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.FLY)
                                .get(0);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.FLY);
                    } else if ((is.getType() == Material.SPIDER_EYE) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Drops.Displayname"))))) {
                        upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.DROPS)
                                .get(0);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.DROPS);
                    } else if ((is.getType() == Material.BEACON) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Size.Displayname"))))) {
                        viewer.setType(Viewer.Type.SIZE);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE);

                        soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                    } else if ((is.getType() == Material.BOOKSHELF) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName().equals(plugin.formatText(
                            configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Members.Displayname"))))) {
                        viewer.setType(Viewer.Type.MEMBERS);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS);

                        soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                    } else if ((is.getType() == XMaterial.SPAWNER.parseMaterial()) && (is.hasItemMeta())
                            && (is.getItemMeta().getDisplayName()
                            .equals(plugin.formatText(configLoad
                                    .getString("Menu.Admin.Upgrade.Upgrades.Item.Spawner.Displayname"))))) {
                        upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SPAWNER)
                                .get(0);
                        viewer.setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.SPAWNER);
                    }

                    if (upgrade != null) {
                        if (event.getClick() == ClickType.LEFT) {
                            upgrade.setEnabled(!upgrade.isEnabled());

                            if (playerDataManager.hasPlayerData(player)) {
                                com.songoda.skyblock.upgrade.Upgrade.Type upgradeType = ((Viewer) playerDataManager
                                        .getPlayerData(player).getViewer()).getUpgrade();

                                boolean enabled = upgrade.isEnabled();
                                Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin,
                                        () -> {
                                            Config config = fileManager.getConfig(new File(plugin.getDataFolder(), "upgrades.yml"));
                                            FileConfiguration configLoad1 = config.getFileConfiguration();

                                            configLoad1.set("Upgrades." + upgradeType.getFriendlyName() + ".Enable", enabled);

                                            try {
                                                configLoad1.save(config.getFile());
                                            } catch (IOException ex) {
                                                ex.printStackTrace();
                                            }
                                        });
                            }

                            soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                        } else if (event.getClick() == ClickType.RIGHT) {
                            soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> {
                                AnvilGui gui = new AnvilGui(player);
                                gui.setAction(event1 -> {

                                    if (!(player.hasPermission("fabledskyblock.admin.upgrade")
                                            || player.hasPermission("fabledskyblock.admin.*")
                                            || player.hasPermission("fabledskyblock.*"))) {
                                        messageManager.sendMessage(player, configLoad
                                                .getString("Island.Admin.Upgrade.Permission.Message"));
                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                        return;
                                    } else if (!(gui.getInputText().matches("[0-9]+")
                                            || gui.getInputText().matches("([0-9]*)\\.([0-9]{1,2}$)"))) {
                                        messageManager.sendMessage(player, configLoad
                                                .getString("Island.Admin.Upgrade.Numerical.Message"));
                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                        player.closeInventory();

                                        return;
                                    }

                                    if (playerDataManager.hasPlayerData(player)) {
                                        double upgradeCost = Double.parseDouble(gui.getInputText());
                                        com.songoda.skyblock.upgrade.Upgrade.Type upgradeType = ((Viewer) playerDataManager
                                                .getPlayerData(player).getViewer()).getUpgrade();

                                        com.songoda.skyblock.upgrade.Upgrade upgrade1 = upgradeManager.getUpgrades(upgradeType).get(0);
                                        upgrade1.setCost(upgradeCost);
                                        soundManager.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING);

                                        Bukkit.getServer().getScheduler().runTaskAsynchronously(plugin,
                                                () -> {
                                                    Config config = fileManager.getConfig(new File(
                                                            plugin.getDataFolder(), "upgrades.yml"));
                                                    FileConfiguration configLoad1 = config
                                                            .getFileConfiguration();

                                                    configLoad1.set(
                                                            "Upgrades." + upgradeType.getFriendlyName() + ".Cost",
                                                            upgradeCost);

                                                    try {
                                                        configLoad1.save(config.getFile());
                                                    } catch (IOException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                });

                                        Bukkit.getServer().getScheduler()
                                                .runTaskLater(plugin, () -> open(player), 1L);
                                    }
                                    player.closeInventory();
                                });

                                ItemStack is1 = new ItemStack(Material.NAME_TAG);
                                ItemMeta im = is1.getItemMeta();
                                im.setDisplayName(
                                        configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Word.Enter"));
                                is1.setItemMeta(im);

                                gui.setInput(is1);
                                plugin.getGuiManager().showGUI(player, gui);

                            }, 1L);
                        } else {
                            event.setWillClose(false);
                            event.setWillDestroy(false);
                        }
                    }
                });

                ItemStack speedPotion = new ItemStack(Material.POTION);
                ItemStack jumpPotion = new ItemStack(Material.POTION);
                com.songoda.skyblock.upgrade.Upgrade upgrade;

                if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
                    PotionMeta pm = (PotionMeta) speedPotion.getItemMeta();
                    PotionUtils.setPotionData_V1_13(pm, XPotion.SPEED.getPotionType());
                    speedPotion.setItemMeta(pm);
                } else {
                    speedPotion = new ItemStack(Material.POTION, 1, (short) 8194);
                }

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SPEED).get(0);
                nInv.addItem(nInv.createItem(speedPotion,
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Speed.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Speed.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, new ItemFlag[]{XItemFlag.HIDE_ADDITIONAL_TOOLTIP.get()}), 0);

                if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
                    PotionMeta pm = (PotionMeta) jumpPotion.getItemMeta();
                    PotionUtils.setPotionData_V1_13(pm, XPotion.JUMP_BOOST.getPotionType());
                    jumpPotion.setItemMeta(pm);
                } else {
                    jumpPotion = new ItemStack(Material.POTION, 1, (short) 8203);
                }

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.JUMP).get(0);
                nInv.addItem(nInv.createItem(jumpPotion,
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Jump.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Jump.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, new ItemFlag[]{XItemFlag.HIDE_ADDITIONAL_TOOLTIP.get()}), 1);

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.CROP).get(0);
                nInv.addItem(nInv.createItem(XMaterial.WHEAT_SEEDS.parseItem(),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Crop.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Crop.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, null), 2);

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.FLY).get(0);
                nInv.addItem(nInv.createItem(new ItemStack(Material.FEATHER),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Fly.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Fly.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, null), 3);

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.DROPS).get(0);
                nInv.addItem(nInv.createItem(new ItemStack(Material.SPIDER_EYE),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Drops.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Drops.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, null), 4);

                // Size
                List<com.songoda.skyblock.upgrade.Upgrade> upgradesSize = upgradeManager
                        .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE);
                int upgradeTiersSize = 0;

                if (upgradesSize != null) {
                    upgradeTiersSize = upgradesSize.size();
                }

                nInv.addItem(nInv.createItem(new ItemStack(Material.BEACON),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Size.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Size.Lore"),
                        new Placeholder[]{new Placeholder("%tiers", "" + upgradeTiersSize)}, null, null), 5);

                // Members
                List<com.songoda.skyblock.upgrade.Upgrade> upgradesMembers = upgradeManager
                        .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS);
                int upgradeTiersMembers = 0;

                if (upgradesMembers != null) {
                    upgradeTiersMembers = upgradesMembers.size();
                }

                nInv.addItem(nInv.createItem(XMaterial.BOOKSHELF.parseItem(), plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Members.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Members.Lore"),
                        new Placeholder[]{new Placeholder("%tiers", "" + upgradeTiersMembers)}, null, null), 4);

                upgrade = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SPAWNER).get(0);
                nInv.addItem(nInv.createItem(XMaterial.SPAWNER.parseItem(),
                        plugin.formatText(configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Spawner.Displayname")),
                        configLoad.getStringList("Menu.Admin.Upgrade.Upgrades.Item.Spawner.Lore"),
                        new Placeholder[]{
                                new Placeholder("%cost", NumberUtils.formatNumber(upgrade.getCost())),
                                new Placeholder("%status", getStatus(upgrade))},
                        null, null), 6);

                nInv.addItem(nInv.createItem(XMaterial.OAK_FENCE_GATE.parseItem(),
                        plugin.formatText(configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Exit.Displayname")),
                        null, null, null, null), 8);

                nInv.setTitle(plugin.formatText(configLoad.getString("Menu.Admin.Upgrade.Upgrades.Title")));
                nInv.setRows(1);

                Bukkit.getServer().getScheduler().runTask(plugin, nInv::open);
            } else if (viewer.getType() == Upgrade.Viewer.Type.SIZE) {
                nInventoryUtil nInv = new nInventoryUtil(player, event -> {
                    if (!(player.hasPermission("fabledskyblock.admin.upgrade") || player.hasPermission("fabledskyblock.admin.*")
                            || player.hasPermission("fabledskyblock.*"))) {
                        messageManager.sendMessage(player,
                                configLoad.getString("Island.Admin.Upgrade.Permission.Message"));
                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                        return;
                    }

                    if (playerDataManager.hasPlayerData(player)) {
                        PlayerData playerData = playerDataManager.getPlayerData(player);
                        ItemStack is = event.getItem();

                        if ((is.getType() == XMaterial.OAK_FENCE_GATE.parseMaterial()) && (is.hasItemMeta())
                                && (is.getItemMeta().getDisplayName()
                                .equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Size.Item.Return.Displayname"))))) {
                            playerData.setViewer(new Viewer(Viewer.Type.UPGRADES, null));
                            soundManager.playSound(player, XSound.ENTITY_ARROW_HIT);

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                        } else if ((is.getType() == Material.PAINTING) && (is.hasItemMeta()) && (is.getItemMeta()
                                .getDisplayName().equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Size.Item.Information.Displayname"))))) {
                            List<com.songoda.skyblock.upgrade.Upgrade> upgrades = upgradeManager
                                    .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE);

                            if (upgrades != null && upgrades.size() >= 5) {
                                messageManager.sendMessage(player,
                                        configLoad.getString("Island.Admin.Upgrade.Tier.Limit.Message"));
                                soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                event.setWillClose(false);
                                event.setWillDestroy(false);
                            } else {
                                soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                        () -> {
                                            AnvilGui gui = new AnvilGui(player);
                                            gui.setAction(event1 -> {

                                                if (playerDataManager.hasPlayerData(player)
                                                        && playerDataManager
                                                        .getPlayerData(player) != null) {
                                                    if (!gui.getInputText().matches("[0-9]+")) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Numerical.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        player.closeInventory();

                                                        return;
                                                    } else {
                                                        List<com.songoda.skyblock.upgrade.Upgrade> upgrades1 = upgradeManager
                                                                .getUpgrades(
                                                                        com.songoda.skyblock.upgrade.Upgrade.Type.SIZE);

                                                        if (upgrades1 != null && upgrades1.size() >= 5) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Limit.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler()
                                                                    .runTaskLater(plugin,
                                                                            () -> open(player), 1L);

                                                            return;
                                                        }
                                                    }

                                                    int size = Integer.valueOf(gui.getInputText());

                                                    if (size > 1000) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Tier.Size.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        event.setWillClose(false);
                                                        event.setWillDestroy(false);

                                                        return;
                                                    } else if (upgradeManager.hasUpgrade(
                                                            com.songoda.skyblock.upgrade.Upgrade.Type.SIZE,
                                                            size)) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Tier.Exist.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        player.closeInventory();

                                                        return;
                                                    }

                                                    soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                    upgradeManager.addUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE, size);

                                                    Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                }
                                                player.closeInventory();
                                            });

                                            ItemStack is12 = new ItemStack(Material.NAME_TAG);
                                            ItemMeta im = is12.getItemMeta();
                                            im.setDisplayName(configLoad.getString("Menu.Admin.Upgrade.Size.Item.Word.Size.Enter"));
                                            is12.setItemMeta(im);

                                            gui.setInput(is12);
                                            plugin.getGuiManager().showGUI(player, gui);

                                        }, 1L);
                            }
                        } else if ((is.getType() == XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial())
                                && (is.hasItemMeta())
                                && (is.getItemMeta().getDisplayName()
                                .equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Size.Item.Barrier.Displayname"))))) {
                            soundManager.playSound(player, XSound.BLOCK_GLASS_BREAK);

                            event.setWillClose(false);
                            event.setWillDestroy(false);
                        } else if ((is.getType() == Material.PAPER) && (is.hasItemMeta())) {
                            int slot = event.getSlot();
                            int tier = slot - 3;

                            com.songoda.skyblock.upgrade.Upgrade upgrade = upgradeManager
                                    .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE).get(tier);

                            if (upgrade != null) {
                                if (event.getClick() == ClickType.LEFT) {
                                    soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                    Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                            () -> {
                                                AnvilGui gui = new AnvilGui(player);
                                                gui.setAction(event1 -> {

                                                    if (!(player.hasPermission("fabledskyblock.admin.upgrade")
                                                            || player.hasPermission("fabledskyblock.admin.*")
                                                            || player.hasPermission("fabledskyblock.*"))) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Permission.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        return;
                                                    }

                                                    if (playerDataManager.hasPlayerData(player)
                                                            && playerDataManager
                                                            .getPlayerData(player) != null) {
                                                        if (!gui.getInputText().matches("[0-9]+")) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Numerical.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            player.closeInventory();

                                                            return;
                                                        } else if (upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE).get(tier) == null) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Selected.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);

                                                            return;
                                                        }

                                                        int size = Integer.parseInt(gui.getInputText());

                                                        if (size > 1000) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Size.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            event.setWillClose(false);
                                                            event.setWillDestroy(false);

                                                            return;
                                                        } else if (upgradeManager.hasUpgrade(
                                                                com.songoda.skyblock.upgrade.Upgrade.Type.SIZE,
                                                                size)) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Exist.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            event.setWillClose(false);
                                                            event.setWillDestroy(false);

                                                            return;
                                                        }

                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                        upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE).get(tier).setValue(size);
                                                        fileManager
                                                                .getConfig(new File(plugin.getDataFolder(), "upgrades.yml"))
                                                                .getFileConfiguration()
                                                                .set("Upgrades.Size." + tier + ".Value", size);

                                                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                    }

                                                    player.closeInventory();
                                                });

                                                ItemStack is13 = new ItemStack(Material.NAME_TAG);
                                                ItemMeta im = is13.getItemMeta();
                                                im.setDisplayName(configLoad.getString(
                                                        "Menu.Admin.Upgrade.Size.Item.Word.Size.Enter"));
                                                is13.setItemMeta(im);

                                                gui.setInput(is13);
                                                plugin.getGuiManager().showGUI(player, gui);

                                            }, 1L);

                                    return;
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    soundManager.playSound(player, XSound.ENTITY_IRON_GOLEM_ATTACK);
                                    upgradeManager.removeUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE,
                                            upgrade.getCost(), upgrade.getValue());
                                } else if (event.getClick() == ClickType.RIGHT) {
                                    soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                    Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                            () -> {
                                                AnvilGui gui = new AnvilGui(player);
                                                gui.setAction(event1 -> {

                                                    if (!(player.hasPermission("fabledskyblock.admin.upgrade")
                                                            || player.hasPermission("fabledskyblock.admin.*")
                                                            || player.hasPermission("fabledskyblock.*"))) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Permission.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        return;
                                                    }

                                                    if (playerDataManager.hasPlayerData(player)
                                                            && playerDataManager
                                                            .getPlayerData(player) != null) {
                                                        if (!(gui.getInputText().matches("[0-9]+")
                                                                || gui.getInputText().matches(
                                                                "([0-9]*)\\.([0-9]{2}$)"))) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Numerical.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            player.closeInventory();

                                                            return;
                                                        } else if (upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE).get(tier) == null) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Selected.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);

                                                            return;
                                                        }

                                                        double cost = Double.parseDouble(gui.getInputText());

                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                        upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE).get(tier).setCost(cost);
                                                        fileManager
                                                                .getConfig(new File(plugin.getDataFolder(), "upgrades.yml"))
                                                                .getFileConfiguration()
                                                                .set("Upgrades.Size." + tier + ".Cost", cost);

                                                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                    }

                                                    player.closeInventory();
                                                });

                                                ItemStack is14 = new ItemStack(Material.NAME_TAG);
                                                ItemMeta im = is14.getItemMeta();
                                                im.setDisplayName(configLoad.getString(
                                                        "Menu.Admin.Upgrade.Size.Item.Word.Cost.Enter"));
                                                is14.setItemMeta(im);

                                                gui.setInput(is14);
                                                plugin.getGuiManager().showGUI(player, gui);

                                            }, 1L);

                                    return;
                                } else {
                                    event.setWillClose(false);
                                    event.setWillDestroy(false);

                                    return;
                                }
                            }

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                        }
                    }
                });

                nInv.addItem(nInv.createItem(XMaterial.OAK_FENCE_GATE.parseItem(),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Size.Item.Return.Displayname")),
                        null, null, null, null), 0);
                nInv.addItem(nInv.createItem(new

                                        ItemStack(Material.PAINTING),
                                plugin.formatText(
                                        configLoad.getString("Menu.Admin.Upgrade.Size.Item.Information.Displayname")),
                                configLoad.getStringList("Menu.Admin.Upgrade.Size.Item.Information.Lore"), null, null, null),
                        1);
                nInv.addItem(nInv.createItem(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Size.Item.Barrier.Displayname")),
                        null, null, null, null), 2);

                List<com.songoda.skyblock.upgrade.Upgrade> upgrades = upgradeManager
                        .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.SIZE);

                if (upgrades != null) {
                    for (int i = 0; i < 5; i++) {
                        if (upgrades.size() >= i + 1) {
                            com.songoda.skyblock.upgrade.Upgrade upgrade = upgrades.get(i);
                            int tier = i + 1;

                            if (upgrade != null) {
                                nInv.addItem(nInv.createItem(new ItemStack(Material.PAPER, tier),
                                        plugin.formatText(
                                                configLoad.getString("Menu.Admin.Upgrade.Size.Item.Tier.Displayname")
                                                        .replace("%tier", "" + tier)),
                                        configLoad.getStringList("Menu.Admin.Upgrade.Size.Item.Tier.Lore"),
                                        new Placeholder[]{new Placeholder("%size", "" + upgrade.getValue()),
                                                new Placeholder("%cost",
                                                        NumberUtils.formatNumber(upgrade.getCost()))},
                                        null, null), i + 3);
                            }
                        }
                    }
                }

                nInv.setTitle(plugin.formatText(
                        configLoad.getString("Menu.Admin.Upgrade.Size.Title")));
                nInv.setRows(1);

                Bukkit.getServer().

                        getScheduler().

                        runTask(plugin, nInv::open);
            } else if (viewer.getType() == Viewer.Type.MEMBERS) {
                nInventoryUtil nInv = new nInventoryUtil(player, event -> {
                    if (!(player.hasPermission("fabledskyblock.admin.upgrade") || player.hasPermission("fabledskyblock.admin.*")
                            || player.hasPermission("fabledskyblock.*"))) {
                        messageManager.sendMessage(player,
                                configLoad.getString("Island.Admin.Upgrade.Permission.Message"));
                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                        return;
                    }

                    if (playerDataManager.hasPlayerData(player)) {
                        PlayerData playerData = playerDataManager.getPlayerData(player);
                        ItemStack is = event.getItem();

                        if ((is.getType() == XMaterial.OAK_FENCE_GATE.parseMaterial()) && (is.hasItemMeta())
                                && (is.getItemMeta().getDisplayName()
                                .equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Members.Item.Return.Displayname"))))) {
                            playerData.setViewer(new Viewer(Viewer.Type.UPGRADES, null));
                            soundManager.playSound(player, XSound.ENTITY_ARROW_HIT);

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                        } else if ((is.getType() == Material.PAINTING) && (is.hasItemMeta()) && (is.getItemMeta()
                                .getDisplayName().equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Members.Item.Information.Displayname"))))) {
                            List<com.songoda.skyblock.upgrade.Upgrade> upgrades = upgradeManager
                                    .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS);

                            if (upgrades != null && upgrades.size() >= 5) {
                                messageManager.sendMessage(player,
                                        configLoad.getString("Island.Admin.Upgrade.Tier.Limit.Message"));
                                soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                event.setWillClose(false);
                                event.setWillDestroy(false);
                            } else {
                                soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                        () -> {
                                            AnvilGui gui = new AnvilGui(player);
                                            gui.setAction(event1 -> {

                                                if (playerDataManager.hasPlayerData(player)
                                                        && playerDataManager
                                                        .getPlayerData(player) != null) {
                                                    if (!gui.getInputText().matches("[0-9]+")) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Numerical.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        player.closeInventory();

                                                        return;
                                                    } else {
                                                        List<com.songoda.skyblock.upgrade.Upgrade> upgrades1 = upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS);

                                                        if (upgrades1 != null && upgrades1.size() >= 5) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Limit.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);

                                                            return;
                                                        }
                                                    }

                                                    int size = Integer.valueOf(gui.getInputText());

                                                    if (size > 1000) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Tier.Members.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        event.setWillClose(false);
                                                        event.setWillDestroy(false);

                                                        return;
                                                    } else if (upgradeManager.hasUpgrade(
                                                            com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS,
                                                            size)) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Tier.Exist.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        player.closeInventory();

                                                        return;
                                                    }

                                                    soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                    upgradeManager.addUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS, size);

                                                    Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                }

                                                player.closeInventory();

                                            });

                                            ItemStack is12 = new ItemStack(Material.NAME_TAG);
                                            ItemMeta im = is12.getItemMeta();
                                            im.setDisplayName(configLoad
                                                    .getString("Menu.Admin.Upgrade.Members.Item.Word.Members.Enter"));
                                            is12.setItemMeta(im);

                                            gui.setInput(is12);
                                            plugin.getGuiManager().showGUI(player, gui);

                                        }, 1L);
                            }
                        } else if ((is.getType() == XMaterial.BLACK_STAINED_GLASS_PANE.parseMaterial())
                                && (is.hasItemMeta())
                                && (is.getItemMeta().getDisplayName()
                                .equals(plugin.formatText(configLoad
                                        .getString("Menu.Admin.Upgrade.Members.Item.Barrier.Displayname"))))) {
                            soundManager.playSound(player, XSound.BLOCK_GLASS_BREAK);

                            event.setWillClose(false);
                            event.setWillDestroy(false);
                        } else if ((is.getType() == Material.PAPER) && (is.hasItemMeta())) {
                            int slot = event.getSlot();
                            int tier = slot - 3;

                            com.songoda.skyblock.upgrade.Upgrade upgrade = upgradeManager
                                    .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS).get(tier);

                            if (upgrade != null) {
                                if (event.getClick() == ClickType.LEFT) {
                                    soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                    Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                            () -> {
                                                AnvilGui gui = new AnvilGui(player);
                                                gui.setAction(event1 -> {

                                                    if (!(player.hasPermission("fabledskyblock.admin.upgrade")
                                                            || player.hasPermission("fabledskyblock.admin.*")
                                                            || player.hasPermission("fabledskyblock.*"))) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Permission.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        return;
                                                    }

                                                    if (playerDataManager.hasPlayerData(player)
                                                            && playerDataManager
                                                            .getPlayerData(player) != null) {
                                                        if (!gui.getInputText().matches("[0-9]+")) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Numerical.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            player.closeInventory();

                                                            return;
                                                        } else if (upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS).get(tier) == null) {
                                                            messageManager.sendMessage(player, configLoad.getString("Island.Admin.Upgrade.Tier.Selected.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);

                                                            return;
                                                        }

                                                        int size = Integer.parseInt(gui.getInputText());

                                                        if (size > 1000) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Members.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            event.setWillClose(false);
                                                            event.setWillDestroy(false);

                                                            return;
                                                        } else if (upgradeManager.hasUpgrade(
                                                                com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS,
                                                                size)) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Exist.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            event.setWillClose(false);
                                                            event.setWillDestroy(false);

                                                            return;
                                                        }

                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                        upgradeManager.getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS).get(tier).setValue(size);
                                                        fileManager
                                                                .getConfig(new File(plugin.getDataFolder(), "upgrades.yml"))
                                                                .getFileConfiguration()
                                                                .set("Upgrades.Members." + tier + ".Value", size);

                                                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                    }

                                                    player.closeInventory();
                                                });

                                                ItemStack is13 = new ItemStack(Material.NAME_TAG);
                                                ItemMeta im = is13.getItemMeta();
                                                im.setDisplayName(configLoad.getString("Menu.Admin.Upgrade.Members.Item.Word.Members.Enter"));
                                                is13.setItemMeta(im);

                                                gui.setInput(is13);
                                                plugin.getGuiManager().showGUI(player, gui);

                                            }, 1L);

                                    return;
                                } else if (event.getClick() == ClickType.MIDDLE) {
                                    soundManager.playSound(player, XSound.ENTITY_IRON_GOLEM_ATTACK);
                                    upgradeManager.removeUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS,
                                            upgrade.getCost(), upgrade.getValue());
                                } else if (event.getClick() == ClickType.RIGHT) {
                                    soundManager.playSound(player, XSound.BLOCK_WOODEN_BUTTON_CLICK_ON);

                                    Bukkit.getServer().getScheduler().runTaskLater(plugin,
                                            () -> {
                                                AnvilGui gui = new AnvilGui(player);
                                                gui.setAction(event1 -> {

                                                    if (!(player.hasPermission("fabledskyblock.admin.upgrade")
                                                            || player.hasPermission("fabledskyblock.admin.*")
                                                            || player.hasPermission("fabledskyblock.*"))) {
                                                        messageManager.sendMessage(player,
                                                                configLoad.getString(
                                                                        "Island.Admin.Upgrade.Permission.Message"));
                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                        return;
                                                    }

                                                    if (playerDataManager.hasPlayerData(player)
                                                            && playerDataManager
                                                            .getPlayerData(player) != null) {
                                                        if (!(gui.getInputText().matches("[0-9]+")
                                                                || gui.getInputText().matches(
                                                                "([0-9]*)\\.([0-9]{2}$)"))) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Numerical.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            player.closeInventory();

                                                            return;
                                                        } else if (upgradeManager.getUpgrades(
                                                                        com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS)
                                                                .get(tier) == null) {
                                                            messageManager.sendMessage(player,
                                                                    configLoad.getString(
                                                                            "Island.Admin.Upgrade.Tier.Selected.Message"));
                                                            soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                                                            Bukkit.getServer().getScheduler()
                                                                    .runTaskLater(plugin,
                                                                            () -> open(player), 1L);

                                                            return;
                                                        }

                                                        double cost = Double.parseDouble(gui.getInputText());

                                                        soundManager.playSound(player, XSound.BLOCK_ANVIL_USE);
                                                        upgradeManager.getUpgrades(
                                                                        com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS)
                                                                .get(tier).setCost(cost);
                                                        fileManager
                                                                .getConfig(
                                                                        new File(plugin.getDataFolder(),
                                                                                "upgrades.yml"))
                                                                .getFileConfiguration()
                                                                .set("Upgrades.Members." + tier + ".Cost",
                                                                        cost);

                                                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                                                    }

                                                    player.closeInventory();
                                                });

                                                ItemStack is14 = new ItemStack(Material.NAME_TAG);
                                                ItemMeta im = is14.getItemMeta();
                                                im.setDisplayName(configLoad.getString(
                                                        "Menu.Admin.Upgrade.Members.Item.Word.Cost.Enter"));
                                                is14.setItemMeta(im);

                                                gui.setInput(is14);
                                                plugin.getGuiManager().showGUI(player, gui);

                                            }, 1L);

                                    return;
                                } else {
                                    event.setWillClose(false);
                                    event.setWillDestroy(false);

                                    return;
                                }
                            }

                            Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                        }
                    }
                });

                nInv.addItem(nInv.createItem(XMaterial.OAK_FENCE_GATE.parseItem(),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Members.Item.Return.Displayname")),
                        null, null, null, null), 0);
                nInv.addItem(nInv.createItem(new ItemStack(Material.PAINTING),
                                plugin.formatText(
                                        configLoad.getString("Menu.Admin.Upgrade.Members.Item.Information.Displayname")),
                                configLoad.getStringList("Menu.Admin.Upgrade.Members.Item.Information.Lore"), null, null, null),
                        1);
                nInv.addItem(nInv.createItem(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(),
                        plugin.formatText(
                                configLoad.getString("Menu.Admin.Upgrade.Members.Item.Barrier.Displayname")),
                        null, null, null, null), 2);

                List<com.songoda.skyblock.upgrade.Upgrade> upgrades = upgradeManager
                        .getUpgrades(com.songoda.skyblock.upgrade.Upgrade.Type.MEMBERS);

                if (upgrades != null) {
                    for (int i = 0; i < 5; i++) {
                        if (upgrades.size() >= i + 1) {
                            com.songoda.skyblock.upgrade.Upgrade upgrade = upgrades.get(i);
                            int tier = i + 1;

                            if (upgrade != null) {
                                nInv.addItem(nInv.createItem(new ItemStack(Material.PAPER, tier),
                                        plugin.formatText(
                                                configLoad.getString("Menu.Admin.Upgrade.Members.Item.Tier.Displayname")
                                                        .replace("%tier", "" + tier)),
                                        configLoad.getStringList("Menu.Admin.Upgrade.Members.Item.Tier.Lore"),
                                        new Placeholder[]{new Placeholder("%maxMembers", "" + upgrade.getValue()),
                                                new Placeholder("%cost",
                                                        NumberUtils.formatNumber(upgrade.getCost()))},
                                        null, null), i + 3);
                            }
                        }
                    }
                }

                nInv.setTitle(plugin.formatText(configLoad.getString("Menu.Admin.Upgrade.Members.Title")));
                nInv.setRows(1);

                Bukkit.getServer().getScheduler().runTask(plugin, nInv::open);
            }
        }
    }

    private String getStatus(com.songoda.skyblock.upgrade.Upgrade upgrade) {
        FileConfiguration configLoad = SkyBlock.getPlugin(SkyBlock.class).getLanguage();

        if (upgrade.isEnabled()) {
            return configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Word.Disable");
        } else {
            return configLoad.getString("Menu.Admin.Upgrade.Upgrades.Item.Word.Enable");
        }
    }

    public static class Viewer {
        private Type type;
        private com.songoda.skyblock.upgrade.Upgrade.Type upgrade;

        public Viewer(Type type, com.songoda.skyblock.upgrade.Upgrade.Type upgrade) {
            this.type = type;
            this.upgrade = upgrade;
        }

        public Type getType() {
            return this.type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public com.songoda.skyblock.upgrade.Upgrade.Type getUpgrade() {
            return this.upgrade;
        }

        public void setUpgrade(com.songoda.skyblock.upgrade.Upgrade.Type upgrade) {
            this.upgrade = upgrade;
        }

        public enum Type {
            UPGRADES, SIZE, MEMBERS
        }
    }
}
