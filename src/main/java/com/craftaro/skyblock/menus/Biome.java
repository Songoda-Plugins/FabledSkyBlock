package com.craftaro.skyblock.menus;

import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.biome.BiomeManager;
import com.craftaro.skyblock.cooldown.Cooldown;
import com.craftaro.skyblock.cooldown.CooldownManager;
import com.craftaro.skyblock.cooldown.CooldownPlayer;
import com.craftaro.skyblock.cooldown.CooldownType;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.island.IslandEnvironment;
import com.craftaro.skyblock.island.IslandManager;
import com.craftaro.skyblock.island.IslandRole;
import com.craftaro.skyblock.island.IslandWorld;
import com.craftaro.skyblock.message.MessageManager;
import com.craftaro.skyblock.permission.PermissionManager;
import com.craftaro.skyblock.placeholder.Placeholder;
import com.craftaro.skyblock.playerdata.PlayerDataManager;
import com.craftaro.skyblock.sound.SoundManager;
import com.craftaro.skyblock.utils.NumberUtil;
import com.craftaro.skyblock.utils.item.nInventoryUtil;
import com.craftaro.skyblock.utils.version.SBiome;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class Biome {
    private static Biome instance;

    public static Biome getInstance() {
        if (instance == null) {
            instance = new Biome();
        }

        return instance;
    }

    public void open(Player player) {
        SkyBlock plugin = SkyBlock.getPlugin(SkyBlock.class);

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        CooldownManager cooldownManager = plugin.getCooldownManager();
        MessageManager messageManager = plugin.getMessageManager();
        IslandManager islandManager = plugin.getIslandManager();
        PermissionManager permissionManager = plugin.getPermissionManager();
        BiomeManager biomeManager = plugin.getBiomeManager();
        SoundManager soundManager = plugin.getSoundManager();

        if (playerDataManager.hasPlayerData(player)) {
            FileConfiguration langConfig = plugin.getLanguage();

            nInventoryUtil nInv = new nInventoryUtil(player, event -> {
                Island island = islandManager.getIsland(player);

                if (island == null) {
                    messageManager.sendMessage(player,
                            langConfig.getString("Command.Island.Biome.Owner.Message"));
                    soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);
                    player.closeInventory();

                    return;
                } else if (!((island.hasRole(IslandRole.OPERATOR, player.getUniqueId())
                        && permissionManager.hasPermission(island, "Biome", IslandRole.OPERATOR))
                        || island.hasRole(IslandRole.OWNER, player.getUniqueId()))) {
                    messageManager.sendMessage(player,
                            langConfig.getString("Command.Island.Biome.Permission.Message"));
                    soundManager.playSound(player, XSound.ENTITY_VILLAGER_NO);
                    player.closeInventory();

                    return;
                }

                ItemStack is = event.getItem();

                if ((is.getType() == Material.NAME_TAG) && (is.hasItemMeta())
                        && (is.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&',
                        langConfig.getString("Menu.Biome.Item.Info.Displayname"))))) {
                    soundManager.playSound(player, XSound.ENTITY_CHICKEN_EGG);

                    event.setWillClose(false);
                    event.setWillDestroy(false);
                } else if ((XMaterial.BLACK_STAINED_GLASS_PANE.isSimilar(is))
                        && (is.hasItemMeta())
                        && (is.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&',
                        langConfig.getString("Menu.Biome.Item.Barrier.Displayname"))))) {
                    soundManager.playSound(player, XSound.BLOCK_GLASS_BREAK);

                    event.setWillClose(false);
                    event.setWillDestroy(false);
                } else if ((XMaterial.OAK_FENCE_GATE.isSimilar(is)) && (is.hasItemMeta())
                        && (is.getItemMeta().getDisplayName().equals(ChatColor.translateAlternateColorCodes('&',
                        langConfig.getString("Menu.Biome.Item.Exit.Displayname"))))) {
                    soundManager.playSound(player, XSound.BLOCK_CHEST_CLOSE);
                } else {
                    if (is.getItemMeta().hasEnchant(Enchantment.THORNS)) {
                        soundManager.playSound(player, XSound.BLOCK_ANVIL_LAND);

                        event.setWillClose(false);
                        event.setWillDestroy(false);
                    } else {
                        if (cooldownManager.hasPlayer(CooldownType.BIOME, player) && !player.hasPermission("fabledskyblock.bypass.cooldown")) {
                            CooldownPlayer cooldownPlayer = cooldownManager.getCooldownPlayer(CooldownType.BIOME, player);
                            Cooldown cooldown = cooldownPlayer.getCooldown();

                            if (cooldown.getTime() < 60) {
                                messageManager.sendMessage(player,
                                        langConfig.getString("Island.Biome.Cooldown.Message")
                                                .replace("%time",
                                                        cooldown.getTime() + " " + langConfig
                                                                .getString("Island.Biome.Cooldown.Word.Second")));
                            } else {
                                long[] durationTime = NumberUtil.getDuration(cooldown.getTime());
                                messageManager.sendMessage(player,
                                        langConfig.getString("Island.Biome.Cooldown.Message")
                                                .replace("%time", durationTime[2] + " "
                                                        + langConfig.getString("Island.Biome.Cooldown.Word.Minute")
                                                        + " " + durationTime[3] + " "
                                                        + langConfig.getString("Island.Biome.Cooldown.Word.Second")));
                            }

                            soundManager.playSound(player, XSound.ENTITY_VILLAGER_NO);

                            event.setWillClose(false);
                            event.setWillDestroy(false);

                            return;
                        }

                        @SuppressWarnings("deprecation")
                        SBiome selectedBiomeType = SBiome.getFromGuiIcon(is.getType(), is.getData().getData());
                        XBiome biome = selectedBiomeType.getBiome();

                        cooldownManager.createPlayer(CooldownType.BIOME, player);
                        biomeManager.setBiome(island, IslandWorld.NORMAL, biome, null);
                        island.setBiome(selectedBiomeType.getBiome());
                        island.save();

                        soundManager.playSound(island.getLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND),
                                XSound.ENTITY_GENERIC_SPLASH);

                        if (!islandManager.isPlayerAtIsland(island, player, IslandWorld.NORMAL)) {
                            soundManager.playSound(player, XSound.ENTITY_GENERIC_SPLASH);
                        }

                        Bukkit.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 1L);
                    }
                }
            });

            Island island = islandManager.getIsland(player);
            XBiome islandBiome = island.getBiome();
            String islandBiomeName = island.getBiomeName();

            nInv.addItem(nInv.createItem(new ItemStack(Material.NAME_TAG),
                    ChatColor.translateAlternateColorCodes('&',
                            langConfig.getString("Menu.Biome.Item.Info.Displayname")),
                    langConfig.getStringList("Menu.Biome.Item.Info.Lore"),
                    new Placeholder[]{new Placeholder("%biome_type", islandBiomeName)}, null, null), 4);

            nInv.addItem(nInv.createItem(XMaterial.OAK_FENCE_GATE.parseItem(),
                            langConfig.getString("Menu.Biome.Item.Exit.Displayname"), null, null, null, null),
                    0, 8);

            nInv.addItem(nInv.createItem(XMaterial.BLACK_STAINED_GLASS_PANE.parseItem(),
                            plugin.formatText(langConfig.getString("Menu.Biome.Item.Barrier.Displayname")),
                            null, null, null, null),
                    9, 10, 11, 12, 13, 14, 15, 16, 17);

            FileConfiguration settings = plugin.getConfiguration();

            boolean allowNetherBiome = settings.getBoolean("Island.Biome.AllowOtherWorldlyBiomes.Nether");
            boolean allowEndBiome = settings.getBoolean("Island.Biome.AllowOtherWorldlyBiomes.End");

            int slotIndex = 18;
            for (SBiome biome : SBiome.values()) {
                if (!biome.isAvailable()) {
                    continue;
                }
                if (!allowNetherBiome && biome == SBiome.NETHER) {
                    continue;
                }
                if (!allowEndBiome && (biome == SBiome.THE_END || biome == SBiome.THE_VOID)) {
                    continue;
                }
                if (!player.hasPermission("fabledskyblock.biome.*") && !player.hasPermission("fabledskyblock.biome." + biome.name().toLowerCase())) {
                    continue;
                }

                if (islandBiome == biome.getBiome()) {
                    nInv.addItem(nInv.createItem(biome.getGuiIcon(),
                                    ChatColor.translateAlternateColorCodes('&',
                                            langConfig.getString("Menu.Biome.Item.Biome.Current.Displayname")
                                                    .replace("%biome_type", biome.getFormattedBiomeName())),
                                    langConfig.getStringList("Menu.Biome.Item.Biome.Current.Lore"), null,
                                    new Enchantment[]{Enchantment.THORNS}, new ItemFlag[]{ItemFlag.HIDE_ENCHANTS}),
                            slotIndex);
                } else {
                    nInv.addItem(nInv.createItem(biome.getGuiIcon(),
                                    ChatColor.translateAlternateColorCodes('&',
                                            langConfig.getString("Menu.Biome.Item.Biome.Select.Displayname")
                                                    .replace("%biome_type", biome.getFormattedBiomeName())),
                                    langConfig.getStringList("Menu.Biome.Item.Biome.Select.Lore"), null, null, null),
                            slotIndex);
                }

                slotIndex++;
            }

            nInv.setTitle(ChatColor.translateAlternateColorCodes('&', langConfig.getString("Menu.Biome.Title")));
            nInv.setRows(4);

            Bukkit.getServer().getScheduler().runTask(plugin, nInv::open);
        }
    }
}
