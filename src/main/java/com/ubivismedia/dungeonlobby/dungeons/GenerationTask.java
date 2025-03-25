package com.ubivismedia.aidungeon.dungeons;

import java.util.UUID;

/**
 * Represents a task for generating a dungeon
 */
public class GenerationTask {
    
    private final BiomeArea area;
    private final UUID discovererUUID;
    private final long timestamp;
    
    /**
     * Create a new generation task
     */
    public GenerationTask(BiomeArea area, UUID discovererUUID) {
        this.area = area;
        this.discovererUUID = discovererUUID;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the biome area for this task
     */
    public BiomeArea getArea() {
        return area;
    }
    
    /**
     * Get the UUID of the player who discovered this dungeon
     */
    public UUID getDiscovererUUID() {
        return discovererUUID;
    }
    
    /**
     * Get the timestamp when this task was created
     */
    public long getTimestamp() {
        return timestamp;
    }
}
