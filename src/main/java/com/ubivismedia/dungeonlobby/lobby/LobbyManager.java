package com.ubivismedia.dungeonlobby.lobby;

import com.ubivismedia.dungeonlobby.DungeonLobby;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

public class LobbyManager {
    private static final String LOBBY_WORLD_NAME = "dungeon_lobby";
    private final DungeonLobby plugin;
    private final LanguageManager languageManager;

    public LobbyManager(DungeonLobby plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public void createLobbyWorld() {
        World world = Bukkit.getWorld(LOBBY_WORLD_NAME);
        if (world == null) {
            plugin.getLogger().info("Creating Lobby World...");
            WorldCreator creator = new WorldCreator(LOBBY_WORLD_NAME);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            world = creator.createWorld();
        }

        if (world != null) {
            world.setSpawnLocation(0, 65, 0);
            world.setStorm(false);
            world.setThundering(false);
            world.setTime(6000);
            world.setGameRuleValue("doMobSpawning", "false");
            world.setGameRuleValue("doWeatherCycle", "false");
            plugin.getLogger().info("Lobby World ready!");
        }
    }

    public void teleportToLobby(Player player) {
        World lobbyWorld = Bukkit.getWorld(LOBBY_WORLD_NAME);
        if (lobbyWorld != null) {
            Location spawnLocation = new Location(lobbyWorld, 0.5, 65, 0.5);
            player.teleport(spawnLocation);
            languageManager.sendMessage(player, "lobby.welcome");
        }
    }
}
