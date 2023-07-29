package com.songoda.skyblock.placeholder;

import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.manager.Manager;
import com.songoda.skyblock.placeholder.hook.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

public class PlaceholderManager extends Manager {
    private boolean placeholderAPIEnabled = false;

    public PlaceholderManager(SkyBlock plugin) {
        super(plugin);

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIEnabled = true;
        }
    }

    public void registerPlaceholders() {
        if (this.placeholderAPIEnabled) {
            new PlaceholderAPI(this.plugin).register();
        }
    }

    public boolean isPlaceholderAPIEnabled() {
        return this.placeholderAPIEnabled;
    }

    public String parsePlaceholders(Player player, String message) {
        String retValue = message;

        if (this.placeholderAPIEnabled) {
            retValue = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
        }
        return retValue;
    }
}
