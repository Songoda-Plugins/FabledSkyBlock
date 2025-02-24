package com.craftaro.skyblock.command.commands.admin;

import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.biome.BiomeManager;
import com.craftaro.skyblock.command.SubCommand;
import com.craftaro.skyblock.config.FileManager;
import com.craftaro.skyblock.config.FileManager.Config;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.island.IslandManager;
import com.craftaro.skyblock.island.IslandWorld;
import com.craftaro.skyblock.message.MessageManager;
import com.craftaro.skyblock.playerdata.PlayerDataManager;
import com.craftaro.skyblock.sound.SoundManager;
import com.craftaro.skyblock.utils.StringUtil;
import com.craftaro.skyblock.utils.player.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class SetBiomeCommand extends SubCommand {
    public SetBiomeCommand(SkyBlock plugin) {
        super(plugin);
    }

    @Override
    public void onCommandByPlayer(Player player, String[] args) {
        onCommand(player, args);
    }

    @Override
    public void onCommandByConsole(ConsoleCommandSender sender, String[] args) {
        onCommand(sender, args);
    }

    public void onCommand(CommandSender sender, String[] args) {
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        MessageManager messageManager = this.plugin.getMessageManager();
        IslandManager islandManager = this.plugin.getIslandManager();
        BiomeManager biomeManager = this.plugin.getBiomeManager();
        SoundManager soundManager = this.plugin.getSoundManager();
        FileManager fileManager = this.plugin.getFileManager();

        Config config = fileManager.getConfig(new File(this.plugin.getDataFolder(), "language.yml"));
        FileConfiguration configLoad = config.getFileConfiguration();

        if (args.length >= 2) {
            String biomeName = args[1].toUpperCase().trim();

            IslandWorld world = null;
            if (args.length > 2) {
                String worldName = args[2].toUpperCase().trim();
                for (IslandWorld islandWorld : IslandWorld.values()) {
                    if (islandWorld.getFriendlyName().equalsIgnoreCase(worldName)) {
                        world = islandWorld;
                    }
                }
            }

            if (world == null) {
                world = IslandWorld.NORMAL;
            }

            XBiome biome = null;
            for (XBiome xBiome : XBiome.values()) {
                if (xBiome.isSupported() && xBiome.name().equals(biomeName)) {
                    biome = xBiome;
                    break;
                }
            }

            if (biome != null) {
                Player targetPlayer = Bukkit.getPlayerExact(args[0]);
                UUID islandOwnerUUID;
                String targetPlayerName;

                if (targetPlayer == null) {
                    OfflinePlayer targetPlayerOffline = new OfflinePlayer(args[0]);
                    islandOwnerUUID = targetPlayerOffline.getOwner();
                    targetPlayerName = targetPlayerOffline.getName();
                } else {
                    islandOwnerUUID = playerDataManager.getPlayerData(targetPlayer).getOwner();
                    targetPlayerName = targetPlayer.getName();
                }

                if (islandOwnerUUID == null) {
                    messageManager.sendMessage(sender, configLoad.getString("Command.Island.Admin.SetBiome.Island.Owner.Message"));
                    soundManager.playSound(sender, XSound.ENTITY_VILLAGER_NO);
                } else {
                    if (islandManager.containsIsland(islandOwnerUUID)) {
                        Island island = islandManager.getIsland(Bukkit.getServer().getOfflinePlayer(islandOwnerUUID));
                        biomeManager.setBiome(island, world, biome, null);
                        if (world == IslandWorld.NORMAL) {
                            island.setBiome(biome);
                        }
                    } else {
                        islandManager.loadIsland(Bukkit.getOfflinePlayer(islandOwnerUUID));
                        Island island = islandManager.getIsland(Bukkit.getOfflinePlayer(islandOwnerUUID));
                        if (island == null) {
                            messageManager.sendMessage(sender,
                                    configLoad.getString("Command.Island.Admin.SetBiome.Island.Data.Message"));
                            soundManager.playSound(sender, XSound.BLOCK_ANVIL_LAND);
                        } else {
                            XBiome finalBiome = biome;
                            IslandWorld finalWorld = world;
                            biomeManager.setBiome(island, world, biome, () -> {
                                if (finalWorld == IslandWorld.NORMAL) {
                                    island.setBiome(finalBiome);
                                }
                            });
                        }
                    }

                    messageManager.sendMessage(sender,
                            configLoad.getString("Command.Island.Admin.SetBiome.Set.Message")
                                    .replace("%player", targetPlayerName)
                                    .replace("%biome", StringUtil.capitalizeWord(biome.getBiome().name().replaceAll("_", " "))));
                    soundManager.playSound(sender, XSound.BLOCK_NOTE_BLOCK_PLING);
                }
            } else {
                messageManager.sendMessage(sender, configLoad.getString("Command.Island.Admin.SetBiome.InvalidBiome.Message"));
                soundManager.playSound(sender, XSound.BLOCK_ANVIL_LAND);
            }
        } else {
            messageManager.sendMessage(sender, configLoad.getString("Command.Island.Admin.SetBiome.Invalid.Message"));
            soundManager.playSound(sender, XSound.BLOCK_ANVIL_LAND);
        }
    }

    @Override
    public String getName() {
        return "setbiome";
    }

    @Override
    public String getInfoMessagePath() {
        return "Command.Island.Admin.SetBiome.Info.Message";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String[] getArguments() {
        return new String[0];
    }
}
