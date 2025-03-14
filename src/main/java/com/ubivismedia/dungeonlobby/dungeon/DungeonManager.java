package com.ubivismedia.dungeonlobby.dungeon;

import com.ubivismedia.dungeonlobby.DungeonLobby;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class DungeonManager {
    private final DungeonLobby plugin;
    private final LanguageManager languageManager;
    private final HashMap<UUID, String> playerDungeons = new HashMap<>();
    private final HashSet<String> activeDungeons = new HashSet<>();

    public DungeonManager(DungeonLobby plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public String createDungeonInstance(Player player) {
        String dungeonId = "dungeon_" + player.getUniqueId();
        activeDungeons.add(dungeonId);
        playerDungeons.put(player.getUniqueId(), dungeonId);
        plugin.getLogger().info("Dungeon instance created: " + dungeonId);
        return dungeonId;
    }

    public void teleportPlayerToDungeon(Player player, String dungeonId) {
        World dungeonWorld = Bukkit.createWorld(new WorldCreator(dungeonId));
        if (dungeonWorld != null) {
            Location dungeonSpawn = new Location(dungeonWorld, 0, 65, 0);
            player.teleport(dungeonSpawn);
            languageManager.sendMessage(player, "dungeon.enter");
        }
    }

    public void onPlayerLeaveDungeon(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerDungeons.containsKey(playerId)) return;

        String dungeonId = playerDungeons.get(playerId);
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld == null) return;

        long playersInDungeon = dungeonWorld.getPlayers().stream().count();
        if (playersInDungeon <= 1) {
            removeDungeon(dungeonId);
        }
    }

    private void removeDungeon(String dungeonId) {
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld != null) {
            Bukkit.unloadWorld(dungeonWorld, false);
            activeDungeons.remove(dungeonId);
            plugin.getLogger().info("Dungeon instance removed: " + dungeonId);
        }
    }
}