package com.songoda.skyblock.stackable;

import com.songoda.core.compatibility.MajorServerVersion;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.core.utils.NumberUtils;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.config.FileManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.UUID;

public class Stackable {
    private final UUID uuid;

    private Location location;
    private XMaterial material;
    private int size = 1;
    private ArmorStand display;
    private int maxSize;

    public Stackable(Location location, XMaterial material) {
        this.uuid = UUID.randomUUID();
        this.location = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        this.material = material;
        this.updateDisplay();
        SkyBlock.getPlugin(SkyBlock.class).getSoundManager().playSound(location, XSound.BLOCK_ANVIL_LAND);
        this.save();
    }

    public Stackable(Location location, XMaterial material, int maxSize) {
        this.uuid = UUID.randomUUID();
        this.location = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        this.material = material;
        this.maxSize = maxSize;
        this.updateDisplay();
        SkyBlock.getPlugin(SkyBlock.class).getSoundManager().playSound(location, XSound.BLOCK_ANVIL_LAND);
        this.save();
    }

    public Stackable(UUID uuid, Location location, XMaterial material, int size) {
        this.uuid = uuid;
        this.location = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        this.material = material;
        this.size = size;
        this.updateDisplay();
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Location getLocation() {
        return this.location.clone();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public XMaterial getMaterial() {
        return this.material;
    }

    public void setMaterial(XMaterial material) {
        this.material = material;
        this.save();
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
        this.updateDisplay();
        this.save();
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addOne() {
        this.size++;
        this.updateDisplay();
        SkyBlock.getPlugin(SkyBlock.class).getSoundManager().playSound(this.location, XSound.ENTITY_PLAYER_LEVELUP, 1, 1);
        this.save();
    }

    public void takeOne() {
        this.size--;
        this.updateDisplay();
        SkyBlock.getPlugin(SkyBlock.class).getSoundManager().playSound(this.location, XSound.ENTITY_ARROW_HIT, 1, 1);
        this.save();
    }

    public void take(int n) {
        this.size -= n;
        this.updateDisplay();
        SkyBlock.getPlugin(SkyBlock.class).getSoundManager().playSound(this.location, XSound.ENTITY_ARROW_HIT, 1, 1);
        this.save();
    }

    public boolean isMaxSize() {
        return this.size > this.maxSize;
    }

    private void updateDisplay() {
        // The chunk needs to be loaded otherwise the getNearbyEntities() in
        // removeDisplay() won't find anything
        if (!this.location.getWorld().isChunkLoaded(this.location.getBlockX() >> 4, this.location.getBlockZ() >> 4)) {
            this.location.getChunk().load();
        }

        if (this.size > 1) {

            if (this.display == null || !this.display.isValid()) {
                this.createDisplay();
            } else {
                this.display.setCustomName(this.getCustomName());
                this.display.setCustomNameVisible(true);
            }

        } else {
            this.removeDisplay();
        }
    }

    private void createDisplay() {
        this.removeDisplay();

        Location dropLocation = this.location.clone().add(0.5, 1, 0.5);
        ArmorStand as = (ArmorStand) this.location.getWorld().spawnEntity(dropLocation, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setSmall(true);
        if (MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_9)) {
            as.setMarker(true);
        }
        as.setBasePlate(true);
        as.setHelmet(this.material.parseItem());
        as.setCustomName(this.getCustomName());
        as.setCustomNameVisible(true);
        as.setMetadata("StackableArmorStand", new FixedMetadataValue(SkyBlock.getPlugin(SkyBlock.class), ""));
        this.display = as;
    }

    private void removeDisplay() {
        if (this.display != null) {
            this.display.remove();
        }

        // Find any stragglers
        for (Entity entity : this.location.getWorld().getNearbyEntities(this.location.clone().add(0.5, 0.55, 0.5), 0.25, 0.5, 0.25)) {
            if (entity instanceof ArmorStand) {
                entity.remove();
            }
        }
    }

    private void save() {
        File configFile = new File(SkyBlock.getPlugin(SkyBlock.class).getDataFolder() + "/island-data");
        FileManager.Config config = SkyBlock.getPlugin(SkyBlock.class).getFileManager().getConfig(new File(configFile, SkyBlock.getPlugin(SkyBlock.class).getIslandManager().getIslandAtLocation(this.location).getOwnerUUID() + ".yml"));
        FileConfiguration configLoad = config.getFileConfiguration();

        if (this.getSize() == 0) {
            configLoad.set("Stackables." + this.getUuid().toString(), null);
        } else {
            ConfigurationSection section = configLoad.createSection("Stackables." + this.getUuid().toString());
            section.set("Location", this.getLocation());
            section.set("Size", this.getSize());
        }
    }

    private String getCustomName() {
        return ChatColor
                .translateAlternateColorCodes('&', SkyBlock.getPlugin(SkyBlock.class).getLanguage().getString("Hologram.Stackable.Message"))
                .replace("%block", SkyBlock.getPlugin(SkyBlock.class).getLocalizationManager().getLocalizationFor(XMaterial.class).getLocale(this.material)).replace("%amount", NumberUtils.formatNumber(this.size));
    }
}
