package com.ubivismedia.aidungeon.dungeons;

import com.ubivismedia.aidungeon.config.DungeonTheme;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Represents the layout of a dungeon including rooms,
 * corridors, and special features
 */
public class DungeonLayout {
    
    // Grid representation of the dungeon
    private final int size;
    private final RoomType[][] grid;
    private final Map<RoomType, List<Vector>> roomPositions;
    
    // Theme for this dungeon
    private final DungeonTheme theme;
    
    // Entrance position
    private int entranceX;
    private int entranceY;
    
    // Random for placements
    private final Random random = new Random();
    
    /**
     * Create a new empty dungeon layout
     */
    public DungeonLayout(int size, DungeonTheme theme) {
        this.size = size;
        this.grid = new RoomType[size][size];
        this.theme = theme;
        this.roomPositions = new HashMap<>();
        
        // Initialize all cells to empty
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                grid[x][y] = RoomType.EMPTY;
            }
        }
        
        // Initialize room position lists for each type
        for (RoomType type : RoomType.values()) {
            roomPositions.put(type, new ArrayList<>());
        }
    }
    
    /**
     * Set a cell to be a room of a specific type
     */
    public void setRoomType(int x, int y, RoomType type) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return;
        }
        
        // Remove from previous room type list
        RoomType oldType = grid[x][y];
        if (oldType != RoomType.EMPTY) {
            roomPositions.get(oldType).removeIf(v -> v.getBlockX() == x && v.getBlockZ() == y);
        }
        
        // Set new type
        grid[x][y] = type;
        
        // Add to new room type list if not empty
        if (type != RoomType.EMPTY) {
            roomPositions.get(type).add(new Vector(x, 0, y));
        }
    }
    
    /**
     * Get the room type at a specific position
     */
    public RoomType getRoomType(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            return RoomType.EMPTY;
        }
        return grid[x][y];
    }
    
    /**
     * Check if a position contains a room (any non-empty type)
     */
    public boolean isRoom(int x, int y) {
        return getRoomType(x, y) != RoomType.EMPTY;
    }
    
    /**
     * Get all room positions of a specific type
     */
    public List<Vector> getRoomPositions(RoomType type) {
        return roomPositions.getOrDefault(type, Collections.emptyList());
    }
    
    /**
     * Set the entrance position
     */
    public void setEntrancePosition(int x, int y) {
        this.entranceX = x;
        this.entranceY = y;
        setRoomType(x, y, RoomType.ENTRANCE);
    }
    
    /**
     * Get the entrance X coordinate
     */
    public int getEntranceX() {
        return entranceX;
    }
    
    /**
     * Get the entrance Y coordinate
     */
    public int getEntranceY() {
        return entranceY;
    }
    
    /**
     * Get the size of the dungeon grid
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Get the theme of this dungeon
     */
    public DungeonTheme getTheme() {
        return theme;
    }
    
    /**
     * Place the dungeon in the world at the specified location
     */
    public void placeInWorld(Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) return;
        
        int baseX = baseLocation.getBlockX() - (size / 2);
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ() - (size / 2);
        
        // Set of blocks we've already placed to avoid duplicates
        Set<Vector> placedBlocks = new HashSet<>();
        
        // Generate rooms and corridors
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                RoomType type = grid[x][z];
                if (type != RoomType.EMPTY) {
                    placeRoom(world, baseX + x, baseY, baseZ + z, type, placedBlocks);
                }
            }
        }
        
        // Generate corridors between adjacent rooms
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (isRoom(x, z)) {
                    // Check each direction
                    if (isRoom(x + 1, z)) {
                        placeCorridor(world, baseX + x, baseY, baseZ + z, BlockFace.EAST, placedBlocks);
                    }
                    if (isRoom(x, z + 1)) {
                        placeCorridor(world, baseX + x, baseY, baseZ + z, BlockFace.SOUTH, placedBlocks);
                    }
                }
            }
        }
    }
    
    /**
     * Place a room at the specified location
     */
    private void placeRoom(World world, int baseX, int baseY, int baseZ, RoomType type, Set<Vector> placedBlocks) {
        // Room size based on type
        int roomWidth = 7;
        int roomHeight = 5;
        
        if (type == RoomType.BOSS) {
            roomWidth = 11;
            roomHeight = 7;
        } else if (type == RoomType.ENTRANCE) {
            // Create stairs up to surface
            placeEntrance(world, baseX, baseY, baseZ, placedBlocks);
            return;
        }
        
        // Materials based on theme and room type
        List<Material> floorMaterials = theme.getFloorBlocks();
        List<Material> wallMaterials = theme.getPrimaryBlocks();
        List<Material> ceilingMaterials = theme.getCeilingBlocks();
        
        // Room center
        int centerX = baseX;
        int centerZ = baseZ;
        
        // Create room structure
        for (int x = -roomWidth/2; x <= roomWidth/2; x++) {
            for (int z = -roomWidth/2; z <= roomWidth/2; z++) {
                for (int y = 0; y < roomHeight; y++) {
                    int worldX = centerX + x;
                    int worldY = baseY + y;
                    int worldZ = centerZ + z;
                    
                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (placedBlocks.contains(pos)) continue;
                    
                    // Determine what to place
                    if (y == 0) {
                        // Floor
                        Material floor = floorMaterials.get(random.nextInt(floorMaterials.size()));
                        world.setType(worldX, worldY, worldZ, floor);
                    } else if (y == roomHeight - 1) {
                        // Ceiling
                        Material ceiling = ceilingMaterials.get(random.nextInt(ceilingMaterials.size()));
                        world.setType(worldX, worldY, worldZ, ceiling);
                    } else if (Math.abs(x) == roomWidth/2 || Math.abs(z) == roomWidth/2) {
                        // Walls
                        Material wall = wallMaterials.get(random.nextInt(wallMaterials.size()));
                        world.setType(worldX, worldY, worldZ, wall);
                    } else {
                        // Interior
                        world.setType(worldX, worldY, worldZ, Material.AIR);
                    }
                    
                    placedBlocks.add(pos);
                }
            }
        }
        
        // Add special features based on room type
        switch (type) {
            case TREASURE:
                placeTreasure(world, centerX, baseY + 1, centerZ, placedBlocks);
                break;
            case TRAP:
                placeTrap(world, centerX, baseY + 1, centerZ, placedBlocks);
                break;
            case BOSS:
                placeBossRoom(world, centerX, baseY + 1, centerZ, placedBlocks);
                break;
            default:
                // Add random decorations
                placeDecorations(world, centerX, baseY + 1, centerZ, placedBlocks);
                break;
        }
    }
    
    /**
     * Place a corridor connecting two rooms
     */
    private void placeCorridor(World world, int x, int baseY, int z, BlockFace direction, Set<Vector> placedBlocks) {
        // Corridor width and height
        int width = 3;
        int height = 3;
        
        // Materials
        List<Material> floorMaterials = theme.getFloorBlocks();
        List<Material> wallMaterials = theme.getPrimaryBlocks();
        List<Material> ceilingMaterials = theme.getCeilingBlocks();
        
        // Placement offset based on direction
        int offsetX = direction == BlockFace.EAST ? 3 : 0;
        int offsetZ = direction == BlockFace.SOUTH ? 3 : 0;
        
        // Length of corridor
        int length = 5;
        
        // Place corridor
        for (int i = 0; i < length; i++) {
            int posX = x + offsetX + (direction == BlockFace.EAST ? i : 0);
            int posZ = z + offsetZ + (direction == BlockFace.SOUTH ? i : 0);
            
            for (int w = -width/2; w <= width/2; w++) {
                for (int h = 0; h < height; h++) {
                    int worldX = posX + (direction == BlockFace.SOUTH ? w : 0);
                    int worldY = baseY + h;
                    int worldZ = posZ + (direction == BlockFace.EAST ? w : 0);
                    
                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (placedBlocks.contains(pos)) continue;
                    
                    // Determine what to place
                    if (h == 0) {
                        // Floor
                        Material floor = floorMaterials.get(random.nextInt(floorMaterials.size()));
                        world.setType(worldX, worldY, worldZ, floor);
                    } else if (h == height - 1) {
                        // Ceiling
                        Material ceiling = ceilingMaterials.get(random.nextInt(ceilingMaterials.size()));
                        world.setType(worldX, worldY, worldZ, ceiling);
                    } else if (Math.abs(w) == width/2) {
                        // Walls
                        Material wall = wallMaterials.get(random.nextInt(wallMaterials.size()));
                        world.setType(worldX, worldY, worldZ, wall);
                    } else {
                        // Interior
                        world.setType(worldX, worldY, worldZ, Material.AIR);
                    }
                    
                    placedBlocks.add(pos);
                }
            }
        }
    }
    
    /**
     * Place the entrance room with stairs to the surface
     */
    private void placeEntrance(World world, int x, int y, int z, Set<Vector> placedBlocks) {
        // TODO: Implement custom entrance with stairs leading to surface
        // For now, just place a basic room
        placeRoom(world, x, y, z, RoomType.NORMAL, placedBlocks);
        
        // Mark entrance with a beacon
        world.setType(x, y + 1, z, Material.BEACON);
    }
    
    /**
     * Place treasure in a treasure room
     */
    private void placeTreasure(World world, int x, int y, int z, Set<Vector> placedBlocks) {
        // Place a chest in the center
        world.setType(x, y, z, Material.CHEST);
        
        // Surround with some gold blocks
        world.setType(x + 1, y, z, Material.GOLD_BLOCK);
        world.setType(x - 1, y, z, Material.GOLD_BLOCK);
        world.setType(x, y, z + 1, Material.GOLD_BLOCK);
        world.setType(x, y, z - 1, Material.GOLD_BLOCK);
        
        // Add to placed blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placedBlocks.add(new Vector(x + dx, y, z + dz));
            }
        }
    }
    
    /**
     * Place a trap in a trap room
     */
    private void placeTrap(World world, int x, int y, int z, Set<Vector> placedBlocks) {
        // For now just place pressure plates on TNT
        // In a real implementation, you'd use more complex trap designs
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (random.nextDouble() < 0.3) {
                    world.setType(x + dx, y - 1, z + dz, Material.TNT);
                    world.setType(x + dx, y, z + dz, Material.STONE_PRESSURE_PLATE);
                    
                    placedBlocks.add(new Vector(x + dx, y - 1, z + dz));
                    placedBlocks.add(new Vector(x + dx, y, z + dz));
                }
            }
        }
    }
    
    /**
     * Place special features in a boss room
     */
    private void placeBossRoom(World world, int x, int y, int z, Set<Vector> placedBlocks) {
        // Place a spawner in the center
        world.setType(x, y, z, Material.SPAWNER);
        placedBlocks.add(new Vector(x, y, z));
        
        // Add some lava pools
        for (int i = 0; i < 4; i++) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            
            if (dx == 0 && dz == 0) continue; // Skip center
            
            world.setType(x + dx, y - 1, z + dz, Material.LAVA);
            placedBlocks.add(new Vector(x + dx, y - 1, z + dz));
        }
    }
    
    /**
     * Place random decorations in a room
     */
    private void placeDecorations(World world, int x, int y, int z, Set<Vector> placedBlocks) {
        // Add some random decorations based on theme
        List<Material> lightBlocks = theme.getLightBlocks();
        
        // Add some light sources
        for (int i = 0; i < 2; i++) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            
            Material light = lightBlocks.get(random.nextInt(lightBlocks.size()));
            world.setType(x + dx, y, z + dz, light);
            placedBlocks.add(new Vector(x + dx, y, z + dz));
        }
    }
}
