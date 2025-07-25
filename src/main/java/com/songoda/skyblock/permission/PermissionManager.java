package com.songoda.skyblock.permission;

import com.songoda.core.compatibility.MajorServerVersion;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.config.FileManager;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandRole;
import com.songoda.skyblock.permission.event.Stoppable;
import com.songoda.skyblock.permission.permissions.basic.BanPermission;
import com.songoda.skyblock.permission.permissions.basic.BiomePermission;
import com.songoda.skyblock.permission.permissions.basic.BorderPermission;
import com.songoda.skyblock.permission.permissions.basic.CoopPlayersPermission;
import com.songoda.skyblock.permission.permissions.basic.FireSpreadPermission;
import com.songoda.skyblock.permission.permissions.basic.IslandPermission;
import com.songoda.skyblock.permission.permissions.basic.KeepItemsOnDeathPermission;
import com.songoda.skyblock.permission.permissions.basic.KickPermission;
import com.songoda.skyblock.permission.permissions.basic.LeafDecayPermission;
import com.songoda.skyblock.permission.permissions.basic.MainSpawnPermission;
import com.songoda.skyblock.permission.permissions.basic.MemberPermission;
import com.songoda.skyblock.permission.permissions.basic.NaturalMobSpawningPermission;
import com.songoda.skyblock.permission.permissions.basic.UnbanPermission;
import com.songoda.skyblock.permission.permissions.basic.VisitorPermission;
import com.songoda.skyblock.permission.permissions.basic.VisitorSpawnPermission;
import com.songoda.skyblock.permission.permissions.basic.WeatherPermission;
import com.songoda.skyblock.permission.permissions.listening.*;
import com.songoda.skyblock.permission.permissions.listening.AnimalBreedingPermission;
import com.songoda.skyblock.permission.permissions.listening.AnvilPermission;
import com.songoda.skyblock.permission.permissions.listening.ArmorStandUsePermission;
import com.songoda.skyblock.permission.permissions.listening.BeaconPermission;
import com.songoda.skyblock.permission.permissions.listening.BedPermission;
import com.songoda.skyblock.permission.permissions.listening.BoatPermission;
import com.songoda.skyblock.permission.permissions.listening.BrewingPermission;
import com.songoda.skyblock.permission.permissions.listening.BucketPermission;
import com.songoda.skyblock.permission.permissions.listening.CakePermission;
import com.songoda.skyblock.permission.permissions.listening.CauldronPermission;
import com.songoda.skyblock.permission.permissions.listening.DamagePermission;
import com.songoda.skyblock.permission.permissions.listening.DestroyPermission;
import com.songoda.skyblock.permission.permissions.listening.DoorPermission;
import com.songoda.skyblock.permission.permissions.listening.DragonEggUsePermission;
import com.songoda.skyblock.permission.permissions.listening.DropperDispenserPermission;
import com.songoda.skyblock.permission.permissions.listening.EnchantPermission;
import com.songoda.skyblock.permission.permissions.listening.EntityPlacementPermission;
import com.songoda.skyblock.permission.permissions.listening.ExperienceOrbPickupPermission;
import com.songoda.skyblock.permission.permissions.listening.ExplosionsPermission;
import com.songoda.skyblock.permission.permissions.listening.FirePermission;
import com.songoda.skyblock.permission.permissions.listening.FishingPermission;
import com.songoda.skyblock.permission.permissions.listening.FurnacePermission;
import com.songoda.skyblock.permission.permissions.listening.GatePermission;
import com.songoda.skyblock.permission.permissions.listening.HangingDestroyPermission;
import com.songoda.skyblock.permission.permissions.listening.HopperPermission;
import com.songoda.skyblock.permission.permissions.listening.HorseInventoryPermission;
import com.songoda.skyblock.permission.permissions.listening.HungerPermission;
import com.songoda.skyblock.permission.permissions.listening.ItemDropPermission;
import com.songoda.skyblock.permission.permissions.listening.ItemPickupPermission;
import com.songoda.skyblock.permission.permissions.listening.JukeboxPermission;
import com.songoda.skyblock.permission.permissions.listening.LeashPermission;
import com.songoda.skyblock.permission.permissions.listening.LeverButtonPermission;
import com.songoda.skyblock.permission.permissions.listening.MilkingPermission;
import com.songoda.skyblock.permission.permissions.listening.MinecartPermission;
import com.songoda.skyblock.permission.permissions.listening.MobGriefingPermission;
import com.songoda.skyblock.permission.permissions.listening.MobHurtingPermission;
import com.songoda.skyblock.permission.permissions.listening.MobRidingPermission;
import com.songoda.skyblock.permission.permissions.listening.MobTamingPermission;
import com.songoda.skyblock.permission.permissions.listening.MonsterHurtingPermission;
import com.songoda.skyblock.permission.permissions.listening.NoteblockPermission;
import com.songoda.skyblock.permission.permissions.listening.PlacePermission;
import com.songoda.skyblock.permission.permissions.listening.PortalPermission;
import com.songoda.skyblock.permission.permissions.listening.PressurePlatePermission;
import com.songoda.skyblock.permission.permissions.listening.ProjectilePermission;
import com.songoda.skyblock.permission.permissions.listening.ProtectorDamagePermission;
import com.songoda.skyblock.permission.permissions.listening.PvpPermission;
import com.songoda.skyblock.permission.permissions.listening.RedstonePermission;
import com.songoda.skyblock.permission.permissions.listening.ShearingPermission;
import com.songoda.skyblock.permission.permissions.listening.SignEditPermission;
import com.songoda.skyblock.permission.permissions.listening.SpawnEggPermission;
import com.songoda.skyblock.permission.permissions.listening.StoragePermission;
import com.songoda.skyblock.permission.permissions.listening.TradingPermission;
import com.songoda.skyblock.permission.permissions.listening.TramplePermission;
import com.songoda.skyblock.permission.permissions.listening.TrapdoorPermission;
import com.songoda.skyblock.permission.permissions.listening.WaterCollectionPermission;
import com.songoda.skyblock.permission.permissions.listening.WorkbenchPermission;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionManager {
    private final SkyBlock plugin;

    private final Map<String, BasicPermission> registeredPermissions = new HashMap<>();
    private List<HandlerWrapper> registeredHandlers = new LinkedList<>();

    public PermissionManager(SkyBlock plugin) {
        this.plugin = plugin;

        // Load default permissions.
        registerPermissions( // TODO Reload them with /is admin reload - Fabrimat
                //Listening
                new StoragePermission(plugin),
                new DragonEggUsePermission(plugin),
                new BeaconPermission(plugin),
                new ProjectilePermission(plugin),
                new DestroyPermission(plugin),
                new AnvilPermission(plugin),
                new BedPermission(plugin),
                new BrewingPermission(plugin),
                new WorkbenchPermission(plugin),
                new DoorPermission(plugin),
                new EnchantPermission(plugin),
                new FurnacePermission(plugin),
                new LeverButtonPermission(plugin),
                new JukeboxPermission(plugin),
                new HopperPermission(plugin),
                new NoteblockPermission(plugin),
                new RedstonePermission(plugin),
                new GatePermission(plugin),
                new DropperDispenserPermission(plugin),
                new BucketPermission(plugin),
                new WaterCollectionPermission(plugin),
                new SpawnEggPermission(plugin),
                new EntityPlacementPermission(plugin),
                new FirePermission(plugin),
                new TramplePermission(plugin),
                new PressurePlatePermission(plugin),
                new CakePermission(plugin),
                new TrapdoorPermission(plugin),
                new PlacePermission(plugin),
                new LeashPermission(plugin),
                new AnimalBreedingPermission(plugin),
                new MinecartPermission(plugin),
                new BoatPermission(plugin),
                new TradingPermission(plugin),
                new MilkingPermission(plugin),
                new ShearingPermission(plugin),
                new MobRidingPermission(plugin),
                new HorseInventoryPermission(plugin),
                new MobHurtingPermission(plugin),
                new ArmorStandUsePermission(plugin),
                new MonsterHurtingPermission(plugin),
                new HangingDestroyPermission(plugin),
                new ExplosionsPermission(plugin),
                new MobTamingPermission(plugin),
                new MobGriefingPermission(plugin),
                new ExperienceOrbPickupPermission(plugin),
                new NaturalMobSpawningPermission(),
                new PortalPermission(plugin),
                new ItemPickupPermission(),
                new ItemDropPermission(),
                new FishingPermission(plugin),
                new CauldronPermission(plugin),
                new ProtectorDamagePermission(plugin),
                new SignEditPermission(plugin),

                // Basic
                new MemberPermission(),
                new VisitorPermission(),
                new KickPermission(),
                new BiomePermission(),
                new UnbanPermission(),
                new BanPermission(),
                new BorderPermission(),
                new FireSpreadPermission(),
                new CoopPlayersPermission(),
                new IslandPermission(),
                new LeafDecayPermission(),
                new WeatherPermission(),
                new MainSpawnPermission(),
                new VisitorSpawnPermission());

        if (plugin.getConfiguration().getBoolean("Island.Settings.KeepItemsOnDeath.Enable")) {
            registerPermission(new KeepItemsOnDeathPermission());
        }

        if (plugin.getConfiguration().getBoolean("Island.Settings.PvP.Enable")) {
            registerPermission(new PvpPermission(plugin));
        }

        if (plugin.getConfiguration().getBoolean("Island.Settings.Damage.Enable")) {
            registerPermission(new DamagePermission(plugin));
        }

        if (plugin.getConfiguration().getBoolean("Island.Settings.Hunger.Enable")) {
            registerPermission(new HungerPermission(plugin));
        }

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_21)) {
            registerPermission(new CrafterPermission(plugin));
        }

        this.registeredHandlers = this.registeredHandlers.stream()
                .sorted(Comparator.comparingInt(h -> h.getHandler().getAnnotation(PermissionHandler.class).priority().ordinal()))
                .collect(Collectors.toList());
    }

    private synchronized void updateSettingsConfig(BasicPermission permission) {
        FileManager.Config settingsConfig = this.plugin.getFileManager().getConfig(new File(this.plugin.getDataFolder(), "settings.yml"));
        FileConfiguration settingsConfigLoad = settingsConfig.getFileConfiguration();

        switch (permission.getType()) {
            case GENERIC:
                if (settingsConfigLoad.getString("Settings.Visitor." + permission.getName()) == null) {
                    settingsConfigLoad.set("Settings.Visitor." + permission.getName(), permission.getDefaultValues().get(IslandRole.VISITOR));
                    if (permission.getName().equals("EditSign")) {
                        System.err.println("Default EditSign Visitor permission set to: " + permission.getDefaultValues().get(IslandRole.VISITOR));
                    }
                }
                if (settingsConfigLoad.getString("Settings.Member." + permission.getName()) == null) {
                    settingsConfigLoad.set("Settings.Member." + permission.getName(), permission.getDefaultValues().get(IslandRole.MEMBER));
                }
                if (settingsConfigLoad.getString("Settings.Coop." + permission.getName()) == null) {
                    settingsConfigLoad.set("Settings.Coop." + permission.getName(), permission.getDefaultValues().get(IslandRole.COOP));
                }
                break;
            case OPERATOR:
                if (settingsConfigLoad.getString("Settings.Operator." + permission.getName()) == null) {
                    settingsConfigLoad.set("Settings.Operator." + permission.getName(), true);
                }
                break;
            case ISLAND:
                if (settingsConfigLoad.getString("Settings.Owner." + permission.getName()) == null) {
                    settingsConfigLoad.set("Settings.Owner." + permission.getName(), true);
                }
                break;
        }
        try {
            settingsConfigLoad.save(settingsConfig.getFile());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean registerPermission(BasicPermission permission) {
        updateSettingsConfig(permission);

        this.registeredPermissions.put(permission.getName().toUpperCase(), permission);
        Set<Method> methods;
        try {
            Method[] publicMethods = permission.getClass().getMethods();
            methods = new HashSet<>(publicMethods.length, Float.MAX_VALUE);
            methods.addAll(Arrays.asList(publicMethods));
            Collections.addAll(methods, permission.getClass().getDeclaredMethods());
        } catch (NoClassDefFoundError e) {
            return false;
        }
        methods.stream().filter(method -> method.getAnnotation(PermissionHandler.class) != null).forEachOrdered(method -> this.registeredHandlers.add(new HandlerWrapper(permission, method)));
        return true;
    }

    public boolean registerPermissions(BasicPermission... permissions) {
        return Arrays.stream(permissions).allMatch(this::registerPermission);
    }

    public boolean processPermission(Cancellable cancellable, Island island) {
        return processPermission(cancellable, null, island);
    }

    public boolean processPermission(Cancellable cancellable, Player player, Location location) {
        return processPermission(cancellable, player, this.plugin.getIslandManager().getIslandAtLocation(location));
    }

    public boolean processPermission(Cancellable cancellable, Player player, Island island) {
        return processPermission(cancellable, player, island, false);
    }

    public boolean processPermission(Cancellable cancellable, Player player, Island island, boolean reversePermission) {
        if (island == null) {
            return true;
        }

        for (HandlerWrapper wrapper : this.registeredHandlers) {
            Method handler = wrapper.getHandler();
            if (handler.getParameterTypes()[0] != cancellable.getClass()) {
                continue;
            }

            if (cancellable.isCancelled()) {
                return false;
            }
            if (cancellable instanceof Stoppable && ((Stoppable) cancellable).isStopped()) {
                return true;
            }

            BasicPermission permission = wrapper.getPermission();

            if (hasPermission(player, island, permission, reversePermission)) {
                continue;
            }

            try {
                handler.invoke(permission, cancellable);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
        return !cancellable.isCancelled();
    }

    public boolean hasPermission(Player player, Island island, BasicPermission permission, boolean reversePermission) {
        if (player == null) {
            return island.hasPermission(IslandRole.OWNER, permission);
        }

        if (player.hasPermission("fabledskyblock.bypass." + permission.getName().toLowerCase())) {
            return !reversePermission;
        }

        FileConfiguration configLoad = SkyBlock.getPlugin(SkyBlock.class).getConfiguration();

        switch (island.getRole(player)) {
            case OWNER:
                if (!configLoad.getBoolean("Island.Settings.OwnersAndOperatorsAsMembers", false)) {
                    if (permission.getType() == PermissionType.ISLAND) {
                        return island.hasPermission(IslandRole.OWNER, permission);
                    } else {
                        return true;
                    }
                }
            case OPERATOR:
                if (!configLoad.getBoolean("Island.Settings.OwnersAndOperatorsAsMembers", false)) {
                    if (permission.getType() == PermissionType.OPERATOR) {
                        return island.hasPermission(IslandRole.OPERATOR, permission);
                    } else {
                        return true;
                    }
                }
            case MEMBER:
                return island.hasPermission(IslandRole.MEMBER, permission);
            case COOP:
                return island.hasPermission(IslandRole.COOP, permission);
            case VISITOR:
                return island.hasPermission(IslandRole.VISITOR, permission);
        }
        return false;
    }

    public boolean hasPermission(Player player, Island island, BasicPermission permission) {
        if (island == null) {
            return true; // Return true as there is no island, we don't have to modify the normal world behavior.
        }
        return this.hasPermission(player, island, permission, false);
    }

    public boolean hasPermission(Location location, String permission, IslandRole islandRole) {
        Island island = this.plugin.getIslandManager().getIslandAtLocation(location);
        if (island == null) {
            return true; // Return true as there is no island, we don't have to modify the normal world behavior.
        }
        return island.hasPermission(islandRole, getPermission(permission));
    }

    public boolean hasPermission(Island island, String permission, IslandRole islandRole) {
        if (island == null) {
            return true; // Return true as there is no island, we don't have to modify the normal world behavior.
        }
        return island.hasPermission(islandRole, getPermission(permission));
    }

    public boolean hasPermission(Player player, Island island, String permission) {
        return hasPermission(player, island, getPermission(permission));
    }

    public BasicPermission getPermission(String permissionName) {
        return this.registeredPermissions.get(permissionName.toUpperCase());
    }

    public List<BasicPermission> getPermissions() {
        return new ArrayList<>(this.registeredPermissions.values());
    }

    public List<ListeningPermission> getListeningPermissions() {
        return this.registeredPermissions.values().stream()
                .filter(p -> p instanceof ListeningPermission)
                .map(p -> (ListeningPermission) p)
                .collect(Collectors.toList());
    }
}
