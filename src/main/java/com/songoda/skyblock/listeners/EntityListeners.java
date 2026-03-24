package com.songoda.skyblock.listeners;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.MajorServerVersion;
import com.songoda.third_party.com.cryptomorin.xseries.XBlock;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandEnvironment;
import com.songoda.skyblock.island.IslandLevel;
import com.songoda.skyblock.island.IslandManager;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.skyblock.limit.impl.EntityLimitation;
import com.songoda.skyblock.stackable.Stackable;
import com.songoda.skyblock.stackable.StackableManager;
import com.songoda.skyblock.upgrade.Upgrade;
import com.songoda.skyblock.utils.world.LocationUtil;
import com.songoda.skyblock.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Illager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.entity.Steerable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class EntityListeners implements Listener {
    private final SkyBlock plugin;

    private final Set<UUID> preventFireTicks = new HashSet<>();

    public EntityListeners(SkyBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireWorkBoom(EntityDamageByEntityEvent event) {
        if (event.getDamager().getType().toString().contains("FIREWORK")
                && this.plugin.getWorldManager().isIslandWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        IslandManager islandManager = this.plugin.getIslandManager();
        if (event.getEntity() instanceof Blaze) {
            WorldManager worldManager = this.plugin.getWorldManager();

            FileConfiguration configLoad = this.plugin.getConfiguration();

            if (configLoad.getBoolean("Island.Nether.BlazeImmuneToWaterInNether", false) &&
                    worldManager.getIslandWorld(event.getEntity().getWorld()) == IslandWorld.NETHER &&
                    event.getCause() == DamageCause.DROWNING) {
                event.setCancelled(true);
            }
        } else if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (this.plugin.getWorldManager().isIslandWorld(player.getWorld())) {
                // Check permissions.
                this.plugin.getPermissionManager().processPermission(event, player, islandManager.getIslandAtLocation(player.getLocation()));
            }

            // Fix a bug in minecraft where arrows with flame still apply fire ticks even if
            // the shot entity isn't damaged
            if (this.preventFireTicks.contains(player.getUniqueId()) && event.getCause() == DamageCause.FIRE_TICK) {
                player.setFireTicks(0);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        IslandManager islandManager = this.plugin.getIslandManager();
        FileConfiguration configLoad = this.plugin.getConfiguration();

        org.bukkit.entity.Entity victim = event.getEntity();
        Island island = islandManager.getIslandAtLocation(victim.getLocation());

        if (island != null) {
            org.bukkit.entity.Entity attacker = event.getDamager();
            if (attacker instanceof Projectile && ((Projectile) attacker).getShooter() instanceof org.bukkit.entity.Entity) {
                attacker = (org.bukkit.entity.Entity) ((Projectile) attacker).getShooter();
            }

            // Rework with better config
            if (victim instanceof Player && attacker instanceof Player) { // PVP
                if (configLoad.getBoolean("Island.Entity_Damage.PVP")) {
                    if (this.plugin.getPermissionManager()
                            .processPermission(event, (Player) attacker, island)) {
                        this.plugin.getPermissionManager()
                                .processPermission(event, (Player) victim, island);
                    }
                } else {
                    event.setCancelled(true);
                }
            } else if (victim instanceof Player) { // EVP
                if (configLoad.getBoolean("Island.Entity_Damage.EVP")) {
                    this.plugin.getPermissionManager()
                            .processPermission(event, (Player) victim, island, true);
                }

            } else if (attacker instanceof Player) { // PVE
                if (configLoad.getBoolean("Island.Entity_Damage.PVE")) {
                    this.plugin.getPermissionManager()
                            .processPermission(event, (Player) attacker, island);
                }

            } else { // EVE
                if (configLoad.getBoolean("Island.Entity_Damage.PVE")) {
                    this.plugin.getPermissionManager()
                            .processPermission(event, island);
                }
            }

            // Fix a bug in minecraft where arrows with flame still apply fire ticks even if
            // the shot entity isn't damaged
            if (event.isCancelled() && event.getDamager() instanceof Arrow && event.getDamager().getFireTicks() != 0) {
                this.preventFireTicks.add(event.getEntity().getUniqueId());
                Bukkit.getScheduler().runTaskLater(this.plugin,
                        () -> this.preventFireTicks.remove(victim.getUniqueId()),
                        5L);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getWorldManager().isIslandWorld(player.getWorld())) {
            IslandManager islandManager = this.plugin.getIslandManager();
            Island island = islandManager.getIslandAtLocation(event.getEntity().getLocation());

            // Check permissions.
            this.plugin.getPermissionManager().processPermission(event, player, island);
        }
    }

    /**
     * Checks that an entity is not targeting another entity on different islands.x
     *
     * @author LimeGlass
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        org.bukkit.entity.Entity entity = event.getEntity();
        WorldManager worldManager = this.plugin.getWorldManager();
        if (!worldManager.isIslandWorld(entity.getWorld())) {
            return;
        }

        org.bukkit.entity.Entity target = event.getTarget();
        // Somehow the target can be null, thanks Spigot.
        if (target == null) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();
        Island entityIsland = islandManager.getIslandAtLocation(entity.getLocation());
        Island targetIsland = islandManager.getIslandAtLocation(target.getLocation());
        // Event not related to Skyblock islands.
        if (entityIsland == null && targetIsland == null) {
            return;
        }
        // One entity is on an island, and the other isn't.
        if (entityIsland == null || targetIsland == null) {
            event.setCancelled(true);
            return;
        }
        // Both entities are on different islands.
        if (!entityIsland.getIslandUUID().equals(targetIsland.getIslandUUID())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStackableInteract(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getWorldManager().isIslandWorld(player.getWorld())) {
            return;
        }
        IslandManager islandManager = this.plugin.getIslandManager();

        // Check permissions.
        if (!this.plugin.getPermissionManager().processPermission(event, player,
                islandManager.getIslandAtLocation(event.getRightClicked().getLocation()))) {
            return;
        }

        if (MajorServerVersion.isServerVersion(MajorServerVersion.V1_8)) {
            return;
        }

        StackableManager stackableManager = SkyBlock.getPlugin(SkyBlock.class).getStackableManager();
        if (stackableManager == null) {
            return;
        }

        ArmorStand armorStand = event.getRightClicked();
        for (Location stackLocation : stackableManager.getStacks().keySet()) {
            if (stackLocation.getWorld().equals(armorStand.getWorld()) && armorStand.getLocation().distanceSquared(stackLocation) <= 1.5) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            if (!this.plugin.getWorldManager().isIslandWorld(player.getWorld())) {
                return;
            }
            IslandManager islandManager = this.plugin.getIslandManager();

            // Check permissions.
            this.plugin.getPermissionManager().processPermission(event, player,
                    islandManager.getIslandAtLocation(event.getEntity().getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Hanging hanging = event.getEntity();

        if (!this.plugin.getWorldManager().isIslandWorld(hanging.getWorld())) {
            return;
        }
        IslandManager islandManager = this.plugin.getIslandManager();

        // Check permissions.
        this.plugin.getPermissionManager().processPermission(event, null,
                islandManager.getIslandAtLocation(hanging.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Hanging hanging = event.getEntity();

        if (!this.plugin.getWorldManager().isIslandWorld(hanging.getWorld())) {
            return;
        }
        IslandManager islandManager = this.plugin.getIslandManager();

        Player p = null;
        if (event.getRemover() instanceof Player) {
            p = (Player) event.getRemover();
        }
        // Check permissions.
        this.plugin.getPermissionManager().processPermission(event, p,
                islandManager.getIslandAtLocation(hanging.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTaming(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) {
            return;
        }

        LivingEntity entity = event.getEntity();

        if (!this.plugin.getWorldManager().isIslandWorld(entity.getWorld())) {
            return;
        }
        IslandManager islandManager = this.plugin.getIslandManager();

        // Check permissions.
        this.plugin.getPermissionManager().processPermission(event, (Player) event.getOwner(),
                islandManager.getIslandAtLocation(entity.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        org.bukkit.entity.Entity entity = event.getEntity();

        if (entity instanceof Player) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();
        WorldManager worldManager = this.plugin.getWorldManager();

        Island island = islandManager.getIslandAtLocation(event.getBlock().getLocation());

        if (island == null || !this.plugin.getWorldManager().isIslandWorld(entity.getWorld())) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        FileConfiguration configLoad = this.plugin.getConfiguration();

        IslandWorld world = worldManager.getIslandWorld(event.getBlock().getWorld());

        org.bukkit.block.Block block = event.getBlock();

        // Check spawn block falling, this can be a bit glitchy, but it's better than
        // nothing
        if ((LocationUtil.isLocationLocation(block.getLocation(), island.getLocation(world, IslandEnvironment.MAIN).clone().subtract(0, 1, 0))
                || LocationUtil.isLocationLocation(block.getLocation(),
                island.getLocation(world, IslandEnvironment.VISITOR).clone().subtract(0, 1, 0)))
                && this.plugin.getConfiguration().getBoolean("Island.Spawn.Protection")) {
            Optional<XMaterial> material = CompatibleMaterial.getMaterial(block.getType());
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                event.getEntity().remove();
                XBlock.setType(event.getBlock(), material.get());
            }, 1L);
            return;
        }

        if ((event.getEntityType() == EntityType.FALLING_BLOCK)
                && LocationUtil.isLocationAffectingIslandSpawn(block.getLocation(), island, world)
                && configLoad.getBoolean("Island.Spawn.Protection")) {
            FallingBlock fallingBlock = (FallingBlock) event.getEntity();
            if (fallingBlock.getDropItem()) {
                if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13)) {
                    fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(),
                            new ItemStack(fallingBlock.getBlockData().getMaterial(), 1));
                } else {
                    try {
                        Method getBlockDataMethod = FallingBlock.class.getMethod("getBlockData");
                        byte data = (byte) getBlockDataMethod.invoke(fallingBlock);
                        if (fallingBlock.getMaterial().name().endsWith("ANVIL")) {
                            data = (byte) Math.ceil(data / 4.0);
                        }
                        fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(), new ItemStack(fallingBlock.getMaterial(), 1, data));
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            event.setCancelled(true);
        }

        if (entity instanceof FallingBlock) {
            return;
        }

        // Check entities interacting with spawn
        if (LocationUtil.isLocationAffectingIslandSpawn(block.getLocation(), island, world) && this.plugin.getConfiguration().getBoolean("Island.Spawn.Protection")) {
            event.setCancelled(true);
            return;
        }

        // Check permissions.
        this.plugin.getPermissionManager().processPermission(event, null, island);


        if (!this.plugin.getConfiguration()
                .getBoolean("Island.Block.Level.Enable")) {
            return;
        }

        removeBlockFromLevel(island, block);
        Optional<XMaterial> materials;

        if (event.getTo() != Material.AIR) {
            materials = CompatibleMaterial.getMaterial(event.getTo());

            if (materials.isPresent()) {
                long materialAmount = 0;
                IslandLevel level = island.getLevel();

                if (level.hasMaterial(materials.get().name())) {
                    materialAmount = level.getMaterialAmount(materials.get().name());
                }

                level.setMaterialAmount(materials.get().name(), materialAmount + 1);
            }
        }

    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent event) {
        org.bukkit.entity.Entity entity = event.getEntity();

        WorldManager worldManager = this.plugin.getWorldManager();
        IslandManager islandManager = this.plugin.getIslandManager();

        if (this.plugin.getWorldManager().isIslandWorld(entity.getWorld())) {
            // Check permissions.
            Island island = islandManager.getIslandAtLocation(entity.getLocation());
            this.plugin.getPermissionManager().processPermission(event, null, island);

            if (!event.isCancelled() && island != null) {

                StackableManager stackableManager = this.plugin.getStackableManager();

                boolean removed;
                Iterator<org.bukkit.block.Block> it = event.blockList().iterator();
                while (it.hasNext()) {
                    removed = false;
                    org.bukkit.block.Block block = it.next();
                    if (SkyBlock.getPlugin(SkyBlock.class).getConfiguration().getBoolean("Island.Spawn.Protection")) {
                        IslandWorld world = worldManager.getIslandWorld(event.getEntity().getWorld());
                        if (LocationUtil.isLocationLocation(block.getLocation(),
                                island.getLocation(world, IslandEnvironment.MAIN).clone().subtract(0.0D, 1.0D, 0.0D))) {
                            it.remove();
                            removed = true;
                        }
                    }

                    Location blockLocation = block.getLocation();

                    if (stackableManager != null && stackableManager.isStacked(blockLocation)) {
                        Stackable stackable = stackableManager.getStack(block.getLocation(), CompatibleMaterial.getMaterial(block.getType()).get());
                        if (stackable != null) {
                            Optional<XMaterial> material = CompatibleMaterial.getMaterial(block.getType());
                            byte data = block.getData();

                            int removedAmount = (int) (Math.random() * Math.min(64, stackable.getSize() - 1));
                            stackable.take(removedAmount);
                            Bukkit.getScheduler().runTask(this.plugin, () -> {
                                block.getWorld().dropItemNaturally(blockLocation.clone().add(.5, 1, .5),
                                        new ItemStack(material.get().parseMaterial(), (int) (Math.random() * removedAmount), data));
                            });

                            if (stackable.getSize() <= 1) {
                                stackableManager.removeStack(stackable);
                            }

                            FileConfiguration configLoad = this.plugin.getConfiguration();

                            if (configLoad.getBoolean("Island.Block.Level.Enable")) {
                                removeBlockFromLevel(island, block);
                            }

                            it.remove();
                            if (!removed) {
                                removed = true;
                            }
                        }
                    }
                    if (this.plugin.getConfiguration()
                            .getBoolean("Island.Block.Level.Enable")) {
                        if (!removed) {
                            removeBlockFromLevel(island, block);
                        }

                    }
                }
            }
        }
    }

    private void removeBlockFromLevel(Island island, XMaterial material) {
        if (material == null) {
            return;
        }

        IslandLevel level = island.getLevel();
        if (level.hasMaterial(material.name())) {
            long materialAmount = level.getMaterialAmount(material.name());

            if (materialAmount - 1 <= 0) {
                level.removeMaterial(material.name());
            } else {
                level.setMaterialAmount(material.name(), materialAmount - 1);
            }
        }
    }

    private void removeBlockFromLevel(Island island, Block block) {
        removeBlockFromLevel(island, CompatibleMaterial.getMaterial(block.getType()).get());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();

        // Certain entities shouldn't drop twice the amount
        if (livingEntity instanceof Player || livingEntity instanceof ArmorStand || livingEntity instanceof Horse) {
            return;
        }

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_10)) {
            if (livingEntity instanceof Donkey || livingEntity instanceof Mule || livingEntity instanceof ElderGuardian) {
                return;
            }
        }

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_11)) {
            if (livingEntity instanceof Evoker) {
                return;
            }
        }

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_12)) {
            if (livingEntity instanceof Llama) {
                return;
            }
        }

        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_14)) {
            if (livingEntity instanceof Ravager || livingEntity instanceof Illager) {
                return;
            }
        }

        if (livingEntity.hasMetadata("SkyBlock")) {
            return;
        }

        IslandManager islandManager = this.plugin.getIslandManager();

        if (!this.plugin.getWorldManager().isIslandWorld(livingEntity.getWorld())) {
            return;
        }

        Island island = islandManager.getIslandAtLocation(livingEntity.getLocation());

        if (island == null) {
            return;
        }

        List<Upgrade> upgrades = this.plugin.getUpgradeManager().getUpgrades(Upgrade.Type.DROPS);

        if (upgrades != null && !upgrades.isEmpty() && upgrades.get(0).isEnabled() && island.isUpgrade(Upgrade.Type.DROPS)) {
            Set<ItemStack> dontMultiply = new HashSet<>();

            if (MajorServerVersion.isServerVersionAbove(MajorServerVersion.V1_8)) {
                EntityEquipment equipment = livingEntity.getEquipment();
                if (equipment != null) {
                    for (ItemStack item : event.getDrops()) {
                        if (item.isSimilar(equipment.getHelmet()) || item.isSimilar(equipment.getChestplate())
                                || item.isSimilar(equipment.getLeggings()) || item.isSimilar(equipment.getBoots())
                                || item.isSimilar(equipment.getItemInMainHand()) || item.isSimilar(equipment.getItemInOffHand())) {
                            dontMultiply.add(item);
                        }
                    }
                }

                if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_16)) {
                    if (livingEntity instanceof Steerable) {
                        Steerable steerable = (Steerable) livingEntity;
                        if (steerable.hasSaddle()) {
                            dontMultiply.add(new ItemStack(XMaterial.SADDLE.parseMaterial(), 1));
                        }
                    }
                } else {
                    if (livingEntity instanceof Pig) {
                        Pig pig = (Pig) livingEntity;
                        if (pig.hasSaddle()) {
                            dontMultiply.add(new ItemStack(XMaterial.SADDLE.parseMaterial(), 1));
                        }
                    }
                }
            }

            // Only drop items that are not in the dontMultiply set
            for (ItemStack item : event.getDrops()) {
                boolean shouldDrop = true;
                for (ItemStack dontMultiplyItem : dontMultiply) {
                    if (item.isSimilar(dontMultiplyItem)) {
                        shouldDrop = false;
                        break;
                    }
                }
                if (shouldDrop) {
                    livingEntity.getWorld().dropItemNaturally(livingEntity.getLocation(), item);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getTarget();
        if (!this.plugin.getWorldManager().isIslandWorld(player.getWorld())) {
            return;
        }
        IslandManager islandManager = this.plugin.getIslandManager();

        // Check permissions.
        this.plugin.getPermissionManager().processPermission(event, player, islandManager.getIslandAtLocation(event.getEntity().getLocation()));
    }

    private static final Set<SpawnReason> CHECKED_REASONS;

    static {
        CHECKED_REASONS = EnumSet.of(SpawnReason.NATURAL, SpawnReason.JOCKEY, SpawnReason.MOUNT);

        final SpawnReason raid = getSpawnReason("RAID");
        final SpawnReason patrol = getSpawnReason("PATROL");

        if (patrol != null) {
            CHECKED_REASONS.add(patrol);
        }
        if (raid != null) {
            CHECKED_REASONS.add(raid);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ArmorStand) {
            return;
        }
        // if (entity.hasMetadata("SkyBlock")) return;
        // Doesn't appear this is ever set by our plugin and it is extremely intensive.

        Location entityLocation = entity.getLocation();

        Island island = this.plugin.getIslandManager().getIslandAtLocation(entityLocation);

        if (island == null) {
            return;
        }

        EntityLimitation limits = this.plugin.getLimitationHandler().getInstance(EntityLimitation.class);
        EntityType type = entity.getType();

        if (limits.isBeingTracked(type)) {
            FileConfiguration configLoad = this.plugin.getConfiguration();

            boolean isSplit = event.getSpawnReason() == SpawnReason.SLIME_SPLIT;
            boolean splitBypass = configLoad.getBoolean("Island.Challenge.PerIsland", true);

            if (!isSplit || !splitBypass) {
                long count = limits.getEntityCount(island, this.plugin.getWorldManager().getIslandWorld(entityLocation.getWorld()), type);
                if (limits.hasTooMuch(count + 1, type)) {
                    entity.remove();
                    event.setCancelled(true);
                    return;
                }
            }
        }

        SpawnReason spawnReason = event.getSpawnReason();

        if (!CHECKED_REASONS.contains(spawnReason)) {
            return;
        }

        if (!this.plugin.getWorldManager().isIslandWorld(entity.getWorld())) {
            return;
        }
        if (this.plugin.getPermissionManager().hasPermission(null, island, "NaturalMobSpawning")) {
            return;
        }
        if (spawnReason != SpawnReason.JOCKEY && spawnReason != SpawnReason.MOUNT) {
            entity.remove(); // Older versions ignore the event being cancelled, so this fixes that issue.
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
            if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_11)) { // getPassengers() was added in 1.11
                for (org.bukkit.entity.Entity passenger : entity.getPassengers()) {
                    passenger.remove();
                }
            } else { // TODO Reflection
                if (entity.getPassenger() != null) {
                    entity.getPassenger().remove();
                }
            }
            entity.remove();
        });
        event.setCancelled(true); // For other plugin API reasons.
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        WorldManager worldManager = this.plugin.getWorldManager();
        if (worldManager.isIslandWorld(event.getLocation().getWorld())) {
            org.bukkit.entity.Entity entity = event.getEntity();

            if (event.getEntity() instanceof EnderSignal) {
                ((EnderSignal) entity).setTargetLocation(entity.getLocation().add(0d, 50d, 0d));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageVehicle(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            IslandManager islandManager = this.plugin.getIslandManager();
            this.plugin.getPermissionManager().processPermission(event, null, islandManager.getIslandAtLocation(event.getVehicle().getLocation()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDestroyVehicle(VehicleDestroyEvent event) {
        if (!(event.getAttacker() instanceof Player)) {
            IslandManager islandManager = this.plugin.getIslandManager();
            this.plugin.getPermissionManager().processPermission(event, null, islandManager.getIslandAtLocation(event.getVehicle().getLocation()));
        }
    }

    private static SpawnReason getSpawnReason(String reason) {
        try {
            return SpawnReason.valueOf(reason);
        } catch (Exception e) {
            return null;
        }
    }
}
