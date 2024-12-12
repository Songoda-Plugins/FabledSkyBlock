package com.craftaro.skyblock.command.commands.island;

import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.command.SubCommand;
import com.craftaro.skyblock.config.FileManager;
import com.craftaro.skyblock.confirmation.Confirmation;
import com.craftaro.skyblock.cooldown.Cooldown;
import com.craftaro.skyblock.cooldown.CooldownManager;
import com.craftaro.skyblock.cooldown.CooldownPlayer;
import com.craftaro.skyblock.cooldown.CooldownType;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.message.MessageManager;
import com.craftaro.skyblock.playerdata.PlayerData;
import com.craftaro.skyblock.sound.SoundManager;
import com.craftaro.skyblock.structure.Structure;
import com.craftaro.skyblock.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class PreviewCommand extends SubCommand {
    public PreviewCommand(SkyBlock plugin) {
        super(plugin);
    }

    @Override
    public void onCommandByPlayer(Player player, String[] args) {
        CooldownManager cooldownManager = this.plugin.getCooldownManager();
        MessageManager messageManager = this.plugin.getMessageManager();
        SoundManager soundManager = this.plugin.getSoundManager();
        FileManager fileManager = this.plugin.getFileManager();
        FileManager.Config config = fileManager.getConfig(new File(this.plugin.getDataFolder(), "language.yml"));
        FileConfiguration configLang = config.getFileConfiguration();

        if (args.length != 1) {
            this.plugin.getMessageManager().sendMessage(player, configLang.getString("Command.Island.Preview.Argument.Count.Message"));
            return;
        }

        PlayerData data = this.plugin.getPlayerDataManager().getPlayerData(player);
        Island island = this.plugin.getIslandManager().getIsland(Bukkit.getOfflinePlayer(player.getUniqueId()));

        if (args[0].equals("confirm")) {
            if (data.getConfirmation() == Confirmation.PREVIEW && data.getConfirmationTime() > 0) {
                Structure islandStructure = this.plugin.getStructureManager().getStructure(island.getStructure());

                if (this.plugin.getIslandManager().deleteIsland(island, true)) {
                    island.setDeleted(true);
                    data.setPreview(false);
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }

                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (this.plugin.getIslandManager().createIsland(player, islandStructure)) {
                            this.plugin.getMessageManager().sendMessage(player, configLang.getString("Island.Creator.Selector.Created.Message"));
                            this.plugin.getSoundManager().playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING);
                        }
                    }, 30L);
                }
            }
        } else if (args[0].equals("cancel")) {
            if (data.getConfirmation() == Confirmation.PREVIEW && data.getConfirmationTime() > 0) {
                if (this.plugin.getIslandManager().deleteIsland(island, true)) {
                    island.setDeleted(true);
                    data.setPreview(false);
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }
            }
        } else {
            // Do not preview if cooldown is still active
            if (fileManager.getConfig(new File(this.plugin.getDataFolder(), "config.yml"))
                    .getFileConfiguration().getBoolean("Island.Preview.Cooldown.Enable")
                    && cooldownManager.hasPlayer(CooldownType.PREVIEW, player)) {
                CooldownPlayer cooldownPlayer = cooldownManager.getCooldownPlayer(CooldownType.PREVIEW, player);
                Cooldown cooldown = cooldownPlayer.getCooldown();

                if (cooldown.getTime() < 60) {
                    messageManager.sendMessage(player,
                            config.getFileConfiguration().getString("Island.Preview.Cooldown.Message")
                                    .replace("%time", cooldown.getTime() + " " + config.getFileConfiguration()
                                            .getString("Island.Preview.Cooldown.Word.Second")));
                } else {
                    long[] durationTime = NumberUtil.getDuration(cooldown.getTime());
                    messageManager.sendMessage(player,
                            config.getFileConfiguration().getString("Island.Preview.Cooldown.Message")
                                    .replace("%time", durationTime[2] + " "
                                            + config.getFileConfiguration()
                                            .getString("Island.Preview.Cooldown.Word.Minute")
                                            + " " + durationTime[3] + " " + config.getFileConfiguration()
                                            .getString("Island.Preview.Cooldown.Word.Second")));
                }

                soundManager.playSound(player, XSound.ENTITY_VILLAGER_NO);
                return;
            }
            // Do not preview if user has an island
            if (island != null) {
                this.plugin.getMessageManager().sendMessage(player, configLang.getString("Command.Island.Preview.Island.Message"));
                return;
            }
            Structure structure = this.plugin.getStructureManager().getStructure(args[0]);
            if (structure == null) {
                this.plugin.getMessageManager().sendMessage(player, configLang.getString("Command.Island.Preview.File.Message"));
                return;
            }
            this.plugin.getIslandManager().previewIsland(player, structure);
        }
    }

    @Override
    public void onCommandByConsole(ConsoleCommandSender sender, String[] args) {
        sender.sendMessage("SkyBlock | Error: You must be a player to perform that command.");
    }

    @Override
    public String getName() {
        return "preview";
    }

    @Override
    public String getInfoMessagePath() {
        return "Command.Island.Preview.Info.Message";
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
