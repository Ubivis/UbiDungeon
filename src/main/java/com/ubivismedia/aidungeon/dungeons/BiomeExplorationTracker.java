package com.ubivismedia.aidungeon.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how much of each biome players have explored
 */
public class BiomeExplorationTracker {

    // Store explored chunks per biome and world for each player
    private final Map<UUID, Map<String, Map<Biome, Integer>>> playerBiomeExploration = new ConcurrentHashMap<>();

    // Store total explored chunks for each biome across all players
    private final Map<String, Map<Biome, Integer>> totalExploredChunks = new ConcurrentHashMap<>();

    // Store estimated total chunks per biome type in each world
    private final Map<String, Map<Biome, Integer>> estimatedBiomeChunks = new ConcurrentHashMap<>();

    // Store whether a dungeon has been generated for a biome in a world
    private final Map<String, Map<Biome, Boolean>> generatedDungeons = new ConcurrentHashMap<>();

    // Store highest exploration percentage seen for each biome/world
    private final Map<String, Map<Biome, Double>> highestExplorationPercentage = new ConcurrentHashMap<>();

    /**
     * Record that a player has explored a chunk with a particular biome
     * @param player The player
     * @param world The world
     * @param biome The biome type
     * @return The percentage of this biome type explored (0.0 - 1.0)
     */
    public double recordExploredChunk(Player player, World world, Biome biome) {
        UUID playerId = player.getUniqueId();
        String worldName = world.getName();

        // Create the key for this world+biome combination
        String worldBiomeKey = worldName + ":" + biome.name();

        // Initialize maps if needed for player tracking
        playerBiomeExploration.putIfAbsent(playerId, new ConcurrentHashMap<>());
        playerBiomeExploration.get(playerId).putIfAbsent(worldName, new ConcurrentHashMap<>());

        // Get the player's exploration for this world and biome
        Map<Biome, Integer> biomeExploration = playerBiomeExploration.get(playerId).get(worldName);

        // Increment explored chunks for player
        int playerExplored = biomeExploration.getOrDefault(biome, 0) + 1;
        biomeExploration.put(biome, playerExplored);

        // Increment total explored chunks across all players
        totalExploredChunks.putIfAbsent(worldName, new ConcurrentHashMap<>());
        int totalExplored = totalExploredChunks.get(worldName).getOrDefault(biome, 0) + 1;
        totalExploredChunks.get(worldName).put(biome, totalExplored);

        // Get estimated total for this biome
        estimatedBiomeChunks.putIfAbsent(worldName, new ConcurrentHashMap<>());
        int estimatedTotal = estimatedBiomeChunks.get(worldName).getOrDefault(biome, -1);

        if (estimatedTotal == -1) {
            // Set initial estimate based on biome type
            estimatedTotal = getEstimatedBiomeSize(world, biome);
            estimatedBiomeChunks.get(worldName).put(biome, estimatedTotal);
        }

        // Calculate percentage explored (based on total exploration, not just this player)
        double percentage = (double) totalExplored / estimatedTotal;

        // Update highest exploration percentage if this is higher
        highestExplorationPercentage.putIfAbsent(worldName, new ConcurrentHashMap<>());
        double currentHighest = highestExplorationPercentage.get(worldName).getOrDefault(biome, 0.0);

        if (percentage > currentHighest) {
            highestExplorationPercentage.get(worldName).put(biome, percentage);
        }

        // Return the highest percentage ever seen for this biome
        return highestExplorationPercentage.get(worldName).getOrDefault(biome, percentage);
    }

    /**
     * Get the exploration percentage for a biome in a world
     * Returns the highest percentage seen across all players
     */
    public double getExplorationPercentage(World world, Biome biome) {
        String worldName = world.getName();

        // If we haven't tracked this world/biome combo, return 0
        if (!highestExplorationPercentage.containsKey(worldName) ||
                !highestExplorationPercentage.get(worldName).containsKey(biome)) {
            return 0.0;
        }

        return highestExplorationPercentage.get(worldName).get(biome);
    }

    /**
     * Get an estimated size for a biome type
     */
    private int getEstimatedBiomeSize(World world, Biome biome) {
        // Very rough estimates - adjust these based on your world generation
        switch (biome) {
            case OCEAN:
            case DEEP_OCEAN:
                return 5000; // Oceans are typically large

            case DESERT:
            case PLAINS:
            case FOREST:
            case TAIGA:
                return 2000; // Medium-sized common biomes

            case SWAMP:
            case JUNGLE:
            case BADLANDS:
            case ICE_SPIKES:
                return 1000; // Less common biomes

            case MUSHROOM_FIELDS:
            case BAMBOO_JUNGLE:
                return 500; // Rare biomes

            default:
                return 1500; // Default estimate
        }
    }

    /**
     * Check if a dungeon has been generated for this biome in this world
     */
    public boolean hasDungeonBeenGenerated(World world, Biome biome) {
        String worldName = world.getName();

        generatedDungeons.putIfAbsent(worldName, new ConcurrentHashMap<>());
        return generatedDungeons.get(worldName).getOrDefault(biome, false);
    }

    /**
     * Mark that a dungeon has been generated for this biome in this world
     */
    public void markDungeonGenerated(World world, Biome biome) {
        String worldName = world.getName();

        generatedDungeons.putIfAbsent(worldName, new ConcurrentHashMap<>());
        generatedDungeons.get(worldName).put(biome, true);
    }

    /**
     * Reset exploration data for a player
     */
    public void resetPlayerData(UUID playerId) {
        playerBiomeExploration.remove(playerId);
    }

    /**
     * Debug method to print exploration stats for a player in a specific biome
     */
    public String getDebugInfo(Player player, World world, Biome biome) {
        UUID playerId = player.getUniqueId();
        String worldName = world.getName();

        // Get player's explored chunks
        int playerExplored = 0;
        if (playerBiomeExploration.containsKey(playerId) &&
                playerBiomeExploration.get(playerId).containsKey(worldName) &&
                playerBiomeExploration.get(playerId).get(worldName).containsKey(biome)) {
            playerExplored = playerBiomeExploration.get(playerId).get(worldName).get(biome);
        }

        // Get total explored chunks
        int totalExplored = 0;
        if (totalExploredChunks.containsKey(worldName) &&
                totalExploredChunks.get(worldName).containsKey(biome)) {
            totalExplored = totalExploredChunks.get(worldName).get(biome);
        }

        // Get estimated total
        int estimatedTotal = 0;
        if (estimatedBiomeChunks.containsKey(worldName) &&
                estimatedBiomeChunks.get(worldName).containsKey(biome)) {
            estimatedTotal = estimatedBiomeChunks.get(worldName).get(biome);
        }

        // Get current percentage
        double percentage = getExplorationPercentage(world, biome) * 100;

        return String.format(
                "Biome: %s, Player Explored: %d, Total Explored: %d, Estimated Total: %d, Percentage: %.2f%%",
                biome.name(), playerExplored, totalExplored, estimatedTotal, percentage
        );
    }
}