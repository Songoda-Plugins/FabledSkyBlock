package com.craftaro.skyblock.world.generator;

import com.craftaro.core.compatibility.MajorServerVersion;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.island.IslandWorld;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class VoidGenerator extends ChunkGenerator {
    private final IslandWorld islandWorld;
    private final SkyBlock plugin;

    public VoidGenerator(IslandWorld islandWorld, SkyBlock plugin) {
        this.islandWorld = islandWorld;
        this.plugin = plugin;
    }

    @Override
    @Nonnull
    public ChunkData generateChunkData(@Nonnull World world, @Nonnull Random random, int chunkX, int chunkZ, @Nonnull BiomeGrid biomeGrid) {
        final ChunkData chunkData = createChunkData(world);

        final Configuration configLoad = this.plugin.getConfiguration();
        final ConfigurationSection worldSection = configLoad.getConfigurationSection("Island.World");

        XBiome biome;

        switch (world.getEnvironment()) {
            case NORMAL:
                biome = Arrays.stream(XBiome.values())
                        .filter(xBiome -> xBiome.name().equals(configLoad.getString("Island.Biome.Default.Type", "PLAINS").toUpperCase()) && xBiome.isSupported())
                        .findFirst()
                        .orElse(XBiome.PLAINS);
                break;
            case NETHER:
                biome = XBiome.NETHER_WASTES;
                break;
            case THE_END:
                biome = XBiome.THE_END;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + world.getEnvironment());
        }

        setChunkBiome(biome, world, chunkX, chunkZ);

        ConfigurationSection section = worldSection.getConfigurationSection(this.islandWorld.getFriendlyName());

        if (section.getBoolean("Liquid.Enable")) {
            if (section.getBoolean("Liquid.Lava")) {
                setBlock(chunkData, XMaterial.LAVA.parseMaterial(), section.getInt("Liquid.Height"));
            } else {
                setBlock(chunkData, XMaterial.WATER.parseMaterial(), section.getInt("Liquid.Height"));
            }
        }


        return chunkData;
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(final @NotNull World world) {
        return Collections.emptyList();
    }

    @Override
    public boolean canSpawn(@NotNull World world, int x, int z) {
        return true;
    }

    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        return new byte[world.getMaxHeight() / 16][];
    }

    private void setBlock(ChunkData chunkData, Material material, int height) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < height; ++y) {
                    chunkData.setBlock(x, y, z, material);
                }
            }
        }
    }

    private void setChunkBiome(XBiome biome, World world, int chunkX, int chunkZ) {
        biome.setBiome(world.getChunkAt(chunkX, chunkZ));
    }
}
