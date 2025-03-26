package com.ubivismedia.aidungeon.dungeons;

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

/**
 * Enum representing different room variants for themes
 */
enum RoomVariant {
    DEFAULT,
    CIRCULAR,
    PILLARED,
    TEMPLE,
    NATURAL_CAVE,
    FLOODED,
    LIBRARY,
    TREASURE_VAULT,
    THRONE_ROOM,
    RITUALISTIC,
    SCULK_INFESTED;

    /**
     * Check if this variant is compatible with the given theme and room type
     */
    public boolean isCompatibleWith(String themeName, RoomType roomType) {
        switch (this) {
            case CIRCULAR:
                return themeName.equals("TEMPLE") || themeName.equals("UNDERWATER_RUINS") ||
                        roomType == RoomType.BOSS;

            case PILLARED:
                return themeName.equals("DWARVEN_HALLS") || themeName.equals("TEMPLE") ||
                        themeName.equals("NETHER_BASTION");

            case TEMPLE:
                return themeName.equals("TEMPLE") || themeName.equals("PYRAMID") ||
                        roomType == RoomType.BOSS;

            case NATURAL_CAVE:
                return themeName.equals("MINESHAFT") || roomType != RoomType.ENTRANCE;

            case FLOODED:
                return themeName.equals("UNDERWATER_RUINS") || themeName.equals("SUNKEN_TEMPLE");

            case LIBRARY:
                return themeName.equals("ANCIENT_LIBRARY") || themeName.equals("WITCH_HUT");

            case TREASURE_VAULT:
                return roomType == RoomType.TREASURE || themeName.equals("DWARVEN_HALLS");

            case THRONE_ROOM:
                return themeName.equals("ICE_CASTLE") || roomType == RoomType.BOSS;

            case RITUALISTIC:
                return themeName.equals("WITCH_HUT") || themeName.equals("NETHER_BASTION") || roomType == RoomType.BOSS;

            case SCULK_INFESTED:
                return themeName.equals("SCULK_DUNGEON") || roomType == RoomType.BOSS;

            case DEFAULT:
                return true; // Always compatible

            default:
                return false;
        }
    }
}
