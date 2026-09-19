package com.songoda.skyblock.biome;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.blockscanner.ChunkLoader;
import com.songoda.skyblock.island.Island;
import com.songoda.skyblock.island.IslandEnvironment;
import com.songoda.skyblock.island.IslandWorld;
import com.songoda.third_party.com.cryptomorin.xseries.XBiome;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BiomeManager {
    private final SkyBlock plugin;
    private final List<Island> updatingIslands;
    private final FileConfiguration language;
    private final int runEveryX;
    private final int biomeUpdatesPerTick;

    public BiomeManager(SkyBlock plugin) {
        this.plugin = plugin;
        this.updatingIslands = new ArrayList<>();
        this.language = SkyBlock.getPlugin(SkyBlock.class).getLanguage();
        this.runEveryX = Math.max(1, this.language
                .getInt("Command.Island.Biome.Progress.Display-Every-X-Updates"));
        this.biomeUpdatesPerTick = Math.max(1, plugin.getConfiguration()
                .getInt("Island.Performance.ChunkPerTick", 25));
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
        List<CompletableFuture<Chunk>> chunksToUpdate = new ArrayList<>();

        ChunkLoader.startChunkLoadingPerChunk(island, world, this.plugin.isPaperAsync(), (cachedChunk) -> {
            CompletableFuture<Chunk> chunkFuture = cachedChunk.getChunk();
            if (chunkFuture != null) {
                // Convert failed/missing loads into a null result so one bad chunk
                // does not prevent the rest of the island from being updated.
                chunksToUpdate.add(chunkFuture.handle((chunk, throwable) -> {
                    if (throwable != null) {
                        this.plugin.getLogger().warning("Unable to load a chunk while updating an island biome: "
                                + throwable.getMessage());
                    }
                    return chunk;
                }));
            }
        }, (island1 -> {
            CompletableFuture<?>[] futures = chunksToUpdate.toArray(new CompletableFuture<?>[0]);
            CompletableFuture.allOf(futures).whenComplete((ignored, throwable) ->
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    new BukkitRunnable() {
                        private int nextChunk;
                        private int progress;

                        @Override
                        public void run() {
                            int updatesThisTick = 0;
                            while (nextChunk < chunksToUpdate.size() &&
                                    updatesThisTick++ < biomeUpdatesPerTick) {
                                Chunk chunk = chunksToUpdate.get(nextChunk++).getNow(null);
                                if (chunk == null) {
                                    continue;
                                }

                                try {
                                    // XBiome mutates Bukkit world state and must
                                    // run on the server thread. The chunk was
                                    // loaded by the future, and XBiome will safely
                                    // load it again if it was unloaded meanwhile.
                                    biome.setBiome(chunk);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                                progress++;
                                if (BiomeManager.this.language.getBoolean("Command.Island.Biome.Progress.Should-Display-Message") &&
                                        (progress == 1 || progress == chunkAmount || progress % BiomeManager.this.runEveryX == 0)) {
                                    final double percent = ((double) progress / (double) chunkAmount) * 100;

                                    String message = BiomeManager.this.language.getString("Command.Island.Biome.Progress.Message");
                                    message = message.replace("%current_updated_chunks%", String.valueOf(progress));
                                    message = message.replace("%max_chunks%", String.valueOf(chunkAmount));
                                    message = message.replace("%percent_whole%", String.valueOf((int) percent));
                                    message = message.replace("%percent%", NumberFormat.getInstance().format(percent));

                                    for (Player player : SkyBlock.getPlugin(SkyBlock.class).getIslandManager().getPlayersAtIsland(island)) {
                                        BiomeManager.this.plugin.getMessageManager().sendMessage(player, message);
                                    }
                                }
                            }

                            if (nextChunk >= chunksToUpdate.size()) {
                                cancel();
                                removeUpdatingIsland(island1);
                                if (task != null) {
                                    task.onCompleteUpdate();
                                }
                            }
                        }
                    }.runTaskTimer(this.plugin, 0L, 1L);
                })
            );
        }));
    }

    public interface CompleteTask {
        void onCompleteUpdate();
    }
}
