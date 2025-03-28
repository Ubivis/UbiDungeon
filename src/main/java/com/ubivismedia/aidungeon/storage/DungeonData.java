package com.ubivismedia.aidungeon.storage;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;
import com.ubivismedia.aidungeon.dungeons.RoomType;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;

import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

/**
 * Represents the data for a generated dungeon
 */
public class DungeonData {
    
    private final DungeonLayout layout;
    private final AIDungeonGenerator plugin;
    private final UUID discovererUUID;
    private final long timestamp;
    
    /**
     * Create new dungeon data
     */
    public DungeonData(DungeonLayout layout, UUID discovererUUID, long timestamp, AIDungeonGenerator plugin) {
        this.layout = layout;
        this.discovererUUID = discovererUUID;
        this.timestamp = timestamp;
        this.plugin = plugin;
    }
    
    /**
     * Create new dungeon data with current timestamp
     */
    public DungeonData(DungeonLayout layout, UUID discovererUUID) {
        this(layout, discovererUUID, System.currentTimeMillis(), null);
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

    public String getBossTypeAt(Location location) {
        // Convert world location to layout coordinates
        int x = calculateXInLayout(location.getBlockX());
        int z = calculateZInLayout(location.getBlockZ());
        
        return layout.getBossTypeAt(x, z);
    }

    public RoomType getRoomTypeAt(Location location) {
        // Convert world location to layout coordinates
        int x = convertToLayoutX(location.getBlockX());
        int z = convertToLayoutZ(location.getBlockZ());
        
        return layout.getRoomType(x, z);
    }

    private int convertToLayoutX(int worldX) {
        // Calculate X coordinate in layout
        // Based on dungeon center and size
        return worldX - (getCenterX() - layout.getSize()/2);
    }

    private int convertToLayoutZ(int worldZ) {
        // Calculate Z coordinate in layout
        return worldZ - (getCenterZ() - layout.getSize()/2);
    }

    private int getCenterX() {
        // Get center X from BiomeArea
        for (BiomeArea area : plugin.getDungeonManager().getAllDungeons().keySet()) {
            if (getDungeonId().equals(area.getUniqueId())) {
                return area.getCenterX();
            }
        }
        return 0;
    }

    private int getCenterZ() {
        // Get center Z from BiomeArea
        for (BiomeArea area : plugin.getDungeonManager().getAllDungeons().keySet()) {
            if (getDungeonId().equals(area.getUniqueId())) {
                return area.getCenterZ();
            }
        }
        return 0;
    }

    private int calculateXInLayout(int worldX) {
        // Convert world X coordinate to layout X coordinate
        // Based on dungeon center position and layout size
        return worldX - (getArea().getCenterX() - layout.getSize()/2);
    }

    private int calculateZInLayout(int worldZ) {
        // Convert world Z coordinate to layout Z coordinate
        return worldZ - (getArea().getCenterZ() - layout.getSize()/2);
    }

    // Helper method to get the BiomeArea for this dungeon
    private BiomeArea getArea() {
        for (Map.Entry<BiomeArea, DungeonData> entry : plugin.getDungeonManager().getAllDungeons().entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Could not find BiomeArea for this DungeonData");
    }

    private String getDungeonId() {
        for (Map.Entry<BiomeArea, DungeonData> entry : plugin.getDungeonManager().getAllDungeons().entrySet()) {
            if (entry.getValue() == this) {
                return entry.getKey().getUniqueId();
            }
        }
        return null;
    }
}
