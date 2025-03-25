package com.yourdomain.aidungeon.dungeons;

/**
 * Enum representing different types of rooms in a dungeon
 */
public enum RoomType {
    /**
     * Empty space, not a room
     */
    EMPTY,
    
    /**
     * Normal, generic room
     */
    NORMAL,
    
    /**
     * Dungeon entrance
     */
    ENTRANCE,
    
    /**
     * Room containing treasure
     */
    TREASURE,
    
    /**
     * Room containing a trap
     */
    TRAP,
    
    /**
     * Room containing a boss
     */
    BOSS;
    
    /**
     * Check if this room type is traversable (can be walked through)
     */
    public boolean isTraversable() {
        return this != EMPTY;
    }
    
    /**
     * Check if this room type is a special room
     */
    public boolean isSpecial() {
        return this == TREASURE || this == TRAP || this == BOSS || this == ENTRANCE;
    }
}
