package com.craftaro.skyblock.biome;

import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.blockscanner.ChunkLoader;
import com.craftaro.skyblock.island.Island;
import com.craftaro.skyblock.island.IslandEnvironment;
import com.craftaro.skyblock.island.IslandWorld;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BiomeManager {
    private final SkyBlock plugin;
    private final List<Island> updatingIslands;
    private final FileConfiguration language;
    private final int runEveryX;

    public BiomeManager(SkyBlock plugin) {
        this.plugin = plugin;
        this.updatingIslands = new ArrayList<>();
        this.language = SkyBlock.getPlugin(SkyBlock.class).getLanguage();
        this.runEveryX = this.language.getInt("Command.Island.Biome.Progress.Display-Every-X-Updates");
    }

    public boolean isUpdating(Island island) {
        return this.updatingIslands.contains(island);
    }

    public void addUpdatingIsland(Island island) {
        this.updatingIslands.add(island);
    }

    public void removeUpdatingIsland(Island island) {
        this.updatingIslands.remove(island);
    }

    public void setBiome(Island island, IslandWorld world, XBiome biome, CompleteTask task) {
        addUpdatingIsland(island);

        if (island.getLocation(world, IslandEnvironment.ISLAND) == null) {
            return;
        }

        // We keep it sequentially in order to use less RAM
        int chunkAmount = (int) Math.ceil(Math.pow(island.getSize() / 16d, 2d));
        AtomicInteger progress = new AtomicInteger();

        ChunkLoader.startChunkLoadingPerChunk(island, world, this.plugin.isPaperAsync(), (cachedChunk) -> {
            // I don't like this. But CompletableFuture#join causes a crash on some setups.
            cachedChunk.getChunk().thenAccept(chunk -> {
                try {
                    if (chunk != null) {
                        biome.setBiome(chunk);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                progress.getAndIncrement();

                if (this.language.getBoolean("Command.Island.Biome.Progress.Should-Display-Message") &&
                        progress.get() == 1 || progress.get() == chunkAmount || progress.get() % this.runEveryX == 0) {
                    final double percent = ((double) progress.get() / (double) chunkAmount) * 100;

                    String message = this.language.getString("Command.Island.Biome.Progress.Message");
                    message = message.replace("%current_updated_chunks%", String.valueOf(progress.get()));
                    message = message.replace("%max_chunks%", String.valueOf(chunkAmount));
                    message = message.replace("%percent_whole%", String.valueOf((int) percent));
                    message = message.replace("%percent%", NumberFormat.getInstance().format(percent));

                    for (Player player : SkyBlock.getPlugin(SkyBlock.class).getIslandManager().getPlayersAtIsland(island)) {
                        this.plugin.getMessageManager().sendMessage(player, message);
                    }
                }
            });
        }, (island1 -> {
            removeUpdatingIsland(island1);
            if (task != null) {
                task.onCompleteUpdate();
            }
        }));
    }

    public interface CompleteTask {
        void onCompleteUpdate();
    }
}
