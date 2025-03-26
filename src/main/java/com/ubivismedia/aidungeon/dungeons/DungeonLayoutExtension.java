package com.ubivismedia.aidungeon.dungeons;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Set;

/**
 * Extension to the placeRoom method of DungeonLayout to use ThemeRoomGenerator
 * for theme-specific room generation
 */
public class DungeonLayoutExtension {

    /**
     * Enhanced room placement method to replace the default implementation
     * in DungeonLayout with theme-specific room generation
     *
     * @param layout The original DungeonLayout
     * @param world The world to place the room in
     * @param baseX The base X coordinate
     * @param baseY The base Y coordinate
     * @param baseZ The base Z coordinate
     * @param type The room type to place
     * @param placedBlocks Set of already placed blocks
     */
    public static void placeThemeRoom(DungeonLayout layout, World world,
                                      int baseX, int baseY, int baseZ,
                                      RoomType type, Set<Vector> placedBlocks) {
        // Room size based on type
        int roomWidth = 7;
        int roomHeight = 5;

        if (type == RoomType.BOSS) {
            roomWidth = 11;
            roomHeight = 7;
        } else if (type == RoomType.ENTRANCE) {
            roomWidth = 9;
            roomHeight = 6;
        }

        // Create a new theme room generator
        ThemeRoomGenerator roomGenerator = new ThemeRoomGenerator(layout.getTheme());

        // Set the center location of the room
        Location center = new Location(world, baseX, baseY, baseZ);

        // Generate the themed room
        Set<Vector> generatedBlocks = roomGenerator.generateThemedRoom(world, center, type, roomWidth, roomHeight);

        // Add all generated blocks to the placed blocks set
        placedBlocks.addAll(generatedBlocks);
    }

    /**
     * Modifies the DungeonLayout's placeInWorld method to use theme-specific rooms
     *
     * @param layout The original DungeonLayout
     * @param baseLocation The location to place the dungeon
     */
    public static void placeThemeBasedDungeonInWorld(DungeonLayout layout, Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) return;

        int baseX = baseLocation.getBlockX() - (layout.getSize() / 2);
        int baseY = baseLocation.getBlockY();
        int baseZ = baseLocation.getBlockZ() - (layout.getSize() / 2);

        // Set of blocks we've already placed to avoid duplicates
        Set<Vector> placedBlocks = new java.util.HashSet<>();

        // Generate rooms and corridors
        for (int x = 0; x < layout.getSize(); x++) {
            for (int z = 0; z < layout.getSize(); z++) {
                RoomType type = layout.getRoomType(x, z);
                if (type != RoomType.EMPTY) {
                    // Place theme-specific room instead of default
                    placeThemeRoom(layout, world, baseX + x, baseY, baseZ + z, type, placedBlocks);
                }
            }
        }

        // Generate corridors between adjacent rooms - use original method or enhanced version
        for (int x = 0; x < layout.getSize(); x++) {
            for (int z = 0; z < layout.getSize(); z++) {
                if (layout.isRoom(x, z)) {
                    // Check each direction
                    if (layout.isRoom(x + 1, z)) {
                        placeCorridor(world, baseX + x, baseY, baseZ + z,
                                org.bukkit.block.BlockFace.EAST, layout.getTheme(), placedBlocks);
                    }
                    if (layout.isRoom(x, z + 1)) {
                        placeCorridor(world, baseX + x, baseY, baseZ + z,
                                org.bukkit.block.BlockFace.SOUTH, layout.getTheme(), placedBlocks);
                    }
                }
            }
        }
    }

    /**
     * Place a themed corridor between rooms
     */
    private static void placeCorridor(World world, int x, int baseY, int z,
                                      org.bukkit.block.BlockFace direction,
                                      com.ubivismedia.aidungeon.config.DungeonTheme theme,
                                      Set<Vector> placedBlocks) {
        // Corridor width and height
        int width = 3;
        int height = 3;

        // Materials from theme
        java.util.List<org.bukkit.Material> floorMaterials = theme.getFloorBlocks();
        java.util.List<org.bukkit.Material> wallMaterials = theme.getPrimaryBlocks();
        java.util.List<org.bukkit.Material> ceilingMaterials = theme.getCeilingBlocks();

        // Random for material selection
        java.util.Random random = new java.util.Random();

        // Placement offset based on direction
        int offsetX = direction == org.bukkit.block.BlockFace.EAST ? 3 : 0;
        int offsetZ = direction == org.bukkit.block.BlockFace.SOUTH ? 3 : 0;

        // Length of corridor
        int length = 5;

        // Place corridor
        for (int i = 0; i < length; i++) {
            int posX = x + offsetX + (direction == org.bukkit.block.BlockFace.EAST ? i : 0);
            int posZ = z + offsetZ + (direction == org.bukkit.block.BlockFace.SOUTH ? i : 0);

            for (int w = -width/2; w <= width/2; w++) {
                for (int h = 0; h < height; h++) {
                    int worldX = posX + (direction == org.bukkit.block.BlockFace.SOUTH ? w : 0);
                    int worldY = baseY + h;
                    int worldZ = posZ + (direction == org.bukkit.block.BlockFace.EAST ? w : 0);

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (placedBlocks.contains(pos)) continue;

                    // Determine what to place
                    if (h == 0) {
                        // Floor
                        org.bukkit.Material floor = floorMaterials.get(random.nextInt(floorMaterials.size()));
                        world.setType(worldX, worldY, worldZ, floor);
                    } else if (h == height - 1) {
                        // Ceiling
                        org.bukkit.Material ceiling = ceilingMaterials.get(random.nextInt(ceilingMaterials.size()));
                        world.setType(worldX, worldY, worldZ, ceiling);
                    } else if (Math.abs(w) == width/2) {
                        // Walls
                        org.bukkit.Material wall = wallMaterials.get(random.nextInt(wallMaterials.size()));
                        world.setType(worldX, worldY, worldZ, wall);
                    } else {
                        // Interior
                        world.setType(worldX, worldY, worldZ, org.bukkit.Material.AIR);
                    }

                    placedBlocks.add(pos);
                }
            }

            // Add occasional light sources in corridors
            if (i % 2 == 0 && random.nextDouble() < 0.7) {
                int lightX = posX + (direction == org.bukkit.block.BlockFace.SOUTH ? 0 : 0);
                int lightY = baseY + 2;
                int lightZ = posZ + (direction == org.bukkit.block.BlockFace.EAST ? 0 : 0);

                Vector lightPos = new Vector(lightX, lightY, lightZ);
                if (!placedBlocks.contains(lightPos)) {
                    world.setType(lightX, lightY, lightZ, theme.getLightBlocks().get(0));
                    placedBlocks.add(lightPos);
                }
            }
        }
    }
}