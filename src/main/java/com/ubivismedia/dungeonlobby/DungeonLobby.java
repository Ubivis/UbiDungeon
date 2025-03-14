package com.ubivismedia.dungeonlobby;

import org.bukkit.plugin.java.JavaPlugin;

public class DungeonLobby extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("DungeonLobby Plugin enabled!");
        // Register event listeners, commands, and managers here
    }

    @Override
    public void onDisable() {
        getLogger().info("DungeonLobby Plugin disabled!");
        // Cleanup resources, save data if necessary
    }
}