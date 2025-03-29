package com.ubivismedia.aidungeon.dungeons;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Periodically checks exploration levels and triggers dungeon generation
 * when thresholds are met
 */
public class ExplorationChecker {

    private final AIDungeonGenerator plugin;
    private final DungeonManager dungeonManager;
    private int taskId = -1;

    // Track which biomes have been checked recently for each player to avoid spam
    private final Map<UUID, Map<String, Long>> recentChecks = new HashMap<>();
    private static final long CHECK_COOLDOWN = 60000; // 1 minute cooldown between checks for same biome/player

    public ExplorationChecker(AIDungeonGenerator plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }

    /**
     * Start the periodic exploration check task
     */
    public void startTask() {
        // Stop any existing task
        stopTask();

        // How often to check (in ticks) - default to 5 minutes (6000 ticks)
        int checkInterval = plugin.getConfig().getInt("discovery.periodic_check_interval", 6000);

        // Start a new task
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkAllPlayersExploration,
                200L, // Initial delay (10 seconds)
                checkInterval); // Repeat interval

        plugin.getLogger().info("Started periodic exploration check task (interval: " +
                checkInterval + " ticks)");
    }

    /**
     * Stop the periodic task
     */
    public void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Check exploration for all online players
     */
    private void checkAllPlayersExploration() {
        // Skip if no players are online
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        plugin.getLogger().info("Running periodic exploration check for all players");

        // Check each online player
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players without permission
            if (!player.hasPermission("aidungeon.discover")) {
                continue;
            }

            checkPlayerExploration(player);
        }
    }

    /**
     * Check exploration for a specific player
     */
    public void checkPlayerExploration(Player player) {
        UUID playerUuid = player.getUniqueId();
        World playerWorld = player.getWorld();

        // Get current biome
        Biome currentBiome = playerWorld.getBiome(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        );

        // Get the map of recently checked biomes for this player
        Map<String, Long> playerChecks = recentChecks.computeIfAbsent(playerUuid, k -> new HashMap<>());

        // Create key for this world+biome combination
        String biomeKey = playerWorld.getName() + ":" + currentBiome.name();

        // Skip if this biome was checked recently
        long currentTime = System.currentTimeMillis();
        if (playerChecks.containsKey(biomeKey)) {
            long lastCheck = playerChecks.get(biomeKey);
            if (currentTime - lastCheck < CHECK_COOLDOWN) {
                return;
            }
        }

        // Update check time
        playerChecks.put(biomeKey, currentTime);

        // Get exploration percentage
        double explorationPercentage = plugin.getBiomeExplorationTracker().recordExploredChunk(
                player,
                playerWorld,
                currentBiome
        );

        // Get threshold from config
        double threshold = plugin.getConfig().getDouble("discovery.exploration-threshold", 0.1);

        // Check if dungeon has been generated
        boolean hasDungeonBeenGenerated = plugin.getBiomeExplorationTracker().hasDungeonBeenGenerated(
                playerWorld,
                currentBiome
        );

        // Debug mode
        boolean debug = plugin.getConfig().getBoolean("settings.debug-mode", false);

        // Show debug info if enabled
        if (debug) {
            plugin.getLogger().info("Exploration Check: " + playerWorld.getName() +
                    " - " + currentBiome.name() +
                    " - " + player.getName() +
                    " - " + String.format("%.2f%%", explorationPercentage * 100) +
                    " - Generated: " + hasDungeonBeenGenerated);
        }

        // Check if we've explored enough and no dungeon exists
        if (explorationPercentage >= threshold && !hasDungeonBeenGenerated) {
            // Create a BiomeArea for this location
            BiomeArea area = new BiomeArea(
                    playerWorld.getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockZ(),
                    plugin.getConfig().getInt("discovery.discovery-radius", 100),
                    currentBiome
            );

            // Check if we can generate a dungeon here
            if (dungeonManager.canGenerateDungeon(player, area)) {
                // Queue dungeon generation
                dungeonManager.queueDungeonGeneration(area, player);

                // Mark this biome as having a dungeon
                plugin.getBiomeExplorationTracker().markDungeonGenerated(
                        playerWorld,
                        currentBiome
                );

                // Debug output
                if (debug) {
                    player.sendMessage("§7[Debug] Dungeon generation queued in " +
                            currentBiome.name() + " at " +
                            area.getCenterX() + "," + area.getCenterZ() +
                            " (Exploration: " + String.format("%.1f", explorationPercentage * 100) + "%)");
                }

                plugin.getLogger().info("Dungeon generation queued in " +
                        currentBiome.name() + " at " +
                        area.getCenterX() + "," + area.getCenterZ() +
                        " from periodic check");
            }
        }
    }

    /**
     * Clear tracking data for a player
     */
    public void clearPlayer(UUID playerUuid) {
        recentChecks.remove(playerUuid);
    }
}