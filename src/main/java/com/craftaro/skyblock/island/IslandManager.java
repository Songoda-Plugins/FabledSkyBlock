package com.craftaro.skyblock.island;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.compatibility.MajorServerVersion;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.nms.Nms;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.api.event.island.IslandCreateEvent;
import com.craftaro.skyblock.api.event.island.IslandDeleteEvent;
import com.craftaro.skyblock.api.event.island.IslandLoadEvent;
import com.craftaro.skyblock.api.event.island.IslandOwnershipTransferEvent;
import com.craftaro.skyblock.api.event.island.IslandUnloadEvent;
import com.craftaro.skyblock.ban.BanManager;
import com.craftaro.skyblock.blockscanner.CachedChunk;
import com.craftaro.skyblock.blockscanner.ChunkLoader;
import com.craftaro.skyblock.config.FileManager;
import com.craftaro.skyblock.confirmation.Confirmation;
import com.craftaro.skyblock.cooldown.CooldownManager;
import com.craftaro.skyblock.cooldown.CooldownType;
import com.craftaro.skyblock.invite.Invite;
import com.craftaro.skyblock.invite.InviteManager;
import com.craftaro.skyblock.island.removal.ChunkDeleteSplitter;
import com.craftaro.skyblock.message.MessageManager;
import com.craftaro.skyblock.playerdata.PlayerData;
import com.craftaro.skyblock.playerdata.PlayerDataManager;
import com.craftaro.skyblock.scoreboard.ScoreboardManager;
import com.craftaro.skyblock.sound.SoundManager;
import com.craftaro.skyblock.structure.Structure;
import com.craftaro.skyblock.structure.StructureManager;
import com.craftaro.skyblock.upgrade.Upgrade;
import com.craftaro.skyblock.upgrade.UpgradeManager;
import com.craftaro.skyblock.utils.ChatComponent;
import com.craftaro.skyblock.utils.player.OfflinePlayer;
import com.craftaro.skyblock.utils.player.PlayerUtil;
import com.craftaro.skyblock.utils.structure.SchematicUtil;
import com.craftaro.skyblock.utils.structure.StructureUtil;
import com.craftaro.skyblock.utils.world.LocationUtil;
import com.craftaro.skyblock.utils.world.block.BlockDegreesType;
import com.craftaro.skyblock.visit.VisitManager;
import com.craftaro.skyblock.world.WorldManager;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.third_party.com.cryptomorin.xseries.XSound;
import com.eatthepath.uuid.FastUUID;
import com.google.common.base.Preconditions;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandManager {
    private final SkyBlock plugin;

    private final List<IslandPosition> islandPositions = new ArrayList<>();
    private final Map<UUID, UUID> islandProxies = new HashMap<>();
    private final Map<UUID, Island> islandStorage = new ConcurrentHashMap<>();
    private final int offset;

    private HashMap<IslandWorld, Integer> oldSystemIslands;

    public IslandManager(SkyBlock plugin) {
        this.plugin = plugin;

        FileManager.Config config = plugin.getFileManager().getConfig(new File(plugin.getDataFolder(), "worlds.yml"));
        FileConfiguration configLoad = config.getFileConfiguration();

        this.offset = plugin.getConfiguration().getInt("Island.Creation.Distance", 1200);

        for (IslandWorld worldList : IslandWorld.values()) {
            ConfigurationSection configSection = configLoad.getConfigurationSection("World." + worldList.getFriendlyName() + ".nextAvailableLocation");
            this.islandPositions.add(new IslandPosition(worldList, configSection.getDouble("x"), configSection.getDouble("z")));
        }

        Bukkit.getOnlinePlayers().forEach(this::loadIsland);
        for (Island island : getIslands().values()) {
            if (island.isAlwaysLoaded()) {
                loadIslandAtLocation(island.getLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND));
            }
        }

        loadIslandPositions();
    }

    public void onDisable() {
        for (int i = 0; i < this.islandStorage.size(); i++) {
            UUID islandOwnerUUID = (UUID) this.islandStorage.keySet().toArray()[i];
            Island island = this.islandStorage.get(islandOwnerUUID);
            island.save();
        }
    }

    public synchronized void saveNextAvailableLocation(IslandWorld world) {
        FileManager fileManager = this.plugin.getFileManager();
        FileManager.Config config = fileManager.getConfig(new File(this.plugin.getDataFolder(), "worlds.yml"));

        File configFile = config.getFile();
        FileConfiguration configLoad = config.getFileConfiguration();
        for (IslandPosition islandPositionList : this.islandPositions) {
            if (islandPositionList.getWorld() == world) {
                int island_number = (int) configLoad.get("World." + world.getFriendlyName() + ".nextAvailableLocation.island_number");
                ConfigurationSection configSection = configLoad.createSection("World." + world.getFriendlyName() + ".nextAvailableLocation");
                configSection.set("x", islandPositionList.getX());
                configSection.set("z", islandPositionList.getZ());
                configSection.set("island_number", (island_number + 1));
            }
        }
        try {
            configLoad.save(configFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized void setNextAvailableLocation(IslandWorld world, org.bukkit.Location location) {
        for (IslandPosition islandPositionList : this.islandPositions) {
            if (islandPositionList.getWorld() == world) {
                islandPositionList.setX(location.getX());
                islandPositionList.setZ(location.getZ());
            }
        }
    }


    public synchronized org.bukkit.Location prepareNextAvailableLocation(IslandWorld world) {
        for (IslandPosition islandPositionList : this.islandPositions) {
            if (islandPositionList.getWorld() == world) {

                FileManager.Config config_world = this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "worlds.yml"));

                FileConfiguration configLoad_world = config_world.getFileConfiguration();
                FileConfiguration configLoad_config = this.plugin.getConfiguration();
                int x = (int) configLoad_world.get("World." + world.getFriendlyName() + ".nextAvailableLocation.island_number");
                int islandHeight = configLoad_config.getInt("Island.World." + world.getFriendlyName() + ".IslandSpawnHeight", 72);
                while (true) {
                    double r = Math.floor((Math.sqrt(x + 1) - 1) / 2) + 1;
                    double p = (8 * r * (r - 1)) / 2;
                    double en = r * 2;
                    double a = (x - p) % (r * 8);
                    int posX = 0;
                    int posY = 0;
                    int loc = (int) Math.floor(a / (r * 2));
                    switch (loc) {
                        case 0:
                            posX = (int) (a - r);
                            posY = (int) (-r);
                            break;
                        case 1:
                            posX = (int) r;
                            posY = (int) ((a % en) - r);
                            break;
                        case 2:
                            posX = (int) (r - (a % en));
                            posY = (int) r;
                            break;
                        case 3:
                            posX = (int) (-r);
                            posY = (int) (r - (a % en));
                            break;
                        default:
                            this.plugin.getLogger().warning("[FabledSkyblock][prepareNextAvailableLocation] Error in the spiral value: " + loc);
                            return null;
                    }
                    posX = posX * this.offset;
                    posY = posY * this.offset;
                    islandPositionList.setX(posX);
                    islandPositionList.setZ(posY);
                    // Check if there was an island at this position
                    int oldFormatPos = this.oldSystemIslands.get(world);
                    Location islandLocation = new org.bukkit.Location(this.plugin.getWorldManager().getWorld(world), islandPositionList.getX(), islandHeight, islandPositionList.getZ());
                    if (posX == 1200 && posY >= 0 && posY <= oldFormatPos) {
                        // We have to save to avoid having two islands at same location
                        setNextAvailableLocation(world, islandLocation);
                        saveNextAvailableLocation(world);
                        x++;
                        continue;
                    }
                    return islandLocation;
                }
            }
        }

        return null;
    }

    public synchronized boolean createIsland(Player player, Structure structure) {
        ScoreboardManager scoreboardManager = this.plugin.getScoreboardManager();
        VisitManager visitManager = this.plugin.getVisitManager();
        FileManager fileManager = this.plugin.getFileManager();
        BanManager banManager = this.plugin.getBanManager();

        PlayerData data = this.plugin.getPlayerDataManager().getPlayerData(player);

        long amt = 0;

        if (data != null) {
            final int highest = PlayerUtil.getNumberFromPermission(player, "fabledskyblock.limit.create", true, 2);

            if ((amt = data.getIslandCreationCount()) >= highest) {
                this.plugin.getLanguage().getString("Island.Creator.Error.MaxCreationMessage");
                return false;
            }
        }

        if (fileManager.getConfig(new File(this.plugin.getDataFolder(), "locations.yml")).getFileConfiguration().getString("Location.Spawn") == null) {
            this.plugin.getMessageManager().sendMessage(player, this.plugin.getLanguage().getString("Island.Creator.Error.Message"));
            this.plugin.getSoundManager().playSound(player, XSound.BLOCK_ANVIL_LAND);

            return false;
        }

        if (data != null) {
            data.setIslandCreationCount(amt + 1);
        }

        Island island = new Island(player);
        island.setStructure(structure.getName());
        this.islandStorage.put(player.getUniqueId(), island);

        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            prepareIsland(island, worldList);
        }

        if (!visitManager.hasIsland(island.getOwnerUUID())) {
            visitManager.createIsland(island.getOwnerUUID(),
                    new IslandLocation[]{island.getIslandLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND), island.getIslandLocation(IslandWorld.NETHER, IslandEnvironment.ISLAND),
                            island.getIslandLocation(IslandWorld.END, IslandEnvironment.ISLAND)},
                    island.getSize(), island.getRole(IslandRole.MEMBER).size() + island.getRole(IslandRole.OPERATOR).size() + 1, island.getBankBalance(), visitManager.getIslandSafeLevel(island.getOwnerUUID()), island.getLevel(),
                    island.getMessage(IslandMessage.SIGNATURE), island.getStatus());
        }

        if (!banManager.hasIsland(island.getOwnerUUID())) {
            banManager.createIsland(island.getOwnerUUID());
        }

        FileConfiguration configLoad = this.plugin.getConfiguration();

        if (configLoad.getBoolean("Island.Creation.Cooldown.Creation.Enable") && !player.hasPermission("fabledskyblock.bypass.cooldown") && !player.hasPermission("fabledskyblock.bypass.*")
                && !player.hasPermission("fabledskyblock.*")) {
            this.plugin.getCooldownManager().createPlayer(CooldownType.CREATION, player);
        }
        if (configLoad.getBoolean("Island.Deletion.Cooldown.Deletion.Enable") && !player.hasPermission("fabledskyblock.bypass.cooldown") && !player.hasPermission("fabledskyblock.bypass.*")
                && !player.hasPermission("fabledskyblock.*")) {
            this.plugin.getCooldownManager().createPlayer(CooldownType.DELETION, player);
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> Bukkit.getServer().getPluginManager().callEvent(new IslandCreateEvent(island.getAPIWrapper(), player)));

        data.setIsland(player.getUniqueId());
        data.setOwner(player.getUniqueId());

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            scoreboardManager.updatePlayerScoreboardType(player);
        });

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            PaperLib.teleportAsync(player, island.getLocation(IslandWorld.NORMAL, IslandEnvironment.MAIN));
            player.setFallDistance(0.0F);
        }, configLoad.getInt("Island.Creation.TeleportTimeout") * 20);

        String biomeName = this.plugin.getConfiguration().getString("Island.Biome.Default.Type").toUpperCase();
        XBiome xBiome;
        try {
            xBiome = XBiome.of(biomeName).get();
        } catch (Exception ex) {
            xBiome = XBiome.PLAINS;
        }
        final XBiome compatibleBiome = xBiome;

        Bukkit.getServer().getScheduler().runTaskLater(this.plugin, () ->
                this.plugin.getBiomeManager().setBiome(island, IslandWorld.NORMAL, compatibleBiome, () -> {
                    if (structure.getCommands() != null) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
                            for (String commandList : structure.getCommands()) {
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandList.replace("%player", player.getName()));
                            }
                        });
                    }
                }), 20L);

        // Recalculate island level after 5 seconds
        if (configLoad.getBoolean("Island.Levelling.ScanAutomatically")) {
            Bukkit.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getLevellingManager().startScan(null, island), 100L);
        }

        return true;
    }

    public synchronized boolean previewIsland(Player player, Structure structure) {
        FileManager fileManager = this.plugin.getFileManager();

        PlayerData data = this.plugin.getPlayerDataManager().getPlayerData(player);
        FileConfiguration configLang = this.plugin.getLanguage();
        FileConfiguration configMain = this.plugin.getConfiguration();


        if (data != null) {
            final int highest = PlayerUtil.getNumberFromPermission(player, "fabledskyblock.limit.create", true, 2);

            if ((data.getIslandCreationCount()) >= highest) {
                this.plugin.getMessageManager().sendMessage(player, this.plugin.getLanguage().getString("Island.Creator.Error.MaxCreationMessage"));
                return false;
            }

        }

        if (fileManager.getConfig(new File(this.plugin.getDataFolder(), "locations.yml")).getFileConfiguration().getString("Location.Spawn") == null) {
            this.plugin.getMessageManager().sendMessage(player, configLang.getString("Island.Creator.Error.Message"));
            this.plugin.getSoundManager().playSound(player, XSound.BLOCK_ANVIL_LAND);

            return false;
        }

        Island island = new Island(player);
        island.setStructure(structure.getName());
        this.islandStorage.put(player.getUniqueId(), island);

        data.setPreview(true);

        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            prepareIsland(island, worldList);
        }


        Bukkit.getScheduler().callSyncMethod(this.plugin, () -> {
            PaperLib.teleportAsync(player, island.getLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND));
            player.setGameMode(GameMode.SPECTATOR);
            return true;
        });

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (data.isPreview()) {
                Location spawn = fileManager.getLocation(fileManager.getConfig(new File(this.plugin.getDataFolder(), "locations.yml")), "Location.Spawn", true);
                PaperLib.teleportAsync(player, spawn);
                player.setGameMode(GameMode.SURVIVAL);
                data.setIsland(null);
                this.islandStorage.remove(player.getUniqueId(), island);
                deleteIsland(island, true);
                this.plugin.getMessageManager().sendMessage(player, configLang.getString("Island.Preview.Timeout.Message"));
                data.setPreview(false);
            }
        }, configMain.getInt("Island.Preview.Time") * 20);


        String defaultMessage = configLang.getString("Command.Island.Preview.Confirmation.Message")
                .replaceAll("%time", "" + configMain.get("Island.Preview.Time"));

        defaultMessage = defaultMessage.replace("\\n", "\n");

        for (String message : defaultMessage.split("\n")) {
            ChatComponent confirmation = null, cancelation = null;

            if (message.contains("%confirm")) {
                message = message.replace("%confirm", "");
                confirmation = new ChatComponent(configLang.getString("Command.Island.Preview.Confirmation.Word.Confirm").toUpperCase() + "     ",
                        true, net.md_5.bungee.api.ChatColor.GREEN,
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island preview confirm"),
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(
                                        net.md_5.bungee.api.ChatColor.translateAlternateColorCodes(
                                                '&',
                                                configLang.getString("Command.Island.Preview.Confirmation.Word.TutorialConfirm")))
                                        .create()
                        ));
            }

            if (message.contains("%cancel")) {
                message = message.replace("%cancel", "");
                cancelation = new ChatComponent(configLang.getString("Command.Island.Preview.Confirmation.Word.Cancel").toUpperCase(),
                        true, net.md_5.bungee.api.ChatColor.GREEN,
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island preview cancel"),
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(
                                        net.md_5.bungee.api.ChatColor.translateAlternateColorCodes(
                                                '&',
                                                configLang.getString("Command.Island.Preview.Confirmation.Word.TutorialCancel")))
                                        .create()
                        ));
            }

            TextComponent confirmationMessage = new TextComponent(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message));
            if (confirmation != null) {
                confirmationMessage.addExtra(confirmation.getTextComponent());
            }
            if (cancelation != null) {
                confirmationMessage.addExtra(cancelation.getTextComponent());
            }

            player.spigot().sendMessage(confirmationMessage);

        }

        data.setConfirmation(Confirmation.PREVIEW);
        data.setConfirmationTime(configMain.getInt("Island.Preview.Time"));


        FileConfiguration configLoad = this.plugin.getConfiguration();
        if (configLoad.getBoolean("Island.Preview.Cooldown.Enable") && !player.hasPermission("fabledskyblock.bypass.cooldown")
                && !player.hasPermission("fabledskyblock.bypass.*") && !player.hasPermission("fabledskyblock.*")) {
            this.plugin.getCooldownManager().createPlayer(CooldownType.PREVIEW, player);
        }

        return true;
    }

    public synchronized void giveOwnership(Island island, org.bukkit.OfflinePlayer player) {
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        CooldownManager cooldownManager = this.plugin.getCooldownManager();
        FileManager fileManager = this.plugin.getFileManager();

        if (island.isDeleted()) {
            return;
        }

        if (island.hasRole(IslandRole.MEMBER, player.getUniqueId()) || island.hasRole(IslandRole.OPERATOR, player.getUniqueId())) {
            UUID uuid2 = island.getOwnerUUID();

            island.save();
            island.setOwnerUUID(player.getUniqueId());
            island.getAPIWrapper().setPlayer(player);

            IslandLevel level = island.getLevel();
            level.save();
            level.setOwnerUUID(player.getUniqueId());

            FileConfiguration configLoad = this.plugin.getConfiguration();

            if (configLoad.getBoolean("Island.Ownership.Password.Reset")) {
                island.setPassword(null);
            }

            File oldCoopDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/coop-data"), uuid2.toString() + ".yml");
            fileManager.unloadConfig(oldCoopDataFile);

            if (fileManager.isFileExist(oldCoopDataFile)) {
                File newCoopDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/coop-data"), player.getUniqueId().toString() + ".yml");

                fileManager.unloadConfig(newCoopDataFile);
                oldCoopDataFile.renameTo(newCoopDataFile);
            }

            File oldLevelDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/level-data"), uuid2.toString() + ".yml");
            File newLevelDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/level-data"), player.getUniqueId().toString() + ".yml");

            fileManager.unloadConfig(oldLevelDataFile);
            fileManager.unloadConfig(newLevelDataFile);
            oldLevelDataFile.renameTo(newLevelDataFile);

            File oldSettingDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/setting-data"), uuid2.toString() + ".yml");
            File newSettingDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/setting-data"), player.getUniqueId().toString() + ".yml");

            fileManager.unloadConfig(oldSettingDataFile);
            fileManager.unloadConfig(newSettingDataFile);
            oldSettingDataFile.renameTo(newSettingDataFile);

            File oldIslandDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/island-data"), uuid2.toString() + ".yml");
            File newIslandDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/island-data"), player.getUniqueId().toString() + ".yml");

            fileManager.unloadConfig(oldIslandDataFile);
            fileManager.unloadConfig(newIslandDataFile);
            oldIslandDataFile.renameTo(newIslandDataFile);

            if (this.plugin.getConfiguration()
                    .getBoolean("Island.Challenge.PerIsland", true)) {
                File oldChallengeDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/challenge-data"), uuid2.toString() + ".yml");
                File newChallengeDataFile = new File(new File(this.plugin.getDataFolder().toString() + "/challenge-data"), player.getUniqueId().toString() + ".yml");

                fileManager.unloadConfig(oldChallengeDataFile);
                fileManager.unloadConfig(newChallengeDataFile);
                oldChallengeDataFile.renameTo(newChallengeDataFile);
            }

            this.plugin.getVisitManager().transfer(uuid2, player.getUniqueId());
            this.plugin.getBanManager().transfer(uuid2, player.getUniqueId());
            this.plugin.getInviteManager().tranfer(uuid2, player.getUniqueId());

            org.bukkit.OfflinePlayer player1 = Bukkit.getServer().getOfflinePlayer(uuid2);

            cooldownManager.transferPlayer(CooldownType.LEVELLING, player1, player);
            cooldownManager.transferPlayer(CooldownType.OWNERSHIP, player1, player);

            if (configLoad.getBoolean("Island.Ownership.Transfer.Operator")) {
                island.setRole(IslandRole.OPERATOR, uuid2);
            } else {
                island.setRole(IslandRole.MEMBER, uuid2);
            }

            if (island.hasRole(IslandRole.MEMBER, player.getUniqueId())) {
                island.removeRole(IslandRole.MEMBER, player.getUniqueId());
            } else {
                island.removeRole(IslandRole.OPERATOR, player.getUniqueId());
            }

            removeIsland(uuid2);
            this.islandStorage.put(player.getUniqueId(), island);

            Bukkit.getServer().getPluginManager().callEvent(new IslandOwnershipTransferEvent(island.getAPIWrapper(), player, uuid2));

            ArrayList<UUID> islandMembers = new ArrayList<>();
            islandMembers.addAll(island.getRole(IslandRole.MEMBER));
            islandMembers.addAll(island.getRole(IslandRole.OPERATOR));
            islandMembers.add(player.getUniqueId());

            for (UUID islandMemberList : islandMembers) {
                Player targetPlayer = Bukkit.getServer().getPlayer(islandMemberList);

                if (targetPlayer == null) {
                    File configFile = new File(new File(this.plugin.getDataFolder().toString() + "/player-data"), islandMemberList.toString() + ".yml");
                    configLoad = YamlConfiguration.loadConfiguration(configFile);
                    configLoad.set("Island.Owner", player.getUniqueId().toString());

                    try {
                        configLoad.save(configFile);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    PlayerData playerData = playerDataManager.getPlayerData(targetPlayer);
                    playerData.setOwner(player.getUniqueId());
                    playerData.setIsland(player.getUniqueId());
                    playerData.save();
                }
            }
        }
    }

    public synchronized boolean deleteIsland(Island island, boolean force) {
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();
        CooldownManager cooldownManager = this.plugin.getCooldownManager();
        FileManager fileManager = this.plugin.getFileManager();
        WorldManager worldManager = this.plugin.getWorldManager();

        if (!force) {
            PlayerData data = playerDataManager.getPlayerData(island.getOwnerUUID());

            if (data != null) {
                final Player player = data.getPlayer();

                if (player != null) {
                    long amt;
                    final int highest = PlayerUtil.getNumberFromPermission(player, "fabledskyblock.limit.delete", true, 1);

                    if ((amt = data.getIslandDeletionCount()) >= highest) {
                        return false;
                    }

                    data.setIslandDeletionCount(amt + 1);
                    data.deleteTransactions();
                }
            }
        }

        FileConfiguration configLoad = this.plugin.getConfiguration();

        if (configLoad.getBoolean("Island.Deletion.DeleteIsland", true)) {
            startDeletion(island, worldManager);
        }

        this.plugin.getVisitManager().deleteIsland(island.getOwnerUUID());
        this.plugin.getBanManager().deleteIsland(island.getOwnerUUID());
        this.plugin.getVisitManager().removeVisitors(island, VisitManager.Removal.DELETED);

        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(island.getOwnerUUID());
        cooldownManager.removeCooldownPlayer(CooldownType.LEVELLING, offlinePlayer);
        cooldownManager.removeCooldownPlayer(CooldownType.OWNERSHIP, offlinePlayer);

        boolean cooldownCreationEnabled = configLoad.getBoolean("Island.Creation.Cooldown.Creation.Enable");
        boolean cooldownDeletionEnabled = configLoad.getBoolean("Island.Creation.Cooldown.Deletion.Enable");
        boolean cooldownPreviewEnabled = configLoad.getBoolean("Island.Preview.Cooldown.Enable");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if ((island.hasRole(IslandRole.MEMBER, player.getUniqueId()) ||
                    island.hasRole(IslandRole.OPERATOR, player.getUniqueId()) ||
                    island.hasRole(IslandRole.OWNER, player.getUniqueId())) &&
                    playerDataManager.hasPlayerData(player)) {
                PlayerData playerData = playerDataManager.getPlayerData(player);
                playerData.setOwner(null);
                playerData.setMemberSince(null);
                playerData.setChat(false);
                playerData.save();

                if (isPlayerAtIsland(island, player)) {
                    LocationUtil.teleportPlayerToSpawn(player);
                }

                // TODO - Find a way to delete also offline players
                if (configLoad.getBoolean("Island.Deletion.ClearInventory", false) && !playerData.isPreview()) {
                    player.getInventory().clear();
                }
                if (configLoad.getBoolean("Island.Deletion.ClearEnderChest", false) && !playerData.isPreview()) {
                    player.getEnderChest().clear();
                }

                if (!playerData.isPreview()) {
                    if (cooldownCreationEnabled) {
                        if (!player.hasPermission("fabledskyblock.bypass.cooldown") && !player.hasPermission("fabledskyblock.bypass.*") && !player.hasPermission("fabledskyblock.*")) {
                            this.plugin.getCooldownManager().createPlayer(CooldownType.CREATION, player);
                        }
                    }
                    if (cooldownDeletionEnabled) {
                        if (!player.hasPermission("fabledskyblock.bypass.cooldown") && !player.hasPermission("fabledskyblock.bypass.*") && !player.hasPermission("fabledskyblock.*")) {
                            this.plugin.getCooldownManager().createPlayer(CooldownType.DELETION, player);
                        }
                    }
                } else {
                    if (cooldownPreviewEnabled) {
                        if (!player.hasPermission("fabledskyblock.bypass.cooldown") && !player.hasPermission("fabledskyblock.bypass.*") && !player.hasPermission("fabledskyblock.*")) {
                            this.plugin.getCooldownManager().createPlayer(CooldownType.PREVIEW, player);
                        }
                    }
                }
            }

            InviteManager inviteManager = this.plugin.getInviteManager();

            if (inviteManager.hasInvite(player.getUniqueId())) {
                Invite invite = inviteManager.getInvite(player.getUniqueId());

                if (invite.getOwnerUUID().equals(island.getOwnerUUID())) {
                    inviteManager.removeInvite(player.getUniqueId());
                }
            }
        }

        fileManager.deleteConfig(new File(new File(this.plugin.getDataFolder().toString() + "/coop-data"), island.getOwnerUUID().toString() + ".yml"));
        fileManager.deleteConfig(new File(new File(this.plugin.getDataFolder().toString() + "/level-data"), island.getOwnerUUID().toString() + ".yml"));
        fileManager.deleteConfig(new File(new File(this.plugin.getDataFolder().toString() + "/setting-data"), island.getOwnerUUID().toString() + ".yml"));
        fileManager.deleteConfig(new File(new File(this.plugin.getDataFolder().toString() + "/island-data"), island.getOwnerUUID().toString() + ".yml"));
        if (this.plugin.getConfiguration()
                .getBoolean("Island.Challenge.PerIsland", true)) {
            fileManager.deleteConfig(new File(new File(this.plugin.getDataFolder().toString() + "/challenge-data"), island.getOwnerUUID().toString() + ".yml"));
        }

        Bukkit.getServer().getPluginManager().callEvent(new IslandDeleteEvent(island.getAPIWrapper()));

        this.islandStorage.remove(island.getOwnerUUID());
        return true;
    }

    private void startDeletion(Island island, WorldManager worldManager) {
        final Map<World, List<CachedChunk>> cachedChunks = new HashMap<>(3);

        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            final Location location = island.getLocation(worldList, IslandEnvironment.ISLAND);
            if (location == null) {
                continue;
            }

            final World world = worldManager.getWorld(worldList);

            ChunkLoader.startChunkLoading(island, IslandWorld.NORMAL, this.plugin.isPaperAsync(), (chunks) -> {
                cachedChunks.put(world, chunks);
                ChunkDeleteSplitter.startDeletion(cachedChunks);
            }, null);
        }
    }

    public synchronized void deleteIslandData(UUID uuid) {
        FileManager fileManager = this.plugin.getFileManager();
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/island-data", FastUUID.toString(uuid) + ".yml"));
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/ban-data", FastUUID.toString(uuid) + ".yml"));
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/coop-data", FastUUID.toString(uuid) + ".yml"));
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/level-data", FastUUID.toString(uuid) + ".yml"));
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/setting-data", FastUUID.toString(uuid) + ".yml"));
        fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/visit-data", FastUUID.toString(uuid) + ".yml"));
        if (this.plugin.getConfiguration().getBoolean("Island.Challenge.PerIsland", true)) {
            fileManager.deleteConfig(new File(this.plugin.getDataFolder().toString() + "/challenge-data", FastUUID.toString(uuid) + ".yml"));
        }
    }

    public void loadIsland(org.bukkit.OfflinePlayer player) {
        VisitManager visitManager = this.plugin.getVisitManager();
        FileManager fileManager = this.plugin.getFileManager();
        BanManager banManager = this.plugin.getBanManager();

        UUID islandOwnerUUID = null;

        FileManager.Config config = fileManager.getConfig(new File(new File(this.plugin.getDataFolder().toString() + "/player-data"), player.getUniqueId().toString() + ".yml"));
        FileConfiguration configLoad = config.getFileConfiguration();

        if (isIslandExist(player.getUniqueId())) {
            if (configLoad.getString("Island.Owner") == null || !configLoad.getString("Island.Owner").equals(player.getUniqueId().toString())) {
                deleteIslandData(player.getUniqueId());
                configLoad.set("Island.Owner", null);

                return;
            }

            islandOwnerUUID = player.getUniqueId();
        } else {
            if (configLoad.getString("Island.Owner") != null) {
                islandOwnerUUID = FastUUID.parseUUID(configLoad.getString("Island.Owner"));
            }
        }

        if (islandOwnerUUID != null && !containsIsland(islandOwnerUUID)) {
            config = fileManager.getConfig(new File(this.plugin.getDataFolder().toString() + "/island-data", islandOwnerUUID.toString() + ".yml"));

            if (config.getFileConfiguration().getString("Location") == null) {
                deleteIslandData(islandOwnerUUID);
                configLoad.set("Island.Owner", null);

                return;
            }

            Island island = new Island(Bukkit.getServer().getOfflinePlayer(islandOwnerUUID));
            this.islandStorage.put(islandOwnerUUID, island);

            for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
                prepareIsland(island, worldList);
            }

            if (!visitManager.hasIsland(island.getOwnerUUID())) {
                visitManager.createIsland(island.getOwnerUUID(),
                        new IslandLocation[]{island.getIslandLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND), island.getIslandLocation(IslandWorld.NETHER, IslandEnvironment.ISLAND),
                                island.getIslandLocation(IslandWorld.END, IslandEnvironment.ISLAND)},
                        island.getSize(), island.getRole(IslandRole.MEMBER).size() + island.getRole(IslandRole.OPERATOR).size() + 1, island.getBankBalance(), visitManager.getIslandSafeLevel(island.getOwnerUUID()),
                        island.getLevel(), island.getMessage(IslandMessage.SIGNATURE), island.getStatus());
            }

            if (!banManager.hasIsland(island.getOwnerUUID())) {
                banManager.createIsland(island.getOwnerUUID());
            }

            Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getServer().getPluginManager().callEvent(new IslandLoadEvent(island.getAPIWrapper())));
        }
    }

    public void adjustAllIslandsSize(@Nonnull int diff, @Nullable Runnable callback) {
        FileManager fileManager = this.plugin.getFileManager();
        File islandConfigDir = new File(this.plugin.getDataFolder().toString() + "/island-data");

        if (!islandConfigDir.exists()) {
            return;
        }

        File[] files = islandConfigDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.getName().contains(".yml") && file.getName().length() > 35) {
                try {
                    UUID islandOwnerUUID = FastUUID.parseUUID(file.getName().split("\\.")[0]);

                    Island island = getIslandByOwner(Bukkit.getOfflinePlayer(islandOwnerUUID));

                    if (island != null) {
                        island.setSize(island.getSize() + diff);
                        island.save();
                    } else {
                        loadIsland(file);
                        island = getIslandByOwner(Bukkit.getOfflinePlayer(islandOwnerUUID));

                        island.setSize(island.getSize() + diff);
                        island.save();
                        unloadIsland(island, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (callback != null) {
            callback.run();
        }
    }

    public void setAllIslandsSize(@Nonnull int size, @Nullable Runnable callback) {
        File islandConfigDir = new File(this.plugin.getDataFolder().toString() + "/island-data");
        if (!islandConfigDir.exists()) {
            return;
        }

        File[] files = islandConfigDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.getName().contains(".yml") && file.getName().length() > 35) {
                try {
                    UUID islandOwnerUUID = FastUUID.parseUUID(file.getName().split("\\.")[0]);

                    Island island = getIslandByOwner(Bukkit.getOfflinePlayer(islandOwnerUUID));

                    if (island != null) {
                        island.setSize(size);
                        island.save();
                    } else {
                        loadIsland(file);
                        island = getIslandByOwner(Bukkit.getOfflinePlayer(islandOwnerUUID));

                        island.setSize(size);
                        island.save();
                        unloadIsland(island, null);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (callback != null) {
            callback.run();
        }
    }

    public void loadIsland(File islandFile) {
        VisitManager visitManager = this.plugin.getVisitManager();
        FileManager fileManager = this.plugin.getFileManager();
        BanManager banManager = this.plugin.getBanManager();

        FileManager.Config config = fileManager.getConfig(islandFile);
        FileConfiguration configLoad = config.getFileConfiguration();

        UUID islandOwnerUUID = FastUUID.parseUUID(islandFile.getName().split("\\.")[0]);

        if (config.getFileConfiguration().getString("Location") == null) {
            deleteIslandData(islandOwnerUUID);
            configLoad.set("Island.Owner", null);

            return;
        }

        Island island = new Island(Bukkit.getServer().getOfflinePlayer(islandOwnerUUID));
        this.islandStorage.put(islandOwnerUUID, island);

        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            prepareIsland(island, worldList);
        }

        if (!visitManager.hasIsland(island.getOwnerUUID())) {
            visitManager.createIsland(island.getOwnerUUID(),
                    new IslandLocation[]{island.getIslandLocation(IslandWorld.NORMAL, IslandEnvironment.ISLAND), island.getIslandLocation(IslandWorld.NETHER, IslandEnvironment.ISLAND),
                            island.getIslandLocation(IslandWorld.END, IslandEnvironment.ISLAND)},
                    island.getSize(), island.getRole(IslandRole.MEMBER).size() + island.getRole(IslandRole.OPERATOR).size() + 1, island.getBankBalance(), visitManager.getIslandSafeLevel(island.getOwnerUUID()),
                    island.getLevel(), island.getMessage(IslandMessage.SIGNATURE), island.getStatus());
        }

        if (!banManager.hasIsland(island.getOwnerUUID())) {
            banManager.createIsland(island.getOwnerUUID());
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getServer().getPluginManager().callEvent(new IslandLoadEvent(island.getAPIWrapper())));
    }

    /**
     * The old island position system was not good, it always create islands at x = 1200 and z starting at 0 and increasing by 1200<br />
     * This method will get the nextAvailableLocation for normal, nether and end islands in worlds.yml file
     * to avoid creating island where an existing island was
     */
    public void loadIslandPositions() {
        this.oldSystemIslands = new HashMap<>();

        FileManager fileManager = this.plugin.getFileManager();
        FileManager.Config config = fileManager.getConfig(new File(this.plugin.getDataFolder(), "worlds.yml"));
        FileConfiguration fileConfig = config.getFileConfiguration();
        FileManager.Config config2 = fileManager.getConfig(new File(this.plugin.getDataFolder(), "worlds.oldformat.yml"));
        FileConfiguration fileConfig2 = config2.getFileConfiguration();

        // TODO Find a way to automatically
        int normalZ = 0;
        int netherZ = 0;
        int endZ = 0;
        if (!config2.getFile().exists()) {
            // Old data
            Bukkit.getLogger().info("[FabledSkyblock] Old format detected, please wait ...");
            if (fileConfig.contains("World.Normal.nextAvailableLocation")) {
                normalZ = fileConfig.getInt("World.Normal.nextAvailableLocation.z");
            }
            if (fileConfig.contains("World.Nether.nextAvailableLocation")) {
                netherZ = fileConfig.getInt("World.Nether.nextAvailableLocation.z");
            }
            if (fileConfig.contains("World.End.nextAvailableLocation")) {
                endZ = fileConfig.getInt("World.End.nextAvailableLocation.z");
            }
            // Save
            fileConfig2.set("Normal", normalZ);
            fileConfig2.set("Nether", netherZ);
            fileConfig2.set("End", endZ);
            try {
                fileConfig2.save(config2.getFile());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Bukkit.getLogger().info("[FabledSkyblock] Done ! Got normalZ = " + normalZ + ", netherZ = " + netherZ + ", endZ = " + endZ);
        } else {
            // Load datas
            normalZ = fileConfig2.getInt("Normal");
            netherZ = fileConfig2.getInt("Nether");
            endZ = fileConfig2.getInt("End");
        }
        this.oldSystemIslands.put(IslandWorld.NORMAL, normalZ);
        this.oldSystemIslands.put(IslandWorld.NETHER, netherZ);
        this.oldSystemIslands.put(IslandWorld.END, endZ);
    }

    public void loadIslandAtLocation(Location location) {
        FileManager fileManager = this.plugin.getFileManager();
        File configFile = new File(this.plugin.getDataFolder(), "island-data");
        if (!configFile.exists()) {
            return;
        }

        File[] files = configFile.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file != null && file.getName().contains(".yml") && file.getName().length() > 35) {
                try {
                    FileManager.Config config = new FileManager.Config(fileManager, file);
                    FileConfiguration configLoad = config.getFileConfiguration();

                    int size = 10;
                    if (configLoad.getString("Size") != null) {
                        size = configLoad.getInt("Size");
                    }

                    Location islandLocation = fileManager.getLocation(config, "Location.Normal.Island", false);

                    if (LocationUtil.isLocationInLocationRadius(location, islandLocation, size)) {
                        loadIsland(file);
                        return;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void unloadIsland(Island island, org.bukkit.OfflinePlayer player) {
        if (island.isAlwaysLoaded()) {
            return;
        }
        ScoreboardManager scoreboardManager = this.plugin.getScoreboardManager();
        FileManager fileManager = this.plugin.getFileManager();

        if (island.isDeleted()) {
            return;
        }

        island.save();

        int islandVisitors = getVisitorsAtIsland(island).size();
        boolean unloadIsland = true;

        for (Player loopPlayer : Bukkit.getOnlinePlayers()) {
            if (loopPlayer == null || (player != null && player.getUniqueId().equals(loopPlayer.getUniqueId()))) {
                continue;
            }

            if (island.hasRole(IslandRole.MEMBER, loopPlayer.getUniqueId()) ||
                    island.hasRole(IslandRole.OPERATOR, loopPlayer.getUniqueId()) ||
                    island.hasRole(IslandRole.OWNER, loopPlayer.getUniqueId()) ||
                    island.getCoopType(loopPlayer.getUniqueId()) == IslandCoop.NORMAL) {
                scoreboardManager.updatePlayerScoreboardType(loopPlayer);

                unloadIsland = false;
            }
        }

        if (!unloadIsland) {
            return;
        }

        unloadIsland = this.plugin.getConfiguration().getBoolean("Island.Visitor.Unload");

        if (unloadIsland) {
            VisitManager visitManager = this.plugin.getVisitManager();
            visitManager.removeVisitors(island, VisitManager.Removal.UNLOADED);
            visitManager.unloadIsland(island.getOwnerUUID());

            BanManager banManager = this.plugin.getBanManager();
            banManager.unloadIsland(island.getOwnerUUID());
        } else {
            int nonIslandMembers = islandVisitors - getCoopPlayersAtIsland(island).size();

            if (nonIslandMembers <= 0) {
                if (island.getStatus() == IslandStatus.OPEN) {
                    return;
                } else if (player != null) {
                    removeCoopPlayers(island, player.getUniqueId());
                }
            } else {
                return;
            }
        }

        fileManager.unloadConfig(new File(new File(this.plugin.getDataFolder(), "coop-data"), island.getOwnerUUID() + ".yml"));
        fileManager.unloadConfig(new File(new File(this.plugin.getDataFolder(), "setting-data"), island.getOwnerUUID() + ".yml"));
        fileManager.unloadConfig(new File(new File(this.plugin.getDataFolder(), "island-data"), island.getOwnerUUID() + ".yml"));

        if (this.plugin.getConfiguration().getBoolean("Island.Challenge.PerIsland", true)) {
            fileManager.unloadConfig(new File(new File(this.plugin.getDataFolder(), "challenge-data"), island.getOwnerUUID() + ".yml"));
        }

        this.islandStorage.remove(island.getOwnerUUID());

        Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.getServer().getPluginManager().callEvent(new IslandUnloadEvent(island.getAPIWrapper())));
    }

    public void prepareIsland(Island island, IslandWorld world) {
        WorldManager worldManager = this.plugin.getWorldManager();
        FileManager fileManager = this.plugin.getFileManager();

        FileManager.Config config = fileManager.getConfig(new File(this.plugin.getDataFolder().toString() + "/island-data", island.getOwnerUUID() + ".yml"));

        if (config.getFileConfiguration().getString("Location." + world.getFriendlyName()) == null) {
            pasteStructure(island, world);
            return;
        }

        for (IslandEnvironment environmentList : IslandEnvironment.values()) {
            org.bukkit.Location location;

            if (environmentList == IslandEnvironment.ISLAND) {
                location = fileManager.getLocation(config, "Location." + world.getFriendlyName() + "." + environmentList.getFriendlyName(), true);
            } else {
                location = fileManager.getLocation(config, "Location." + world.getFriendlyName() + ".Spawn." + environmentList.getFriendlyName(), true);
            }

            island.addLocation(world, environmentList, worldManager.getLocation(location, world));
        }

        Bukkit.getServer().getScheduler().runTask(this.plugin, () -> removeSpawnProtection(island.getLocation(world, IslandEnvironment.ISLAND)));
    }

    public void resetIsland(Island island) {
        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            if (isIslandWorldUnlocked(island, worldList)) {
                pasteStructure(island, worldList);
            }
        }
    }

    public void pasteStructure(Island island, IslandWorld world) {
        if (!isIslandWorldUnlocked(island, world)) {
            return;
        }

        StructureManager structureManager = this.plugin.getStructureManager();
        FileManager fileManager = this.plugin.getFileManager();

        Structure structure;

        if (island.getStructure() != null && !island.getStructure().isEmpty() && structureManager.containsStructure(island.getStructure())) {
            structure = structureManager.getStructure(island.getStructure());
        } else {
            structure = structureManager.getStructures().get(0);
        }

        org.bukkit.Location islandLocation = prepareNextAvailableLocation(world);

        FileManager.Config config = fileManager.getConfig(new File(this.plugin.getDataFolder().toString() + "/island-data", island.getOwnerUUID() + ".yml"));

        for (IslandEnvironment environmentList : IslandEnvironment.values()) {
            if (environmentList == IslandEnvironment.ISLAND) {
                island.addLocation(world, environmentList, islandLocation);
                fileManager.setLocation(config, "Location." + world.getFriendlyName() + "." + environmentList.getFriendlyName(), islandLocation, true);
            } else {
                island.addLocation(world, environmentList, islandLocation.clone().add(0.5D, 0.0D, 0.5D));
                fileManager.setLocation(config, "Location." + world.getFriendlyName() + ".Spawn." + environmentList.getFriendlyName(), islandLocation.clone().add(0.5D, 0.0D, 0.5D), true);
            }
        }

        if (this.plugin.getConfiguration().getBoolean("Island.Spawn.Protection")) {
            Bukkit.getServer().getScheduler().runTask(this.plugin, () -> islandLocation.clone().subtract(0.0D, 1.0D, 0.0D).getBlock().setType(Material.STONE));
        }

        try {
            String structureFileName = null;
            switch (world) {
                case NORMAL:
                    structureFileName = structure.getOverworldFile();
                    break;
                case NETHER:
                    structureFileName = structure.getNetherFile();
                    break;
                case END:
                    structureFileName = structure.getEndFile();
                    break;
            }

            boolean isStructureFile = structureFileName.endsWith(".structure");
            File structureFile = new File(new File(this.plugin.getDataFolder() + "/" + (isStructureFile ? "structures" : "schematics")), structureFileName);

            Float[] direction;
            if (isStructureFile) {
                direction = StructureUtil.pasteStructure(StructureUtil.loadStructure(structureFile), island.getLocation(world, IslandEnvironment.ISLAND), BlockDegreesType.ROTATE_360);
            } else {
                direction = SchematicUtil.pasteSchematic(structureFile, island.getLocation(world, IslandEnvironment.ISLAND));
            }

            org.bukkit.Location spawnLocation = island.getLocation(world, IslandEnvironment.MAIN).clone();
            spawnLocation.setYaw(direction[0]);
            spawnLocation.setPitch(direction[1]);
            island.setLocation(world, IslandEnvironment.MAIN, spawnLocation);
            island.setLocation(world, IslandEnvironment.VISITOR, spawnLocation);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setNextAvailableLocation(world, islandLocation);
        saveNextAvailableLocation(world);
    }

    /**
     * Unlocks an island world and pastes the island structure there
     *
     * @param island      The island to unlock for
     * @param islandWorld The island world type to unlock
     */
    public void unlockIslandWorld(Island island, IslandWorld islandWorld) {
        FileManager fileManager = this.plugin.getFileManager();
        FileManager.Config islandData = fileManager.getConfig(new File(new File(this.plugin.getDataFolder().toString() + "/island-data"), island.getOwnerUUID().toString() + ".yml"));
        FileConfiguration configLoadIslandData = islandData.getFileConfiguration();

        configLoadIslandData.set("Unlocked." + islandWorld.getFriendlyName(), true);

        pasteStructure(island, islandWorld);

        // Recalculate island level after 5 seconds
        if (this.plugin.getConfiguration().getBoolean("Island.Levelling.ScanAutomatically")) {
            Bukkit.getServer().getScheduler().runTaskLater(this.plugin, () -> this.plugin.getLevellingManager().startScan(null, island), 100L);
        }
    }

    /**
     * Checks if an island world is unlocked
     *
     * @param island      The island to check
     * @param islandWorld The island world to check
     * @return true if the island world is unlocked, otherwise false
     */
    public boolean isIslandWorldUnlocked(Island island, IslandWorld islandWorld) {
        if (islandWorld == IslandWorld.NORMAL) {
            return true;
        }

        FileManager fileManager = this.plugin.getFileManager();
        FileManager.Config islandData = fileManager.getConfig(new File(new File(this.plugin.getDataFolder().toString() + "/island-data"), island.getOwnerUUID().toString() + ".yml"));
        FileConfiguration configLoadIslandData = islandData.getFileConfiguration();
        boolean unlocked = configLoadIslandData.getBoolean("Unlocked." + islandWorld.getFriendlyName());

        if (!unlocked) {
            FileConfiguration configLoad = this.plugin.getConfiguration();
            double price = configLoad.getDouble("Island.World." + islandWorld.getFriendlyName() + ".UnlockPrice");
            if (price == -1) {
                unlocked = true;
            }
        }

        return unlocked;
    }

    public Set<UUID> getVisitorsAtIsland(Island island) {
        Map<UUID, PlayerData> playerDataStorage = this.plugin.getPlayerDataManager().getPlayerData();
        Set<UUID> islandVisitors = new HashSet<>();

        synchronized (playerDataStorage) {
            for (UUID playerDataStorageList : playerDataStorage.keySet()) {
                PlayerData playerData = playerDataStorage.get(playerDataStorageList);
                UUID islandOwnerUUID = playerData.getIsland();

                if (islandOwnerUUID != null && islandOwnerUUID.equals(island.getOwnerUUID())) {
                    if (playerData.getOwner() == null || !playerData.getOwner().equals(island.getOwnerUUID())) {
                        if (Bukkit.getServer().getPlayer(playerDataStorageList) != null) {
                            islandVisitors.add(playerDataStorageList);
                        }
                    }
                }
            }
        }

        return islandVisitors;
    }

    public void visitIsland(Player player, Island island) {
        ScoreboardManager scoreboardManager = this.plugin.getScoreboardManager();
        FileConfiguration configLoad = this.plugin.getLanguage();

        if (island.hasRole(IslandRole.MEMBER, player.getUniqueId()) || island.hasRole(IslandRole.OPERATOR, player.getUniqueId()) || island.hasRole(IslandRole.OWNER, player.getUniqueId())) {
            Location loc = island.getLocation(IslandWorld.NORMAL, IslandEnvironment.MAIN);
            if (loc != null) {
                PaperLib.teleportAsync(player, loc);
                if (!configLoad.getBoolean("Island.Teleport.FallDamage", true)) {
                    player.setFallDistance(0.0F);
                }
            } else {
                player.sendMessage(this.plugin.formatText(this.plugin.getLanguage().getString("Island.Teleport.Unsafe.Message")));
            }
        } else {
            int islandVisitors = getVisitorsAtIsland(island).size();

            if (islandVisitors == 0) {
                for (Player loopPlayer : Bukkit.getOnlinePlayers()) {
                    PlayerData targetPlayerData = this.plugin.getPlayerDataManager().getPlayerData(loopPlayer);

                    if (targetPlayerData != null &&
                            targetPlayerData.getOwner() != null &&
                            targetPlayerData.getOwner().equals(island.getOwnerUUID())) {
                        scoreboardManager.updatePlayerScoreboardType(loopPlayer);
                    }
                }
            }
            Location loc = island.getLocation(IslandWorld.NORMAL, IslandEnvironment.VISITOR);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (this.plugin.getConfiguration().getBoolean("Island.Teleport.SafetyCheck", true)) {
                    Location safeLoc = LocationUtil.getSafeLocation(loc);
                    if (safeLoc != null) {
                        loc = safeLoc;
                    }
                }
            }
            if (loc != null) {
                PaperLib.teleportAsync(player, loc);
                if (!configLoad.getBoolean("Island.Teleport.FallDamage", true)) {
                    player.setFallDistance(0.0F);
                }
            } else {
                player.sendMessage(this.plugin.formatText(this.plugin.getLanguage().getString("Command.Island.Teleport.Unsafe.Message")));
            }
            if (!configLoad.getBoolean("Island.Teleport.FallDamage", true)) {
                player.setFallDistance(0.0F);
            }

            List<String> islandWelcomeMessage = island.getMessage(IslandMessage.WELCOME);

            if (this.plugin.getConfiguration().getBoolean("Island.Visitor.Welcome.Enable") && !islandWelcomeMessage.isEmpty()) {
                for (String islandWelcomeMessageList : islandWelcomeMessage) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', islandWelcomeMessageList));
                }
            }
        }

        player.closeInventory();
    }

    public void closeIsland(Island island) {
        MessageManager messageManager = this.plugin.getMessageManager();
        FileConfiguration configLoad = this.plugin.getLanguage();

        island.setStatus(IslandStatus.CLOSED);

        UUID islandOwnerUUID = island.getOwnerUUID();
        Player islandOwnerPlayer = Bukkit.getServer().getPlayer(islandOwnerUUID);
        String islandOwnerPlayerName;

        if (islandOwnerPlayer == null) {
            islandOwnerPlayerName = new OfflinePlayer(islandOwnerUUID).getName();
        } else {
            islandOwnerPlayerName = islandOwnerPlayer.getName();
        }

        for (UUID visitor : getVisitorsAtIsland(island)) {
            if (!island.isCoopPlayer(visitor)) {
                Player targetPlayer = Bukkit.getServer().getPlayer(visitor);
                LocationUtil.teleportPlayerToSpawn(targetPlayer);
                messageManager.sendMessage(targetPlayer, configLoad.getString("Island.Visit.Closed.Island.Message").replace("%player", islandOwnerPlayerName));
            }
        }
    }

    public void whitelistIsland(Island island) {
        MessageManager messageManager = this.plugin.getMessageManager();
        FileConfiguration configLoad = this.plugin.getLanguage();

        island.setStatus(IslandStatus.WHITELISTED);

        UUID islandOwnerUUID = island.getOwnerUUID();
        Player islandOwnerPlayer = Bukkit.getServer().getPlayer(islandOwnerUUID);
        String islandOwnerPlayerName;

        if (islandOwnerPlayer == null) {
            islandOwnerPlayerName = new OfflinePlayer(islandOwnerUUID).getName();
        } else {
            islandOwnerPlayerName = islandOwnerPlayer.getName();
        }

        for (UUID visitor : getVisitorsAtIsland(island)) {
            if (!island.isCoopPlayer(visitor) && !island.isPlayerWhitelisted(visitor)) {
                Player targetPlayer = Bukkit.getServer().getPlayer(visitor);
                LocationUtil.teleportPlayerToSpawn(targetPlayer);
                messageManager.sendMessage(targetPlayer, configLoad.getString("Command.Island.Visit.Whitelisted.Message").replace("%player", islandOwnerPlayerName)); // TODO
            }
        }
    }

    public Island getIsland(org.bukkit.OfflinePlayer offlinePlayer) {
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();

        UUID uuid = offlinePlayer.getUniqueId();
        if (this.islandProxies.containsKey(uuid)) {
            uuid = this.islandProxies.get(uuid);
        }

        // TODO: Find out how this can be fixed without this, for some reason
        // IslandManager tries to load PlayerDataManager before it's even loaded
        if (playerDataManager == null) {
            return null;
        }

        if (this.islandStorage.containsKey(uuid)) {
            return this.islandStorage.get(uuid);
        }

        Player player = offlinePlayer.getPlayer();
        if (offlinePlayer.isOnline() && player != null) {

            if (playerDataManager.hasPlayerData(player)) {
                PlayerData playerData = playerDataManager.getPlayerData(player);

                if (playerData.getOwner() != null && this.islandStorage.containsKey(playerData.getOwner())) {
                    return this.islandStorage.get(playerData.getOwner());
                }
            }
        } else {
            OfflinePlayer offlinePlayerData = new OfflinePlayer(offlinePlayer.getUniqueId());
            loadIsland(offlinePlayer);

            if (offlinePlayerData.getOwner() != null && this.islandStorage.containsKey(offlinePlayerData.getOwner())) {
                return this.islandStorage.get(offlinePlayerData.getOwner());
            }
        }

        return null;
    }

    public Island getIslandByUUID(UUID islandUUID) {
        for (Island island : this.islandStorage.values()) {
            if (island.getIslandUUID().equals(islandUUID)) {
                return island;
            }
        }
        return null;
    }

    public void removeIsland(UUID islandOwnerUUID) {
        this.islandStorage.remove(islandOwnerUUID);
    }

    public Map<UUID, Island> getIslands() {
        return this.islandStorage;
    }

    public boolean isIslandExist(UUID uuid) {
        return this.plugin.getFileManager().isFileExist(new File(new File(this.plugin.getDataFolder(), "island-data"), FastUUID.toString(uuid) + ".yml"));
    }

    /*
     * public boolean hasIsland(org.bukkit.OfflinePlayer offlinePlayer) { if
     * (offlinePlayer.isOnline()) { PlayerDataManager playerDataManager =
     * skyblock.getPlayerDataManager();
     *
     * Player player = offlinePlayer.getPlayer();
     *
     * if (playerDataManager.hasPlayerData(player)) { PlayerData playerData =
     * playerDataManager.getPlayerData(player);
     *
     * if (playerData.getOwner() != null &&
     * islandStorage.containsKey(playerData.getOwner())) { return true; } } }
     *
     * if (!isIslandExist(offlinePlayer.getUniqueId())) { return new
     * OfflinePlayer(offlinePlayer.getUniqueId()).getOwner() != null; }
     *
     * return false; }
     */

    public boolean containsIsland(UUID uuid) {
        return this.islandStorage.containsKey(uuid);
    }

    public void removeSpawnProtection(org.bukkit.Location location) {
        Block block = location.getBlock();

        if (CompatibleMaterial.getMaterial(block.getType()).orElse(null) == XMaterial.MOVING_PISTON) {
            block.setType(Material.AIR);
        }

        block = location.clone().add(0.0D, 1.0D, 0.0D).getBlock();

        if (CompatibleMaterial.getMaterial(block.getType()).orElse(null) == XMaterial.MOVING_PISTON) {
            block.setType(Material.AIR);
        }
    }

    public Set<UUID> getMembersOnline(Island island) {
        Set<UUID> membersOnline = new HashSet<>();

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (island.hasRole(IslandRole.MEMBER, all.getUniqueId()) || island.hasRole(IslandRole.OPERATOR, all.getUniqueId()) || island.hasRole(IslandRole.OWNER, all.getUniqueId())) {
                membersOnline.add(all.getUniqueId());
            }
        }

        return membersOnline;
    }

    public List<Player> getPlayersAtIsland(Island island) {
        List<Player> playersAtIsland = new ArrayList<>();

        if (island != null) {
            for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
                playersAtIsland.addAll(getPlayersAtIsland(island, worldList));
            }
        }

        return playersAtIsland;
    }

    public List<Player> getPlayersAtIsland(Island island, IslandWorld world) {
        List<Player> playersAtIsland = new ArrayList<>();

        if (island != null) {
            for (Player all : Bukkit.getOnlinePlayers()) {
                if (isPlayerAtIsland(island, all, world)) {
                    playersAtIsland.add(all);
                }
            }
        }

        return playersAtIsland;
    }

    public Island getIslandPlayerAt(Player player) {
        Preconditions.checkArgument(player != null, "Cannot get Island to null player");

        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();

        if (playerDataManager.hasPlayerData(player)) {
            PlayerData playerData = playerDataManager.getPlayerData(player);

            if (playerData.getIsland() != null) {
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(playerData.getIsland());
                return getIsland(offlinePlayer);
            }
        }

        return null;
    }

    public boolean isPlayerAtAnIsland(Player player) {
        PlayerDataManager playerDataManager = this.plugin.getPlayerDataManager();

        if (playerDataManager.hasPlayerData(player)) {
            PlayerData playerData = playerDataManager.getPlayerData(player);

            return playerData.getIsland() != null;
        }

        return false;
    }

    public void loadPlayer(Player player) {
        WorldManager worldManager = this.plugin.getWorldManager();

        Bukkit.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            if (worldManager.isIslandWorld(player.getWorld())) {
                IslandWorld world = worldManager.getIslandWorld(player.getWorld());
                Island island = getIslandAtLocation(player.getLocation());

                if (island != null) {
                    FileConfiguration configLoad = this.plugin.getConfiguration();

                    if (!island.isWeatherSynchronized()) {
                        player.setPlayerTime(island.getTime(), configLoad.getBoolean("Island.Weather.Time.Cycle"));
                        player.setPlayerWeather(island.getWeather());
                    }

                    updateFlight(player);

                    if (world == IslandWorld.NETHER) {
                        if (MajorServerVersion.isServerVersionBelow(MajorServerVersion.V1_13)) {
                            return;
                        }
                    }

                    double increment = island.getSize() % 2 != 0 ? 0.5d : 0.0d;

                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        if (configLoad.getBoolean("Island.WorldBorder.Enable") && island.isBorder()) {
                            Location islandLocation = island.getLocation(worldManager.getIslandWorld(player.getWorld()), IslandEnvironment.ISLAND);
                            if (islandLocation != null) {
                                Nms.getImplementations().getWorldBorder().send(player, island.getBorderColor(), island.getSize(), islandLocation.clone().add(increment, 0, increment));
                            }
                        } else {
                            Nms.getImplementations().getWorldBorder().send(player, null, 1.4999992E7D, new org.bukkit.Location(player.getWorld(), 0, 0, 0));
                        }
                    });
                }
            }
        });
    }

    public void updateFlightAtIsland(Island island) {
        for (Player player : getPlayersAtIsland(island)) {
            this.updateFlight(player);
        }
    }

    public void updateFlight(Player player) {
        // The player can fly in other worlds if they are in creative or have another
        // plugin's fly permission.


        // Residence support
        if (Bukkit.getServer().getPluginManager().getPlugin("Residence") != null) {
            ClaimedResidence res = Residence.getInstance().getResidenceManagerAPI().getByLoc(player.getLocation());
            if (res != null) {
                if (res.getPermissions().has(Flags.fly, false) || res.getPermissions().has(Flags.nofly, false)) {
                    return;
                }
            }
        }

        Island island = getIslandAtLocation(player.getLocation());

        UpgradeManager upgradeManager = this.plugin.getUpgradeManager();
        List<Upgrade> flyUpgrades = upgradeManager.getUpgrades(Upgrade.Type.FLY);
        boolean isFlyUpgradeEnabled = flyUpgrades != null && !flyUpgrades.isEmpty() && flyUpgrades.get(0).isEnabled();
        boolean setPlayerFlying = false;
        if (isFlyUpgradeEnabled) {
            boolean upgradeEnabled = island != null && island.isUpgrade(Upgrade.Type.FLY);
            setPlayerFlying = upgradeEnabled;
            Bukkit.getServer().getScheduler().runTask(this.plugin, () -> player.setAllowFlight(upgradeEnabled));
        }

        if (island == null || setPlayerFlying) {
            return;
        }

        boolean hasGlobalFlyPermission = player.hasPermission("fabledskyblock.*") || player.hasPermission("fabledskyblock.fly.*");
        boolean hasOwnIslandFlyPermission = player.hasPermission("fabledskyblock.fly") && island.getRole(player) != null && island.getRole(player) != IslandRole.VISITOR;
        if (hasGlobalFlyPermission || hasOwnIslandFlyPermission || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR || player.hasPermission("essentials.fly") || player.hasPermission("cmi.command.fly")) {
            WorldManager worldManager = this.plugin.getWorldManager();
            boolean canFlyInWorld = worldManager.isIslandWorld(player.getWorld());
            Bukkit.getServer().getScheduler().runTask(this.plugin, () -> player.setAllowFlight(canFlyInWorld));
        }
    }

    public Set<UUID> getCoopPlayersAtIsland(Island island) {
        final Set<UUID> coopPlayersAtIsland = new HashSet<>();

        if (island == null) {
            return coopPlayersAtIsland;
        }

        for (UUID coopUUID : island.getCoopPlayers().keySet()) {

            final Player player = Bukkit.getPlayer(coopUUID);

            if (player == null) {
                continue;
            }

            if (isPlayerAtIsland(island, player)) {
                coopPlayersAtIsland.add(coopUUID);
            }
        }

        return coopPlayersAtIsland;
    }

    public boolean removeCoopPlayers(Island island, UUID uuid) {
        MessageManager messageManager = this.plugin.getMessageManager();
        SoundManager soundManager = this.plugin.getSoundManager();

        FileConfiguration configLoad = this.plugin.getLanguage();

        boolean coopPlayers = island.hasPermission(IslandRole.OPERATOR, this.plugin.getPermissionManager().getPermission("CoopPlayers"));

        for (Player all : Bukkit.getOnlinePlayers()) {
            if (uuid != null && all.getUniqueId().equals(uuid)) {
                continue;
            }

            if (island.hasRole(IslandRole.OWNER, all.getUniqueId())) {
                return false;
            } else if (coopPlayers && island.hasRole(IslandRole.OPERATOR, all.getUniqueId())) {
                return false;
            }
        }

        for (UUID coopPlayerAtIslandList : getCoopPlayersAtIsland(island)) {
            Player targetPlayer = Bukkit.getServer().getPlayer(coopPlayerAtIslandList);

            if (island.getCoopType(coopPlayerAtIslandList) == IslandCoop.NORMAL) {
                continue;
            }

            if (targetPlayer != null) {
                LocationUtil.teleportPlayerToSpawn(targetPlayer);

                if (coopPlayers) {
                    messageManager.sendMessage(targetPlayer, configLoad.getString("Island.Coop.Removed.Operator.Message"));
                } else {
                    messageManager.sendMessage(targetPlayer, configLoad.getString("Island.Coop.Removed.Owner.Message"));
                }

                soundManager.playSound(targetPlayer, XSound.ENTITY_IRON_GOLEM_ATTACK);
            }
        }

        return true;
    }

    public int getIslandSafeLevel(Island island) {
        FileConfiguration configLoad = this.plugin.getConfiguration();

        int safeLevel = 0;

        Map<String, Boolean> settings = new HashMap<>();
        settings.put("KeepItemsOnDeath", false);
        settings.put("PvP", true);
        settings.put("Damage", true);

        for (String settingList : settings.keySet()) {
            if (configLoad.getBoolean("Island.Settings." + settingList + ".Enable") &&
                    island.hasPermission(IslandRole.OWNER, this.plugin.getPermissionManager().getPermission(settingList)) ==
                            settings.get(settingList)) {
                safeLevel++;
            }
        }

        return safeLevel;
    }

    public void updateBorder(Island island) {
        WorldManager worldManager = this.plugin.getWorldManager();

        if (island.isBorder()) {
            if (this.plugin.getConfiguration().getBoolean("Island.WorldBorder.Enable")) {
                double increment = island.getSize() % 2 != 0 ? 0.5d : 0.0d;

                for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
                    if (worldList != IslandWorld.NETHER || MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            for (Player all : getPlayersAtIsland(island)) {
                                Nms.getImplementations().getWorldBorder().send(all, island.getBorderColor(), island.getSize(), island.getLocation(worldManager.getIslandWorld(all.getWorld()), IslandEnvironment.ISLAND).clone().add(increment, 0, increment));
                            }
                        });
                    }

                }
            }
        } else {
            for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
                if (worldList != IslandWorld.NETHER || MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        for (Player all : getPlayersAtIsland(island)) {
                            Nms.getImplementations().getWorldBorder().send(all, null, 1.4999992E7D, new Location(all.getWorld(), 0, 0, 0));
                        }
                    });
                }

            }
        }
    }

    public List<Island> getCoopIslands(Player player) {
        List<Island> islands = new ArrayList<>();

        for (Island island : getIslands().values()) {
            if (island.getCoopPlayers().containsKey(player.getUniqueId())) {
                islands.add(island);
            }
        }

        return islands;
    }

    public Island getIslandAtLocation(org.bukkit.Location location) {
        for (Island island : new ArrayList<>(getIslands().values())) {
            if (isLocationAtIsland(island, location)) {
                return island;
            }
        }

        return null;
    }

    public boolean isPlayerProxyingAnotherPlayer(UUID proxying) {
        return this.islandProxies.containsKey(proxying);
    }

    public boolean isPlayerProxyingAnotherPlayer(UUID proxying, UUID proxied) {
        return this.islandProxies.containsKey(proxying) && this.islandProxies.get(proxying) == proxied;
    }

    public UUID getPlayerProxyingAnotherPlayer(UUID proxying) {
        return this.islandProxies.get(proxying);
    }

    public void addProxiedPlayer(UUID toProxy, UUID proxied) {
        this.islandProxies.put(toProxy, proxied);
    }

    public void removeProxyingPlayer(UUID toProxy) {
        this.islandProxies.remove(toProxy);
    }

    public boolean isPlayerAtIsland(Island island, Player player) {
        return isLocationAtIsland(island, player.getLocation());
    }

    public boolean isPlayerAtIsland(Island island, Player player, IslandWorld world) {
        return isLocationAtIsland(island, player.getLocation(), world);
    }

    public boolean isLocationAtIsland(Island island, org.bukkit.Location location) {
        for (IslandWorld worldList : IslandWorld.getIslandWorlds()) {
            if (isLocationAtIsland(island, location, worldList)) {
                return true;
            }
        }

        return false;
    }

    public boolean isLocationAtIsland(Island island, org.bukkit.Location location, IslandWorld world) {
        Location islandLocation = island.getLocation(world, IslandEnvironment.ISLAND);
        if (islandLocation != null && location.getWorld().equals(islandLocation.getWorld())) {
            double locIncrement = island.getSize() % 2d != 0d ? 0.50d + Double.MIN_VALUE : -Double.MIN_VALUE;
            return LocationUtil.isLocationInLocationRadius(
                    islandLocation.clone().add(locIncrement, 0d, locIncrement),
                    LocationUtil.toCenterLocation(location),
                    island.getRadius() + Math.round(locIncrement));
        }
        return false;
    }

    public Island getIslandByOwner(org.bukkit.OfflinePlayer player) {
        if (this.islandStorage.containsKey(player.getUniqueId())) {
            return this.islandStorage.get(player.getUniqueId());
        }
        return null;
    }
}
