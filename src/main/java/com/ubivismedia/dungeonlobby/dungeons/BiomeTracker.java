package com.ubivismedia.aidungeon.dungeons;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yourdomain.aidungeon.AIDungeonGenerator;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tracks player movement across biomes and detects when
 * a player has entered a new biome area
 */
public class BiomeTracker {
    
    private final AIDungeonGenerator plugin;
    
    // Map of player UUIDs to their last seen biome areas
    private final Map<UUID, BiomeArea> playerBiomeAreas = new HashMap<>();
    
    // Cache to prevent too frequent biome checks
    private final Cache<UUID, Long> playerCheckCooldown;
    
    // Radius for biome areas
    private final int biomeAreaRadius;
    
    /**
     * Create a new BiomeTracker
     */
    public BiomeTracker(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        this.biomeAreaRadius = plugin.getConfig().getInt("discovery.discovery-radius", 100);
        
        // Set up cooldown cache (check every 5 seconds per player)
        this.playerCheckCooldown = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Check if a player has entered a new biome area
     * Returns the new biome area if they have, null if not
     */
    public BiomeArea checkPlayerBiomeChange(Player player) {
        // Skip if on cooldown
        if (playerCheckCooldown.getIfPresent(player.getUniqueId()) != null) {
            return null;
        }
        
        // Update cooldown
        playerCheckCooldown.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Get current biome area
        Location location = player.getLocation();
        
        // Skip if in invalid world
        if (location.getWorld() == null) {
            return null;
        }
        
        // Get current biome
        Biome currentBiome = location.getWorld().getBiome(
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ()
        );
        
        // Skip if biome is not configured for dungeons
        if (!isDungeonBiome(currentBiome)) {
            return null;
        }
        
        // Create biome area from current location
        // We normalize to a grid to avoid creating too many overlapping areas
        int gridSize = biomeAreaRadius * 2;
        int normalizedX = Math.round(location.getBlockX() / (float)gridSize) * gridSize;
        int normalizedZ = Math.round(location.getBlockZ() / (float)gridSize) * gridSize;
        
        BiomeArea currentArea = new BiomeArea(
                location.getWorld().getName(),
                normalizedX,
                normalizedZ,
                biomeAreaRadius,
                currentBiome
        );
        
        // Get last seen area for this player
        BiomeArea lastArea = playerBiomeAreas.get(player.getUniqueId());
        
        // Check if this is a new area
        if (lastArea == null || !lastArea.equals(currentArea)) {
            // Update player's current area
            playerBiomeAreas.put(player.getUniqueId(), currentArea);
            
            // This is a new area
            return currentArea;
        }
        
        // No change
        return null;
    }
    
    /**
     * Check if a biome is configured for dungeon generation
     */
    private boolean isDungeonBiome(Biome biome) {
        return plugin.getConfig().isSet("biome-themes." + biome.name());
    }
    
    /**
     * Clear tracking data for a player
     */
    public void clearPlayer(UUID playerUuid) {
        playerBiomeAreas.remove(playerUuid);
        playerCheckCooldown.invalidate(playerUuid);
    }
}
