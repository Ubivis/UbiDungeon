package com.ubivismedia.aidungeon.storage;

import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;

import java.util.UUID;

/**
 * Represents the data for a generated dungeon
 */
public class DungeonData {
    
    private final DungeonLayout layout;
    private final UUID discovererUUID;
    private final long timestamp;
    
    /**
     * Create new dungeon data
     */
    public DungeonData(DungeonLayout layout, UUID discovererUUID, long timestamp) {
        this.layout = layout;
        this.discovererUUID = discovererUUID;
        this.timestamp = timestamp;
    }
    
    /**
     * Create new dungeon data with current timestamp
     */
    public DungeonData(DungeonLayout layout, UUID discovererUUID) {
        this(layout, discovererUUID, System.currentTimeMillis());
    }
    
    /**
     * Get the dungeon layout
     */
    public DungeonLayout getLayout() {
        return layout;
    }
    
    /**
     * Get the UUID of the player who discovered this dungeon
     */
    public UUID getDiscovererUUID() {
        return discovererUUID;
    }
    
    /**
     * Get the timestamp when this dungeon was generated
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the theme of this dungeon
     */
    public DungeonTheme getTheme() {
        return layout.getTheme();
    }
    
    /**
     * Check if this dungeon is older than a specific time
     */
    public boolean isOlderThan(long duration) {
        return System.currentTimeMillis() - timestamp > duration;
    }
}
