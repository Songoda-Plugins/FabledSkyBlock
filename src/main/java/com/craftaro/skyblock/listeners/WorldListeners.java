package com.craftaro.skyblock.listeners;

import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.biome.BiomeManager;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.island.IslandManager;
import com.craftaro.skyblock.island.IslandWorld;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.lang.reflect.Method;

public class WorldListeners implements Listener {
    private final SkyBlock plugin;

    public WorldListeners(SkyBlock plugin) {
        this.plugin = plugin;
    }

    // Hotfix for wrong biome in other worlds;
    @EventHandler(ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        IslandManager islandManager = this.plugin.getIslandManager();
        BiomeManager biomeManager = this.plugin.getBiomeManager();

        Location to = event.getPlayer().getLocation();
        Island island = islandManager.getIslandAtLocation(to);

        if (island != null) {
            switch (to.getWorld().getEnvironment()) {
                case NORMAL:
                    break;
                case NETHER:
                    if (isBiome(to.getBlock(), XBiome.NETHER_WASTES)) {
                        biomeManager.setBiome(island, IslandWorld.NETHER, XBiome.NETHER_WASTES, null);
                    }
                    break;
                case THE_END:
                    if (isBiome(to.getBlock(), XBiome.THE_END)) {
                        biomeManager.setBiome(island, IslandWorld.END, XBiome.THE_END, null);
                    }
                    break;
            }
        }
    }

    private boolean isBiome(Block block, XBiome biome) {
        Object biomeObject = block.getBiome(); //Can be a paper registry type
        Method nameMethod = null;
        try {
            nameMethod = biomeObject.getClass().getMethod("name");
        } catch (NoSuchMethodException ignored) {
        }
        if (nameMethod != null) {
            try {
                return biome.name().equals((String) nameMethod.invoke(biomeObject));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
}
