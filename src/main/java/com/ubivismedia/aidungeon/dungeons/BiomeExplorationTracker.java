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

    // Store estimated total chunks per biome type in each world
    private final Map<String, Map<Biome, Integer>> estimatedBiomeChunks = new ConcurrentHashMap<>();

    // Store whether a dungeon has been generated for a biome in a world
    private final Map<String, Map<Biome, Boolean>> generatedDungeons = new ConcurrentHashMap<>();

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

        // Initialize maps if needed
        playerBiomeExploration.putIfAbsent(playerId, new ConcurrentHashMap<>());
        playerBiomeExploration.get(playerId).putIfAbsent(worldName, new ConcurrentHashMap<>());

        // Get the player's exploration for this world and biome
        Map<Biome, Integer> biomeExploration = playerBiomeExploration.get(playerId).get(worldName);

        // Increment explored chunks
        int explored = biomeExploration.getOrDefault(biome, 0) + 1;
        biomeExploration.put(biome, explored);

        // Get estimated total for this biome
        estimatedBiomeChunks.putIfAbsent(worldName, new ConcurrentHashMap<>());

        // For simplicity, we'll estimate biome sizes based on world type
        // In a real implementation, you'd want a more accurate estimation
        int estimatedTotal = estimatedBiomeChunks.get(worldName).getOrDefault(biome, -1);
        if (estimatedTotal == -1) {
            // Set initial estimate based on biome type
            // These are arbitrary values - adjust based on your server
            estimatedTotal = getEstimatedBiomeSize(world, biome);
            estimatedBiomeChunks.get(worldName).put(biome, estimatedTotal);
        }

        // Calculate percentage explored
        return (double) explored / estimatedTotal;
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
}