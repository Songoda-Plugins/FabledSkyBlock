package com.craftaro.skyblock.api.event.island;

import com.craftaro.skyblock.api.island.Island;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class IslandBiomeChangeEvent extends IslandEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private XBiome biome;

    public IslandBiomeChangeEvent(Island island, XBiome biome) {
        super(island);
        this.biome = biome;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public XBiome getBiome() {
        return this.biome;
    }

    public void setBiome(XBiome biome) {
        this.biome = biome;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
