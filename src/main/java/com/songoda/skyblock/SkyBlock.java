package com.songoda.skyblock;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.compatibility.MajorServerVersion;
import com.songoda.core.compatibility.ServerProject;
import com.songoda.core.configuration.Config;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.HologramManager;
import com.songoda.core.hooks.LogManager;
import com.songoda.skyblock.api.SkyBlockAPI;
import com.songoda.skyblock.ban.BanManager;
import com.songoda.skyblock.bank.BankManager;
import com.songoda.skyblock.biome.BiomeManager;
import com.songoda.skyblock.challenge.FabledChallenge;
import com.songoda.skyblock.command.CommandManager;
import com.songoda.skyblock.command.commands.SkyBlockCommand;
import com.songoda.skyblock.config.FileManager;
import com.songoda.skyblock.confirmation.ConfirmationTask;
import com.songoda.skyblock.cooldown.CooldownManager;
import com.songoda.skyblock.economy.EconomyManager;
import com.songoda.skyblock.generator.GeneratorManager;
import com.songoda.skyblock.invite.InviteManager;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.reward.RewardManager;
import com.songoda.skyblock.leaderboard.LeaderboardManager;
import com.songoda.skyblock.levelling.IslandLevelManager;
import com.songoda.skyblock.limit.LimitationInstanceHandler;
import com.songoda.skyblock.listeners.BlockListeners;
import com.songoda.skyblock.listeners.BucketListeners;
import com.songoda.skyblock.listeners.ChatListeners;
import com.songoda.skyblock.listeners.DeathListeners;
import com.songoda.skyblock.listeners.EntityListeners;
import com.songoda.skyblock.listeners.FallBreakListeners;
import com.songoda.skyblock.listeners.FoodListeners;
import com.songoda.skyblock.listeners.GrowListeners;
import com.songoda.skyblock.listeners.InteractListeners;
import com.songoda.skyblock.listeners.InventoryListeners;
import com.songoda.skyblock.listeners.ItemListeners;
import com.songoda.skyblock.listeners.JoinListeners;
import com.songoda.skyblock.listeners.MoveListeners;
import com.songoda.skyblock.listeners.PistonListeners;
import com.songoda.skyblock.listeners.PortalListeners;
import com.songoda.skyblock.listeners.ProjectileListeners;
import com.songoda.skyblock.listeners.QuitListeners;
import com.songoda.skyblock.listeners.RespawnListeners;
import com.songoda.skyblock.listeners.SpawnerListeners;
import com.songoda.skyblock.listeners.SpongeListeners;
import com.songoda.skyblock.listeners.TeleportListeners;
import com.songoda.skyblock.listeners.WorldListeners;
import com.songoda.skyblock.listeners.hooks.EpicSpawners;
import com.songoda.skyblock.listeners.hooks.UltimateStacker;
import com.songoda.skyblock.localization.LocalizationManager;
import com.songoda.skyblock.menus.admin.Creator;
import com.songoda.skyblock.menus.admin.Generator;
import com.songoda.skyblock.menus.admin.Levelling;
import com.songoda.skyblock.message.MessageManager;
import com.songoda.skyblock.permission.PermissionManager;
import com.songoda.skyblock.permission.permissions.listening.SignEditPermission;
import com.songoda.skyblock.placeholder.PlaceholderManager;
import com.songoda.skyblock.playerdata.PlayerDataManager;
import com.songoda.skyblock.playtime.PlaytimeTask;
import com.songoda.skyblock.scoreboard.ScoreboardManager;
import com.songoda.skyblock.sound.SoundManager;
import com.songoda.skyblock.stackable.StackableManager;
import com.songoda.skyblock.structure.StructureManager;
import com.songoda.skyblock.tasks.HologramTask;
import com.songoda.skyblock.tasks.MobNetherWaterTask;
import com.songoda.skyblock.upgrade.UpgradeManager;
import com.songoda.skyblock.usercache.UserCacheManager;
import com.songoda.skyblock.visit.VisitManager;
import com.songoda.skyblock.visit.VisitTask;
import com.songoda.skyblock.world.WorldManager;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class SkyBlock extends SongodaPlugin {
    private FileManager fileManager;
    private final WorldManager worldManager = new WorldManager(this);
    private UserCacheManager userCacheManager;
    private VisitManager visitManager;
    private BanManager banManager;
    private IslandManager islandManager;
    private UpgradeManager upgradeManager;
    private PlayerDataManager playerDataManager;
    private CooldownManager cooldownManager;
    private ScoreboardManager scoreboardManager;
    private InviteManager inviteManager;
    private BiomeManager biomeManager;
    private IslandLevelManager levellingManager;
    private EconomyManager economyManager;
    private CommandManager commandManager;
    private StructureManager structureManager;
    private StackableManager stackableManager;
    private SoundManager soundManager;
    private GeneratorManager generatorManager;
    private LeaderboardManager leaderboardManager;
    private PlaceholderManager placeholderManager;
    private MessageManager messageManager;
    private HologramTask hologramTask;
    private MobNetherWaterTask mobNetherWaterTask;
    private LimitationInstanceHandler limitationHandler;
    private LocalizationManager localizationManager;
    private RewardManager rewardManager;
    private FabledChallenge fabledChallenge;
    private BankManager bankManager;
    private PermissionManager permissionManager;

    private Permission vaultPermission;

    private boolean paper;
    private boolean paperAsync;

    private final GuiManager guiManager = new GuiManager(this);

    /**
     * @deprecated Use {@link org.bukkit.plugin.java.JavaPlugin#getPlugin(Class)} instead
     */
    @Deprecated
    public static SkyBlock getInstance() {
        return getPlugin(SkyBlock.class);
    }

    // Add ymlFiles to cache
    private FileConfiguration biomes;
    private FileConfiguration challenges;
    private FileConfiguration config;
    private FileConfiguration generators;
    private FileConfiguration language;
    private FileConfiguration levelling;
    private FileConfiguration limits;
    private FileConfiguration menus;
    private FileConfiguration placeholders;
    private FileConfiguration rewards;
    private FileConfiguration scoreboard;
    private FileConfiguration settings;
    private FileConfiguration stackables;
    private FileConfiguration upgrades;

    @Override
    public void onPluginLoad() {
    }

    @Override
    public void onPluginEnable() {
        if (MajorServerVersion.isServerVersionAbove(MajorServerVersion.V1_21) || MajorServerVersion.isServerVersionBelow(MajorServerVersion.V1_8)) {
            this.getLogger().warning("This Minecraft version is not officially supported.");
        }

        this.paper = ServerProject.isServer(ServerProject.PAPER);
        if (this.paper) {
            try {
                Bukkit.spigot().getClass().getMethod("getPaperConfig");
                if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_16)) {
                    this.paperAsync = true;
                } else {
                    this.paperAsync = MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13) &&
                            Bukkit.spigot().getPaperConfig().getBoolean("settings.async-chunks.enable", false);
                }
            } catch (NoSuchMethodException ignored) {
                this.paperAsync = false;
            }
            this.getLogger().info("Enabling Paper hooks");
        }

        SongodaCore.registerPlugin(this, 17, XMaterial.GRASS_BLOCK);

        // Load Economy
        this.economyManager = new EconomyManager(this);

        // Load Holograms
        HologramManager.load(this);

        this.fileManager = new FileManager(this);

        if (!loadConfigs()) {
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.permissionManager = new PermissionManager(this);
        this.localizationManager = new LocalizationManager();
        this.worldManager.loadWorlds();
        this.userCacheManager = new UserCacheManager(this);
        this.visitManager = new VisitManager(this);
        this.banManager = new BanManager(this);
        this.islandManager = new IslandManager(this);
        this.upgradeManager = new UpgradeManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.limitationHandler = new LimitationInstanceHandler();
        this.fabledChallenge = new FabledChallenge(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.inviteManager = new InviteManager(this);
        this.biomeManager = new BiomeManager(this);
        this.levellingManager = new IslandLevelManager(this);
        this.commandManager = new CommandManager(this);
        this.structureManager = new StructureManager(this);
        this.soundManager = new SoundManager(this);

        if (this.config.getBoolean("Island.Generator.Enable")) {
            this.generatorManager = new GeneratorManager(this);
        }

        if (this.config.getBoolean("Island.Stackable.Enable")) {
            this.stackableManager = new StackableManager(this);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> this.stackableManager.loadSavedStackables(), 5L);
        }

        this.leaderboardManager = new LeaderboardManager(this);

        this.placeholderManager = new PlaceholderManager(this);
        this.placeholderManager.registerPlaceholders();

        this.messageManager = new MessageManager(this);

        this.rewardManager = new RewardManager(this);
        this.rewardManager.loadRewards();

        this.bankManager = new BankManager(this);

        if (this.config.getBoolean("Island.Task.PlaytimeTask")) {
            new PlaytimeTask(this.playerDataManager, this.islandManager).runTaskTimerAsynchronously(this, 0L, 20L);
        }

        if (this.config.getBoolean("Island.Task.VisitTask")) {
            new VisitTask(this.playerDataManager).runTaskTimerAsynchronously(this, 0L, 20L);
        }

        new ConfirmationTask(this.playerDataManager).runTaskTimerAsynchronously(this, 0L, 20L);

        // Start Tasks
        this.hologramTask = HologramTask.startTask(this);
        this.mobNetherWaterTask = MobNetherWaterTask.startTask(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new JoinListeners(this), this);
        pluginManager.registerEvents(new QuitListeners(this), this);
        pluginManager.registerEvents(new BlockListeners(this), this);
        pluginManager.registerEvents(new InteractListeners(this), this);
        pluginManager.registerEvents(new EntityListeners(this), this);
        pluginManager.registerEvents(new BucketListeners(this), this);
        pluginManager.registerEvents(new ProjectileListeners(this), this);
        pluginManager.registerEvents(new InventoryListeners(this), this);
        pluginManager.registerEvents(new ItemListeners(this), this);
        pluginManager.registerEvents(new TeleportListeners(this), this);
        pluginManager.registerEvents(new PortalListeners(this), this);
        pluginManager.registerEvents(new MoveListeners(this), this);
        pluginManager.registerEvents(new DeathListeners(this), this);
        pluginManager.registerEvents(new RespawnListeners(this), this);
        pluginManager.registerEvents(new ChatListeners(this), this);
        pluginManager.registerEvents(new SpawnerListeners(this), this);
        pluginManager.registerEvents(new FoodListeners(this), this);
        pluginManager.registerEvents(new GrowListeners(this), this);
        pluginManager.registerEvents(new PistonListeners(this), this);
        pluginManager.registerEvents(new FallBreakListeners(this), this);
        pluginManager.registerEvents(new WorldListeners(this), this);
        pluginManager.registerEvents(new SignEditPermission(this), this);

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
            pluginManager.registerEvents(new SpongeListeners(this), this);
        }

        if (pluginManager.isPluginEnabled("EpicSpawners")) {
            pluginManager.registerEvents(new EpicSpawners(this), this);
        }
        if (pluginManager.isPluginEnabled("UltimateStacker")) {
            pluginManager.registerEvents(new UltimateStacker(this), this);
        }

        pluginManager.registerEvents(new Levelling(), this);
        pluginManager.registerEvents(new Generator(), this);
        pluginManager.registerEvents(new Creator(), this);

        this.getCommand("skyblock").setExecutor(new SkyBlockCommand(this));

        if (pluginManager.isPluginEnabled("Vault")) {
            this.vaultPermission = getServer().getServicesManager().getRegistration(Permission.class).getProvider();
        }

        switch (this.config.getString("Economy.Manager", "Default")) {
            case "Vault":
                getEconomyManager().setEconomy("Vault");
                break;
            case "PlayerPoints":
                getEconomyManager().setEconomy("PlayerPoints");
                break;
            case "Reserve":
                getEconomyManager().setEconomy("Reserve");
                break;
            default:
                this.getLogger().warning("EconomyManager is default");
        }

        LogManager.load();

        SkyBlockAPI.setImplementation(this);
    }

    @Override
    public void onPluginDisable() {
        if (this.userCacheManager != null) {
            this.userCacheManager.onDisable();
        }
        if (this.scoreboardManager != null) {
            this.scoreboardManager.disable();
        }
        if (this.islandManager != null) {
            this.islandManager.onDisable();
        }
        if (this.visitManager != null) {
            this.visitManager.onDisable();
        }
        if (this.banManager != null) {
            this.banManager.onDisable();
        }
        if (this.playerDataManager != null) {
            this.playerDataManager.onDisable();
        }
        if (this.cooldownManager != null) {
            this.cooldownManager.onDisable();
        }
        if (this.hologramTask != null) {
            this.hologramTask.onDisable();
        }
        if (this.mobNetherWaterTask != null) {
            this.mobNetherWaterTask.onDisable();
        }
        if (this.fabledChallenge != null) {
            this.fabledChallenge.onDisable();
        }

        HandlerList.unregisterAll(this);
    }

    @Override
    public void onDataLoad() {
    }

    @Override
    public void onConfigReload() {
        if (!loadConfigs()) {
            this.getLogger().warning("Config are not reload !");
        } else {
            this.getLogger().info("Configurations Loaded !");
        }
    }

    @Override
    public List<Config> getExtraConfig() {
        return null;
    }

    private boolean loadConfigs() {
        try {
            this.biomes = this.getFileManager().getConfig(new File(this.getDataFolder(), "biomes.yml")).getFileConfiguration();
            this.challenges = this.getFileManager().getConfig(new File(this.getDataFolder(), "challenges.yml")).getFileConfiguration();
            this.config = this.getFileManager().getConfig(new File(this.getDataFolder(), "config.yml")).getFileConfiguration();
            this.generators = this.getFileManager().getConfig(new File(this.getDataFolder(), "generators.yml")).getFileConfiguration();
            this.language = this.getFileManager().getConfig(new File(this.getDataFolder(), "language.yml")).getFileConfiguration();
            this.levelling = this.getFileManager().getConfig(new File(this.getDataFolder(), "levelling.yml")).getFileConfiguration();
            this.limits = this.getFileManager().getConfig(new File(this.getDataFolder(), "limits.yml")).getFileConfiguration();
            this.menus = this.getFileManager().getConfig(new File(this.getDataFolder(), "menus.yml")).getFileConfiguration();
            this.placeholders = this.getFileManager().getConfig(new File(this.getDataFolder(), "placeholders.yml")).getFileConfiguration();
            this.rewards = this.getFileManager().getConfig(new File(this.getDataFolder(), "rewards.yml")).getFileConfiguration();
            this.scoreboard = this.getFileManager().getConfig(new File(this.getDataFolder(), "scoreboard.yml")).getFileConfiguration();
            this.settings = this.getFileManager().getConfig(new File(this.getDataFolder(), "settings.yml")).getFileConfiguration();
            this.stackables = this.getFileManager().getConfig(new File(this.getDataFolder(), "stackables.yml")).getFileConfiguration();
            this.upgrades = this.getFileManager().getConfig(new File(this.getDataFolder(), "upgrades.yml")).getFileConfiguration();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public String formatText(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    public FileManager getFileManager() {
        return this.fileManager;
    }

    public WorldManager getWorldManager() {
        return this.worldManager;
    }

    public UserCacheManager getUserCacheManager() {
        return this.userCacheManager;
    }

    public VisitManager getVisitManager() {
        return this.visitManager;
    }

    public BanManager getBanManager() {
        return this.banManager;
    }

    public BankManager getBankManager() {
        return this.bankManager;
    }

    public IslandManager getIslandManager() {
        return this.islandManager;
    }

    public UpgradeManager getUpgradeManager() {
        return this.upgradeManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return this.playerDataManager;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public void setScoreboardManager(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public InviteManager getInviteManager() {
        return this.inviteManager;
    }

    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    public IslandLevelManager getLevellingManager() {
        return this.levellingManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public StructureManager getStructureManager() {
        return this.structureManager;
    }

    public SoundManager getSoundManager() {
        return this.soundManager;
    }

    public GeneratorManager getGeneratorManager() {
        return this.generatorManager;
    }

    public void setGeneratorManager(GeneratorManager generatorManager) {
        this.generatorManager = generatorManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return this.leaderboardManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return this.placeholderManager;
    }

    public MessageManager getMessageManager() {
        return this.messageManager;
    }

    public HologramTask getHologramTask() {
        return this.hologramTask;
    }

    public MobNetherWaterTask getMobNetherWaterTask() {
        return this.mobNetherWaterTask;
    }

    public StackableManager getStackableManager() {
        return this.stackableManager;
    }

    public LimitationInstanceHandler getLimitationHandler() {
        return this.limitationHandler;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return this.worldManager.getWorldGeneratorForMapName(worldName);
    }

    public LocalizationManager getLocalizationManager() {
        return this.localizationManager;
    }

    public RewardManager getRewardManager() {
        return this.rewardManager;
    }

    public FabledChallenge getFabledChallenge() {
        return this.fabledChallenge;
    }

    public PermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    public GuiManager getGuiManager() {
        return this.guiManager;
    }

    public boolean isPaper() {
        return this.paper;
    }

    public boolean isPaperAsync() {
        return this.paperAsync;
    }

    public Permission getVaultPermission() {
        return this.vaultPermission;
    }

    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public FileConfiguration getBiomes() {
        return this.biomes;
    }

    public FileConfiguration getChallenges() {
        return this.challenges;
    }

    public FileConfiguration getConfiguration() {
        return this.config;
    }

    public FileConfiguration getGenerators() {
        return this.generators;
    }

    public FileConfiguration getLanguage() {
        return this.language;
    }

    public FileConfiguration getLevelling() {
        return this.levelling;
    }

    public FileConfiguration getLimits() {
        return this.limits;
    }

    public FileConfiguration getMenus() {
        return this.menus;
    }

    public FileConfiguration getPlaceholders() {
        return this.placeholders;
    }

    public FileConfiguration getRewards() {
        return this.rewards;
    }

    public FileConfiguration getSettings() {
        return this.settings;
    }

    public FileConfiguration getStackables() {
        return this.stackables;
    }

    public FileConfiguration getUpgrades() {
        return this.upgrades;
    }

    public FileConfiguration getScoreboard() {
        return this.scoreboard;
    }
}
