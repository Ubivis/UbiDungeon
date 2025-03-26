package com.ubivismedia.aidungeon.dungeons;

import com.ubivismedia.aidungeon.config.DungeonTheme;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Extension to handle theme-specific room generation for dungeons
 */
public class ThemeRoomGenerator {

    private final Random random = new Random();
    private final DungeonTheme theme;

    // Map to track which positions have been occupied
    private final Set<Vector> occupiedPositions = new HashSet<>();

    /**
     * Create a new theme room generator for a specific theme
     */
    public ThemeRoomGenerator(DungeonTheme theme) {
        this.theme = theme;
    }

    /**
     * Generate a themed room at the specified location
     * @param world The world to place blocks in
     * @param center The center position of the room
     * @param roomType The type of room to generate
     * @param roomWidth The width of the room
     * @param roomHeight The height of the room
     * @return Set of positions that were modified
     */
    public Set<Vector> generateThemedRoom(World world, Location center, RoomType roomType, int roomWidth, int roomHeight) {
        Set<Vector> placedPositions = new HashSet<>();

        // Select room variant based on theme and room type
        RoomVariant variant = selectRoomVariant(theme.getName(), roomType);

        // Place the main structure of the room
        placeRoomStructure(world, center, roomWidth, roomHeight, variant, placedPositions);

        // Add theme-specific decorations
        addRoomDecorations(world, center, roomWidth, roomHeight, roomType, variant, placedPositions);

        // Add room-type specific features
        addRoomFeatures(world, center.getBlockX(), center.getBlockY(), center.getBlockZ(),
                roomWidth, roomHeight, roomType, variant, placedPositions);

        // Record occupied positions
        occupiedPositions.addAll(placedPositions);

        return placedPositions;
    }

    /**
     * Select a room variant based on the theme and room type
     */
    private RoomVariant selectRoomVariant(String themeName, RoomType roomType) {
        List<RoomVariant> availableVariants = new ArrayList<>();

        // Get variants appropriate for this theme and room type
        for (RoomVariant variant : RoomVariant.values()) {
            if (variant.isCompatibleWith(themeName, roomType)) {
                availableVariants.add(variant);
            }
        }

        // If no specific variants, use default
        if (availableVariants.isEmpty()) {
            return RoomVariant.DEFAULT;
        }

        // Select random variant from available options
        return availableVariants.get(random.nextInt(availableVariants.size()));
    }

    /**
     * Place the main structure of the room
     */
    private void placeRoomStructure(World world, Location center, int roomWidth, int roomHeight,
                                    RoomVariant variant, Set<Vector> placedPositions) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        List<Material> primaryBlocks = theme.getPrimaryBlocks();
        List<Material> accentBlocks = theme.getAccentBlocks();
        List<Material> floorBlocks = theme.getFloorBlocks();
        List<Material> ceilingBlocks = theme.getCeilingBlocks();

        // Adjust structure based on variant
        switch (variant) {
            case CIRCULAR:
                placeCircularRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case PILLARED:
                placeSquareRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                placePillars(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        accentBlocks, placedPositions);
                break;

            case TEMPLE:
                placeTempleRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case NATURAL_CAVE:
                placeNaturalCaveRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case FLOODED:
                placeFloodedRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case LIBRARY:
                placeLibraryRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case TREASURE_VAULT:
                placeTreasureVaultRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case THRONE_ROOM:
                placeThroneRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case RITUALISTIC:
                placeRitualisticRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case SCULK_INFESTED:
                placeSculkInfestedRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;

            case DEFAULT:
            default:
                placeSquareRoom(world, centerX, centerY, centerZ, roomWidth, roomHeight,
                        primaryBlocks, accentBlocks, floorBlocks, ceilingBlocks, placedPositions);
                break;
        }
    }

    /**
     * Place a standard square room
     */
    private void placeSquareRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                 List<Material> primaryBlocks, List<Material> accentBlocks,
                                 List<Material> floorBlocks, List<Material> ceilingBlocks,
                                 Set<Vector> placedPositions) {
        int halfWidth = width / 2;

        // Create room structure
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int z = -halfWidth; z <= halfWidth; z++) {
                for (int y = 0; y < height; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Determine what to place
                    if (y == 0) {
                        // Floor
                        Material floor = floorBlocks.get(random.nextInt(floorBlocks.size()));
                        world.setType(worldX, worldY, worldZ, floor);
                    } else if (y == height - 1) {
                        // Ceiling
                        Material ceiling = ceilingBlocks.get(random.nextInt(ceilingBlocks.size()));
                        world.setType(worldX, worldY, worldZ, ceiling);
                    } else if (Math.abs(x) == halfWidth || Math.abs(z) == halfWidth) {
                        // Walls
                        if (y % 3 == 0 && random.nextDouble() < 0.3) {
                            // Accent blocks for texture
                            Material accent = accentBlocks.get(random.nextInt(accentBlocks.size()));
                            world.setType(worldX, worldY, worldZ, accent);
                        } else {
                            Material wall = primaryBlocks.get(random.nextInt(primaryBlocks.size()));
                            world.setType(worldX, worldY, worldZ, wall);
                        }
                    } else {
                        // Interior
                        world.setType(worldX, worldY, worldZ, Material.AIR);
                    }

                    placedPositions.add(pos);
                }
            }
        }
    }

    /**
     * Place a circular room
     */
    private void placeCircularRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                   List<Material> primaryBlocks, List<Material> accentBlocks,
                                   List<Material> floorBlocks, List<Material> ceilingBlocks,
                                   Set<Vector> placedPositions) {
        int radius = width / 2;
        double radiusSq = radius * radius;

        // Create circular room structure
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Check if within circle
                double distSq = x * x + z * z;
                if (distSq > radiusSq) continue;

                for (int y = 0; y < height; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Determine what to place
                    if (y == 0) {
                        // Floor
                        Material floor = floorBlocks.get(random.nextInt(floorBlocks.size()));
                        world.setType(worldX, worldY, worldZ, floor);
                    } else if (y == height - 1) {
                        // Ceiling
                        Material ceiling = ceilingBlocks.get(random.nextInt(ceilingBlocks.size()));
                        world.setType(worldX, worldY, worldZ, ceiling);
                    } else if (distSq >= (radius - 1) * (radius - 1)) {
                        // Walls (slightly inside the exact circle edge)
                        if (y % 3 == 0 && random.nextDouble() < 0.3) {
                            // Accent blocks for texture
                            Material accent = accentBlocks.get(random.nextInt(accentBlocks.size()));
                            world.setType(worldX, worldY, worldZ, accent);
                        } else {
                            Material wall = primaryBlocks.get(random.nextInt(primaryBlocks.size()));
                            world.setType(worldX, worldY, worldZ, wall);
                        }
                    } else {
                        // Interior
                        world.setType(worldX, worldY, worldZ, Material.AIR);
                    }

                    placedPositions.add(pos);
                }
            }
        }
    }

    /**
     * Place pillars in a room
     */
    private void placePillars(World world, int centerX, int centerY, int centerZ, int width, int height,
                              List<Material> accentBlocks, Set<Vector> placedPositions) {
        int halfWidth = width / 2;
        int pillarDistance = Math.max(2, halfWidth - 2);

        // Place pillars at the corners of an inner square
        for (int xOffset : new int[]{-pillarDistance, pillarDistance}) {
            for (int zOffset : new int[]{-pillarDistance, pillarDistance}) {
                for (int y = 0; y < height; y++) {
                    int worldX = centerX + xOffset;
                    int worldY = centerY + y;
                    int worldZ = centerZ + zOffset;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Use accent materials for pillars
                    Material pillar = accentBlocks.get(random.nextInt(accentBlocks.size()));
                    world.setType(worldX, worldY, worldZ, pillar);

                    placedPositions.add(pos);
                }
            }
        }
    }

    /**
     * Place a temple-style room with raised platform
     */
    private void placeTempleRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                 List<Material> primaryBlocks, List<Material> accentBlocks,
                                 List<Material> floorBlocks, List<Material> ceilingBlocks,
                                 Set<Vector> placedPositions) {
        // First place a square room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        // Add raised platform in the center
        int platformWidth = width / 3;
        int platformHeight = 1;

        for (int x = -platformWidth / 2; x <= platformWidth / 2; x++) {
            for (int z = -platformWidth / 2; z <= platformWidth / 2; z++) {
                int worldX = centerX + x;
                int worldY = centerY;
                int worldZ = centerZ + z;

                Vector pos = new Vector(worldX, worldY, worldZ);

                // Use accent material for platform
                Material platformMaterial = accentBlocks.get(random.nextInt(accentBlocks.size()));
                world.setType(worldX, worldY, worldZ, platformMaterial);

                placedPositions.add(pos);
            }
        }

        // Add stairs to the platform
        placeStairs(world, centerX, centerY, centerZ, platformWidth, BlockFace.NORTH,
                primaryBlocks.get(0), placedPositions);
        placeStairs(world, centerX, centerY, centerZ, platformWidth, BlockFace.SOUTH,
                primaryBlocks.get(0), placedPositions);
        placeStairs(world, centerX, centerY, centerZ, platformWidth, BlockFace.EAST,
                primaryBlocks.get(0), placedPositions);
    }

    /**
     * Place stairs in the specified direction
     */
    private void placeStairs(World world, int centerX, int centerY, int centerZ, int platformWidth,
                             BlockFace direction, Material material, Set<Vector> placedPositions) {
        // Calculate the starting position for stairs based on direction and platform size
        int stairX = centerX;
        int stairZ = centerZ;

        switch (direction) {
            case NORTH:
                stairZ = centerZ - platformWidth / 2 - 1;
                break;
            case SOUTH:
                stairZ = centerZ + platformWidth / 2 + 1;
                break;
            case EAST:
                stairX = centerX + platformWidth / 2 + 1;
                break;
            case WEST:
                stairX = centerX - platformWidth / 2 - 1;
                break;
        }

        // Place a stair block
        Block stairBlock = world.getBlockAt(stairX, centerY, stairZ);
        stairBlock.setType(getMaterialStair(material));

        // Set the stairs direction
        if (stairBlock.getBlockData() instanceof Stairs) {
            Stairs stairs = (Stairs) stairBlock.getBlockData();
            stairs.setFacing(direction.getOppositeFace());
            stairs.setHalf(Bisected.Half.BOTTOM);
            stairBlock.setBlockData(stairs);
        }

        placedPositions.add(new Vector(stairX, centerY, stairZ));
    }

    /**
     * Get a stair material that matches the primary material
     */
    private Material getMaterialStair(Material material) {
        // Convert primary material to a matching stair type if possible
        switch (material) {
            case STONE:
                return Material.STONE_STAIRS;
            case COBBLESTONE:
                return Material.COBBLESTONE_STAIRS;
            case STONE_BRICKS:
                return Material.STONE_BRICK_STAIRS;
            case SANDSTONE:
                return Material.SANDSTONE_STAIRS;
            case PRISMARINE:
                return Material.PRISMARINE_STAIRS;
            case PRISMARINE_BRICKS:
                return Material.PRISMARINE_BRICK_STAIRS;
            case DARK_PRISMARINE:
                return Material.DARK_PRISMARINE_STAIRS;
            case BLACKSTONE:
                return Material.BLACKSTONE_STAIRS;
            case DEEPSLATE:
                return Material.DEEPSLATE_TILE_STAIRS;
            case DEEPSLATE_BRICKS:
                return Material.DEEPSLATE_BRICK_STAIRS;
            case DEEPSLATE_TILES:
                return Material.DEEPSLATE_TILE_STAIRS;
            default:
                return Material.STONE_STAIRS;
        }
    }

    /**
     * Place a natural cave-like room
     */
    private void placeNaturalCaveRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                      List<Material> primaryBlocks, List<Material> accentBlocks,
                                      List<Material> floorBlocks, List<Material> ceilingBlocks,
                                      Set<Vector> placedPositions) {
        int radius = width / 2;
        double radiusSq = radius * radius;

        // Create an irregular cave-like room
        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                for (int y = -1; y < height + 1; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Calculate distance from center with some noise
                    double noise = (Math.sin(x * 0.5) + Math.cos(z * 0.5) + Math.sin(y * 0.5)) * 1.5;
                    double distSq = x * x + z * z + (y - height/2.0) * (y - height/2.0);
                    distSq -= noise * noise * 5;

                    if (distSq <= radiusSq) {
                        // Interior of cave
                        if (y == 0) {
                            // Floor
                            Material floor = floorBlocks.get(random.nextInt(floorBlocks.size()));
                            world.setType(worldX, worldY, worldZ, floor);
                        } else {
                            // Air for the cavern interior
                            world.setType(worldX, worldY, worldZ, Material.AIR);
                        }
                    } else if (distSq <= radiusSq + 9) { // Slightly larger than the cave interior
                        // Cave walls
                        if (random.nextDouble() < 0.15) {
                            // Accent blocks for texture
                            Material accent = accentBlocks.get(random.nextInt(accentBlocks.size()));
                            world.setType(worldX, worldY, worldZ, accent);
                        } else {
                            Material wall = primaryBlocks.get(random.nextInt(primaryBlocks.size()));
                            world.setType(worldX, worldY, worldZ, wall);
                        }
                    }

                    placedPositions.add(pos);
                }
            }
        }

        // Add stalactites and stalagmites as decoration
        for (int i = 0; i < radius * 2; i++) {
            int x = random.nextInt(width) - width/2;
            int z = random.nextInt(width) - width/2;

            // Only place if within the cave
            double dist = x * x + z * z;
            if (dist < radiusSq * 0.6) {
                // Stalagmite (from floor)
                int stalagmiteHeight = 1 + random.nextInt(3);
                Material material = accentBlocks.get(random.nextInt(accentBlocks.size()));

                for (int y = 1; y <= stalagmiteHeight; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (!occupiedPositions.contains(pos) && world.getBlockAt(worldX, worldY, worldZ).getType() == Material.AIR) {
                        world.setType(worldX, worldY, worldZ, material);
                        placedPositions.add(pos);
                    }
                }

                // Stalactite (from ceiling)
                if (random.nextBoolean()) {
                    int stalactiteHeight = 1 + random.nextInt(2);
                    material = accentBlocks.get(random.nextInt(accentBlocks.size()));

                    for (int y = 1; y <= stalactiteHeight; y++) {
                        int worldX = centerX + x;
                        int worldY = centerY + height - y;
                        int worldZ = centerZ + z;

                        Vector pos = new Vector(worldX, worldY, worldZ);
                        if (!occupiedPositions.contains(pos) && world.getBlockAt(worldX, worldY, worldZ).getType() == Material.AIR) {
                            world.setType(worldX, worldY, worldZ, material);
                            placedPositions.add(pos);
                        }
                    }
                }
            }
        }
    }

    /**
     * Place a flooded room with water and platforms
     */
    private void placeFloodedRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                  List<Material> primaryBlocks, List<Material> accentBlocks,
                                  List<Material> floorBlocks, List<Material> ceilingBlocks,
                                  Set<Vector> placedPositions) {
        // First place a square room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        int waterLevel = 2; // Height of water
        int halfWidth = width / 2;

        // Add water
        for (int x = -halfWidth + 1; x <= halfWidth - 1; x++) {
            for (int z = -halfWidth + 1; z <= halfWidth - 1; z++) {
                for (int y = 1; y <= waterLevel; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Set water blocks
                    world.setType(worldX, worldY, worldZ, Material.WATER);
                    placedPositions.add(pos);
                }
            }
        }

        // Add platforms above water
        addRandomPlatforms(world, centerX, centerY + waterLevel, centerZ, width,
                floorBlocks, placedPositions);
    }

    /**
     * Add random platforms in a flooded room
     */
    private void addRandomPlatforms(World world, int centerX, int baseY, int centerZ, int width,
                                    List<Material> materials, Set<Vector> placedPositions) {
        int halfWidth = width / 2;
        int numPlatforms = 2 + random.nextInt(3); // 2-4 platforms

        for (int i = 0; i < numPlatforms; i++) {
            // Determine platform position
            int platformWidth = 2 + random.nextInt(3); // 2-4 blocks wide
            int platformX = centerX + random.nextInt(width - platformWidth) - (width - platformWidth) / 2;
            int platformZ = centerZ + random.nextInt(width - platformWidth) - (width - platformWidth) / 2;

            // Place platform blocks
            for (int x = 0; x < platformWidth; x++) {
                for (int z = 0; z < platformWidth; z++) {
                    int worldX = platformX + x;
                    int worldY = baseY;
                    int worldZ = platformZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Set platform block
                    Material platformMaterial = materials.get(random.nextInt(materials.size()));
                    world.setType(worldX, worldY, worldZ, platformMaterial);
                    placedPositions.add(pos);
                }
            }
        }
    }

    /**
     * Place a library-themed room with bookshelves
     */
    private void placeLibraryRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                  List<Material> primaryBlocks, List<Material> accentBlocks,
                                  List<Material> floorBlocks, List<Material> ceilingBlocks,
                                  Set<Vector> placedPositions) {
        // First place a square room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        int halfWidth = width / 2;

        // Add bookshelves along the walls
        for (int x = -halfWidth + 1; x <= halfWidth - 1; x++) {
            for (int z = -halfWidth + 1; z <= halfWidth - 1; z++) {
                // Skip if not close to a wall
                if (Math.abs(x) < halfWidth - 1 && Math.abs(z) < halfWidth - 1) continue;

                for (int y = 1; y < height - 1; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Place bookshelves with some randomness
                    if (random.nextDouble() < 0.8) {
                        world.setType(worldX, worldY, worldZ, Material.BOOKSHELF);
                        placedPositions.add(pos);
                    }
                }
            }
        }

        // Add lecterns and tables in the center
        addLibraryFurniture(world, centerX, centerY, centerZ, width, placedPositions);
    }

    /**
     * Add furniture to a library room
     */
    private void addLibraryFurniture(World world, int centerX, int centerY, int centerZ, int width,
                                     Set<Vector> placedPositions) {
        // Add a central table (oak slabs)
        int tableWidth = width / 3;

        for (int x = -tableWidth/2; x <= tableWidth/2; x++) {
            for (int z = -tableWidth/2; z <= tableWidth/2; z++) {
                int worldX = centerX + x;
                int worldY = centerY + 1; // Table height
                int worldZ = centerZ + z;

                Vector pos = new Vector(worldX, worldY, worldZ);
                if (occupiedPositions.contains(pos)) continue;

                world.setType(worldX, worldY, worldZ, Material.OAK_SLAB);
                placedPositions.add(pos);
            }
        }

        // Add lecterns around the edges
        int lecternCount = 1 + random.nextInt(3); // 1-3 lecterns

        for (int i = 0; i < lecternCount; i++) {
            // Choose a random position near walls
            int x = random.nextBoolean() ? width/2 - 2 : -(width/2 - 2);
            int z = random.nextBoolean() ? width/2 - 2 : -(width/2 - 2);

            // Adjust for variety
            if (random.nextBoolean()) {
                x = random.nextInt(width - 4) - (width - 4)/2;
            } else {
                z = random.nextInt(width - 4) - (width - 4)/2;
            }

            int worldX = centerX + x;
            int worldY = centerY + 1;
            int worldZ = centerZ + z;

            Vector pos = new Vector(worldX, worldY, worldZ);
            if (occupiedPositions.contains(pos)) continue;

            world.setType(worldX, worldY, worldZ, Material.LECTERN);
            placedPositions.add(pos);
        }
    }

    /**
     * Place a treasure vault themed room
     */
    private void placeTreasureVaultRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                        List<Material> primaryBlocks, List<Material> accentBlocks,
                                        List<Material> floorBlocks, List<Material> ceilingBlocks,
                                        Set<Vector> placedPositions) {
        // First place a square room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        // Add stone pedestals for treasure
        int pedestalCount = 3 + random.nextInt(3); // 3-5 pedestals
        List<Vector> pedestalPositions = new ArrayList<>();

        // Place central podium
        int centralPedestalX = centerX;
        int centralPedestalY = centerY;
        int centralPedestalZ = centerZ;

        world.setType(centralPedestalX, centralPedestalY + 1, centralPedestalZ, Material.GOLD_BLOCK);
        pedestalPositions.add(new Vector(centralPedestalX, centralPedestalY + 1, centralPedestalZ));
        placedPositions.add(new Vector(centralPedestalX, centralPedestalY + 1, centralPedestalZ));

        // Place other pedestals in a circle
        double angleStep = 2 * Math.PI / pedestalCount;
        int radius = width / 3;

        for (int i = 0; i < pedestalCount; i++) {
            double angle = i * angleStep;
            int x = (int) (Math.cos(angle) * radius);
            int z = (int) (Math.sin(angle) * radius);

            int pedestalX = centerX + x;
            int pedestalY = centerY;
            int pedestalZ = centerZ + z;

            // Create a simple pedestal
            world.setType(pedestalX, pedestalY + 1, pedestalZ, Material.CHISELED_STONE_BRICKS);
            placedPositions.add(new Vector(pedestalX, pedestalY + 1, pedestalZ));

            // Place treasure chest on some pedestals
            if (random.nextDouble() < 0.6) {
                world.setType(pedestalX, pedestalY + 2, pedestalZ, Material.CHEST);
                placedPositions.add(new Vector(pedestalX, pedestalY + 2, pedestalZ));
            }
        }

        // Add gold and emerald blocks as decoration
        for (int i = 0; i < pedestalCount + 2; i++) {
            int x = random.nextInt(width - 4) - (width - 4)/2;
            int z = random.nextInt(width - 4) - (width - 4)/2;

            int blockX = centerX + x;
            int blockY = centerY + 1;
            int blockZ = centerZ + z;

            Vector pos = new Vector(blockX, blockY, blockZ);
            if (occupiedPositions.contains(pos)) continue;

            // Place decorative blocks
            Material treasureMaterial = random.nextBoolean() ? Material.GOLD_BLOCK : Material.EMERALD_BLOCK;
            world.setType(blockX, blockY, blockZ, treasureMaterial);
            placedPositions.add(pos);
        }
    }

    /**
     * Place a throne room
     */
    private void placeThroneRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                 List<Material> primaryBlocks, List<Material> accentBlocks,
                                 List<Material> floorBlocks, List<Material> ceilingBlocks,
                                 Set<Vector> placedPositions) {
        // First place a square room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        // Build a throne at one end of the room
        int throneX = centerX;
        int throneY = centerY;
        int throneZ = centerZ - width/2 + 2;

        // Create throne base
        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 0; z++) {
                world.setType(throneX + x, throneY + 1, throneZ + z, Material.POLISHED_BLACKSTONE);
                placedPositions.add(new Vector(throneX + x, throneY + 1, throneZ + z));

                // Second level is narrower
                if (Math.abs(x) <= 1) {
                    world.setType(throneX + x, throneY + 2, throneZ + z, Material.POLISHED_BLACKSTONE);
                    placedPositions.add(new Vector(throneX + x, throneY + 2, throneZ + z));
                }
            }
        }

        // Add throne chair
        world.setType(throneX, throneY + 3, throneZ, Material.GOLD_BLOCK);
        placedPositions.add(new Vector(throneX, throneY + 3, throneZ));

        // Add carpet leading to throne
        for (int z = 1; z < width/2 - 2; z++) {
            world.setType(throneX, throneY + 1, throneZ + z, Material.RED_CARPET);
            placedPositions.add(new Vector(throneX, throneY + 1, throneZ + z));
        }

        // Add decorative pillars along sides
        for (int x : new int[]{-width/3, width/3}) {
            for (int z = -width/3; z <= width/3; z += width/2) {
                for (int y = 1; y < height; y++) {
                    world.setType(centerX + x, centerY + y, centerZ + z, accentBlocks.get(0));
                    placedPositions.add(new Vector(centerX + x, centerY + y, centerZ + z));
                }
            }
        }
    }

    /**
     * Place a ritualistic room with an altar
     */
    private void placeRitualisticRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                      List<Material> primaryBlocks, List<Material> accentBlocks,
                                      List<Material> floorBlocks, List<Material> ceilingBlocks,
                                      Set<Vector> placedPositions) {
        // Place a circular room
        placeCircularRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        // Create a ritual circle in the center
        int circleRadius = width / 4;

        // Place ritual altar in center
        world.setType(centerX, centerY + 1, centerZ, Material.CHISELED_STONE_BRICKS);
        world.setType(centerX, centerY + 2, centerZ, Material.ENCHANTING_TABLE);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));
        placedPositions.add(new Vector(centerX, centerY + 2, centerZ));

        // Create ritual circle with candles
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int x = (int) (Math.cos(angle) * circleRadius);
            int z = (int) (Math.sin(angle) * circleRadius);

            int candleX = centerX + x;
            int candleY = centerY + 1;
            int candleZ = centerZ + z;

            world.setType(candleX, candleY, candleZ, Material.CANDLE);
            placedPositions.add(new Vector(candleX, candleY, candleZ));
        }

        // Create ritual symbols on floor (redstone dust patterns)
        for (int x = -circleRadius; x <= circleRadius; x++) {
            for (int z = -circleRadius; z <= circleRadius; z++) {
                double dist = Math.sqrt(x*x + z*z);

                // Create a circular pattern
                if (dist <= circleRadius && dist > circleRadius - 1 && random.nextBoolean()) {
                    world.setType(centerX + x, centerY + 1, centerZ + z, Material.REDSTONE_WIRE);
                    placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));
                }
            }
        }
    }

    /**
     * Place a sculk-infested room
     */
    private void placeSculkInfestedRoom(World world, int centerX, int centerY, int centerZ, int width, int height,
                                        List<Material> primaryBlocks, List<Material> accentBlocks,
                                        List<Material> floorBlocks, List<Material> ceilingBlocks,
                                        Set<Vector> placedPositions) {
        // First place a basic room
        placeSquareRoom(world, centerX, centerY, centerZ, width, height, primaryBlocks, accentBlocks,
                floorBlocks, ceilingBlocks, placedPositions);

        int halfWidth = width / 2;

        // Add sculk growth on walls and floor
        for (int x = -halfWidth + 1; x <= halfWidth - 1; x++) {
            for (int z = -halfWidth + 1; z <= halfWidth - 1; z++) {
                for (int y = 0; y < height; y++) {
                    int worldX = centerX + x;
                    int worldY = centerY + y;
                    int worldZ = centerZ + z;

                    Vector pos = new Vector(worldX, worldY, worldZ);
                    if (occupiedPositions.contains(pos)) continue;

                    // Higher chance of sculk on floor and walls
                    double chance = 0.1; // Default chance
                    if (y == 0) chance = 0.4; // Floor
                    else if (Math.abs(x) == halfWidth - 1 || Math.abs(z) == halfWidth - 1) chance = 0.3; // Walls

                    if (random.nextDouble() < chance) {
                        // Choose a sculk block type
                        Material[] sculkTypes = {
                                Material.SCULK, Material.SCULK_VEIN, Material.SCULK_CATALYST, Material.SCULK_SENSOR
                        };
                        Material sculkType = sculkTypes[random.nextInt(sculkTypes.length)];

                        world.setType(worldX, worldY, worldZ, sculkType);
                        placedPositions.add(pos);
                    }
                }
            }
        }

        // Add a sculk shrieker in the center
        world.setType(centerX, centerY + 1, centerZ, Material.SCULK_SHRIEKER);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));
    }

    /**
     * Add room decorations based on theme and room type
     */
    private void addRoomDecorations(World world, Location center, int roomWidth, int roomHeight,
                                    RoomType roomType, RoomVariant variant, Set<Vector> placedPositions) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        int radius = roomWidth / 2 - 1;

        // Light sources from theme
        List<Material> lightBlocks = theme.getLightBlocks();

        // Add light sources
        addLightSources(world, centerX, centerY, centerZ, roomWidth, roomHeight, lightBlocks, variant, placedPositions);

        // Add theme-specific decorations
        switch (theme.getName()) {
            case "PYRAMID":
                addPyramidDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "RUINS":
                addRuinsDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "WITCH_HUT":
                addWitchHutDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "UNDERWATER_RUINS":
                addUnderwaterDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "TEMPLE":
                addTempleDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "SCULK_DUNGEON":
                // Already handled in sculk-infested room generation
                break;

            case "DWARVEN_HALLS":
                addDwarvenDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            case "ICE_CASTLE":
                addIceDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;

            default:
                addGenericDecorations(world, centerX, centerY, centerZ, radius, placedPositions);
                break;
        }
    }

    /**
     * Add light sources to a room
     */
    private void addLightSources(World world, int centerX, int centerY, int centerZ, int width, int height,
                                 List<Material> lightBlocks, RoomVariant variant, Set<Vector> placedPositions) {
        int halfWidth = width / 2;

        // Choose light pattern based on variant
        switch (variant) {
            case CIRCULAR:
                // Evenly spaced lights around the perimeter
                int lightCount = 6 + random.nextInt(3); // 6-8 lights
                for (int i = 0; i < lightCount; i++) {
                    double angle = i * (2 * Math.PI / lightCount);
                    int x = (int) (Math.cos(angle) * (halfWidth - 1));
                    int z = (int) (Math.sin(angle) * (halfWidth - 1));

                    placeLightSource(world, centerX + x, centerY + height/2, centerZ + z, lightBlocks, placedPositions);
                }
                break;

            case PILLARED:
                // Lights on the pillars
                for (int xOffset : new int[]{-halfWidth + 2, halfWidth - 2}) {
                    for (int zOffset : new int[]{-halfWidth + 2, halfWidth - 2}) {
                        placeLightSource(world, centerX + xOffset, centerY + height/2, centerZ + zOffset, lightBlocks, placedPositions);
                    }
                }
                break;

            case TEMPLE:
                // Wall-mounted lights
                for (int x = -halfWidth + 1; x <= halfWidth - 1; x += halfWidth - 1) {
                    for (int z = -halfWidth + 2; z <= halfWidth - 2; z += 2) {
                        placeLightSource(world, centerX + x, centerY + 2, centerZ + z, lightBlocks, placedPositions);
                    }
                }
                for (int z = -halfWidth + 1; z <= halfWidth - 1; z += halfWidth - 1) {
                    for (int x = -halfWidth + 2; x <= halfWidth - 2; x += 2) {
                        placeLightSource(world, centerX + x, centerY + 2, centerZ + z, lightBlocks, placedPositions);
                    }
                }
                break;

            default:
                // Random lights
                int randomLightCount = 3 + random.nextInt(4); // 3-6 lights
                for (int i = 0; i < randomLightCount; i++) {
                    int x = random.nextInt(width - 2) - (width - 2)/2;
                    int z = random.nextInt(width - 2) - (width - 2)/2;
                    int y = random.nextBoolean() ? 2 : height - 2;

                    placeLightSource(world, centerX + x, centerY + y, centerZ + z, lightBlocks, placedPositions);
                }
                break;
        }
    }

    /**
     * Place a light source at the specified location
     */
    private void placeLightSource(World world, int x, int y, int z, List<Material> lightBlocks, Set<Vector> placedPositions) {
        Vector pos = new Vector(x, y, z);
        if (occupiedPositions.contains(pos)) return;

        Material lightType = lightBlocks.get(random.nextInt(lightBlocks.size()));
        world.setType(x, y, z, lightType);
        placedPositions.add(pos);
    }

    /**
     * Add decorations specific to pyramid theme
     */
    private void addPyramidDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add some decorative sandstone blocks
        for (int i = 0; i < radius * 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue; // Skip center area

            int y = 1 + random.nextInt(2);

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material decorMaterial;
            double rand = random.nextDouble();
            if (rand < 0.4) decorMaterial = Material.CHISELED_SANDSTONE;
            else if (rand < 0.7) decorMaterial = Material.CUT_SANDSTONE;
            else decorMaterial = Material.SANDSTONE_WALL;

            world.setType(centerX + x, centerY + y, centerZ + z, decorMaterial);
            placedPositions.add(pos);
        }

        // Add gold blocks for treasure theme
        for (int i = 0; i < 1 + random.nextInt(3); i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            Vector pos = new Vector(centerX + x, centerY + 1, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            world.setType(centerX + x, centerY + 1, centerZ + z, Material.GOLD_BLOCK);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to ruins theme
     */
    private void addRuinsDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add some collapsed parts (cobwebs, fallen blocks)
        for (int i = 0; i < radius * 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue; // Skip center area

            int y = random.nextBoolean() ? 0 : 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material decorMaterial;
            double rand = random.nextDouble();
            if (rand < 0.3) decorMaterial = Material.COBWEB;
            else if (rand < 0.6) decorMaterial = Material.MOSSY_COBBLESTONE;
            else if (rand < 0.8) decorMaterial = Material.GRAVEL;
            else decorMaterial = Material.CRACKED_STONE_BRICKS;

            world.setType(centerX + x, centerY + y, centerZ + z, decorMaterial);
            placedPositions.add(pos);
        }

        // Add some vegetation
        for (int i = 0; i < radius; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            Vector pos = new Vector(centerX + x, centerY + 1, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material plantMaterial;
            double rand = random.nextDouble();
            if (rand < 0.4) plantMaterial = Material.GRASS;
            else if (rand < 0.7) plantMaterial = Material.BROWN_MUSHROOM;
            else plantMaterial = Material.VINE;

            world.setType(centerX + x, centerY + 1, centerZ + z, plantMaterial);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to witch hut theme
     */
    private void addWitchHutDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add cauldron in center
        world.setType(centerX, centerY + 1, centerZ, Material.CAULDRON);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));

        // Add some potion brewing related items
        Material[] witchItems = {
                Material.BREWING_STAND, Material.FLOWER_POT, Material.COBWEB,
                Material.MUSHROOM_STEM, Material.RED_MUSHROOM, Material.SOUL_LANTERN
        };

        for (int i = 0; i < radius + 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            // Skip the center where the cauldron is
            if (x == 0 && z == 0) continue;

            int y = 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material witchItem = witchItems[random.nextInt(witchItems.length)];
            world.setType(centerX + x, centerY + y, centerZ + z, witchItem);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to underwater ruins theme
     */
    private void addUnderwaterDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add sea-themed decorations: seagrass, coral, etc.
        Material[] underwaterItems = {
                Material.SEAGRASS, Material.BRAIN_CORAL, Material.TUBE_CORAL,
                Material.BUBBLE_CORAL, Material.FIRE_CORAL, Material.HORN_CORAL,
                Material.SEA_LANTERN, Material.KELP
        };

        for (int i = 0; i < radius * 3; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue; // Skip center area

            int y = random.nextBoolean() ? 0 : 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material underwaterItem = underwaterItems[random.nextInt(underwaterItems.length)];
            world.setType(centerX + x, centerY + y, centerZ + z, underwaterItem);
            placedPositions.add(pos);
        }

        // Add some treasure chests
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            Vector pos = new Vector(centerX + x, centerY + 1, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            world.setType(centerX + x, centerY + 1, centerZ + z, Material.CHEST);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to temple theme
     */
    private void addTempleDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add an altar in the center
        world.setType(centerX, centerY + 1, centerZ, Material.CHISELED_STONE_BRICKS);
        world.setType(centerX, centerY + 2, centerZ, Material.END_PORTAL_FRAME);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));
        placedPositions.add(new Vector(centerX, centerY + 2, centerZ));

        // Add decorative blocks around
        for (int i = 0; i < radius + 3; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            // Skip the altar
            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;

            int y = 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material decorMaterial;
            double rand = random.nextDouble();
            if (rand < 0.3) decorMaterial = Material.CHISELED_STONE_BRICKS;
            else if (rand < 0.5) decorMaterial = Material.FLOWER_POT;
            else if (rand < 0.7) decorMaterial = Material.MOSSY_STONE_BRICKS;
            else decorMaterial = Material.GOLD_BLOCK;

            world.setType(centerX + x, centerY + y, centerZ + z, decorMaterial);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to dwarven halls theme
     */
    private void addDwarvenDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add anvils, smithing tables, etc.
        Material[] dwarvenItems = {
                Material.ANVIL, Material.SMITHING_TABLE, Material.BLAST_FURNACE,
                Material.LOOM, Material.BARREL, Material.GOLD_BLOCK, Material.IRON_BLOCK
        };

        for (int i = 0; i < radius + 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            int y = 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material dwarvenItem = dwarvenItems[random.nextInt(dwarvenItems.length)];
            world.setType(centerX + x, centerY + y, centerZ + z, dwarvenItem);
            placedPositions.add(pos);
        }
    }

    /**
     * Add decorations specific to ice castle theme
     */
    private void addIceDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add ice-themed decoration, snow layers, ice spikes
        for (int i = 0; i < radius * 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            int y = 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material iceMaterial;
            double rand = random.nextDouble();
            if (rand < 0.4) iceMaterial = Material.SNOW;
            else if (rand < 0.7) iceMaterial = Material.BLUE_ICE;
            else iceMaterial = Material.PACKED_ICE;

            world.setType(centerX + x, centerY + y, centerZ + z, iceMaterial);
            placedPositions.add(pos);
        }

        // Add some ice pillars
        for (int i = 0; i < 2 + random.nextInt(3); i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            int height = 2 + random.nextInt(3);
            for (int y = 1; y <= height; y++) {
                Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
                if (occupiedPositions.contains(pos)) continue;

                world.setType(centerX + x, centerY + y, centerZ + z, Material.PACKED_ICE);
                placedPositions.add(pos);
            }
        }
    }

    /**
     * Add generic decorations for undefined themes
     */
    private void addGenericDecorations(World world, int centerX, int centerY, int centerZ, int radius, Set<Vector> placedPositions) {
        // Add some random decoration blocks
        Material[] genericDecorations = {
                Material.FLOWER_POT, Material.COBWEB, Material.CHEST,
                Material.BARREL, Material.LANTERN, Material.CAMPFIRE
        };

        for (int i = 0; i < radius + 2; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;

            int y = 1;

            Vector pos = new Vector(centerX + x, centerY + y, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            Material decorMaterial = genericDecorations[random.nextInt(genericDecorations.length)];
            world.setType(centerX + x, centerY + y, centerZ + z, decorMaterial);
            placedPositions.add(pos);
        }
    }

    /**
     * Add specific features based on room type
     */
    private void addRoomFeatures(World world, int centerX, int centerY, int centerZ, int width, int height,
                                 RoomType roomType, RoomVariant variant, Set<Vector> placedPositions) {
        switch (roomType) {
            case ENTRANCE:
                addEntranceFeatures(world, centerX, centerY, centerZ, width, height, placedPositions);
                break;

            case TREASURE:
                addTreasureFeatures(world, centerX, centerY, centerZ, width, height, placedPositions);
                break;

            case TRAP:
                addTrapFeatures(world, centerX, centerY, centerZ, width, height, placedPositions);
                break;

            case BOSS:
                addBossFeatures(world, centerX, centerY, centerZ, width, height, placedPositions);
                break;

            default:
                // Normal rooms don't need special features
                break;
        }
    }

    /**
     * Add features specific to entrance rooms
     */
    private void addEntranceFeatures(World world, int centerX, int centerY, int centerZ, int width, int height, Set<Vector> placedPositions) {
        // Create a staircase leading up
        int stairHeight = 10;

        for (int y = 1; y <= stairHeight; y++) {
            int stairX = centerX;
            int stairY = centerY + y;
            int stairZ = centerZ - y;

            // Place stair blocks
            world.setType(stairX, stairY, stairZ, Material.STONE_STAIRS);
            placedPositions.add(new Vector(stairX, stairY, stairZ));

            // Clear air blocks above stairs
            world.setType(stairX, stairY + 1, stairZ, Material.AIR);
            world.setType(stairX, stairY + 2, stairZ, Material.AIR);
            placedPositions.add(new Vector(stairX, stairY + 1, stairZ));
            placedPositions.add(new Vector(stairX, stairY + 2, stairZ));

            // Place walls on sides
            world.setType(stairX - 1, stairY, stairZ, Material.STONE_BRICKS);
            world.setType(stairX + 1, stairY, stairZ, Material.STONE_BRICKS);
            placedPositions.add(new Vector(stairX - 1, stairY, stairZ));
            placedPositions.add(new Vector(stairX + 1, stairY, stairZ));

            // Place wall blocks above walls
            world.setType(stairX - 1, stairY + 1, stairZ, Material.STONE_BRICKS);
            world.setType(stairX + 1, stairY + 1, stairZ, Material.STONE_BRICKS);
            placedPositions.add(new Vector(stairX - 1, stairY + 1, stairZ));
            placedPositions.add(new Vector(stairX + 1, stairY + 1, stairZ));

            // Add lighting every few blocks
            if (y % 3 == 0) {
                world.setType(stairX - 1, stairY, stairZ, Material.LANTERN);
                world.setType(stairX + 1, stairY, stairZ, Material.LANTERN);
            }
        }

        // Add a sign to mark entrance
        world.setType(centerX, centerY + 1, centerZ, Material.OAK_SIGN);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));
    }

    /**
     * Add features specific to treasure rooms
     */
    private void addTreasureFeatures(World world, int centerX, int centerY, int centerZ, int width, int height, Set<Vector> placedPositions) {
        // Place a chest in the center
        world.setType(centerX, centerY + 1, centerZ, Material.CHEST);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));

        // Add decoration gold blocks around
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip the chest position

                if (random.nextDouble() < 0.7) {
                    world.setType(centerX + x, centerY + 1, centerZ + z, Material.GOLD_BLOCK);
                    placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));
                }
            }
        }

        // Add some extra chests around
        int extraChests = 1 + random.nextInt(3);
        for (int i = 0; i < extraChests; i++) {
            int x = random.nextInt(width - 2) - (width - 2) / 2;
            int z = random.nextInt(width - 2) - (width - 2) / 2;

            if (x == 0 && z == 0) continue; // Skip the center

            Vector pos = new Vector(centerX + x, centerY + 1, centerZ + z);
            if (occupiedPositions.contains(pos)) continue;

            world.setType(centerX + x, centerY + 1, centerZ + z, Material.CHEST);
            placedPositions.add(pos);
        }
    }

    /**
     * Add features specific to trap rooms
     */
    private void addTrapFeatures(World world, int centerX, int centerY, int centerZ, int width, int height, Set<Vector> placedPositions) {
        // Choose a trap type
        int trapType = random.nextInt(4);

        switch (trapType) {
            case 0:
                // Pressure plate on TNT
                placeTNTTrap(world, centerX, centerY, centerZ, width, placedPositions);
                break;

            case 1:
                // Pit trap
                placePitTrap(world, centerX, centerY, centerZ, width, placedPositions);
                break;

            case 2:
                // Arrow trap
                placeArrowTrap(world, centerX, centerY, centerZ, width, placedPositions);
                break;

            case 3:
                // Lava trap
                placeLavaTrap(world, centerX, centerY, centerZ, width, placedPositions);
                break;
        }
    }

    /**
     * Place a TNT trap with pressure plates
     */
    private void placeTNTTrap(World world, int centerX, int centerY, int centerZ, int width, Set<Vector> placedPositions) {
        int radius = width / 3;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Only place in a diamond pattern
                if (Math.abs(x) + Math.abs(z) > radius) continue;

                // Place TNT below
                world.setType(centerX + x, centerY, centerZ + z, Material.TNT);
                placedPositions.add(new Vector(centerX + x, centerY, centerZ + z));

                // Place pressure plates on top
                world.setType(centerX + x, centerY + 1, centerZ + z, Material.STONE_PRESSURE_PLATE);
                placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));
            }
        }
    }

    /**
     * Place a pit trap with hidden pressure plate
     */
    private void placePitTrap(World world, int centerX, int centerY, int centerZ, int width, Set<Vector> placedPositions) {
        // Only visual - actual trap mechanics would be handled by TrapHandler
        int radius = width / 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Only place in a circular pattern
                if (x*x + z*z > radius*radius) continue;

                // Place dirt (will be swapped for air when triggered)
                world.setType(centerX + x, centerY + 1, centerZ + z, Material.COARSE_DIRT);
                placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));

                // Place pressure plates on some blocks
                if (random.nextDouble() < 0.4) {
                    world.setType(centerX + x, centerY + 2, centerZ + z, Material.STONE_PRESSURE_PLATE);
                    placedPositions.add(new Vector(centerX + x, centerY + 2, centerZ + z));
                }
            }
        }
    }

    /**
     * Place an arrow trap with dispensers
     */
    private void placeArrowTrap(World world, int centerX, int centerY, int centerZ, int width, Set<Vector> placedPositions) {
        // Place dispensers in walls
        int halfWidth = width / 2;

        // North wall
        for (int x = -2; x <= 2; x++) {
            world.setType(centerX + x, centerY + 2, centerZ - halfWidth + 1, Material.DISPENSER);
            placedPositions.add(new Vector(centerX + x, centerY + 2, centerZ - halfWidth + 1));
        }

        // South wall
        for (int x = -2; x <= 2; x++) {
            world.setType(centerX + x, centerY + 2, centerZ + halfWidth - 1, Material.DISPENSER);
            placedPositions.add(new Vector(centerX + x, centerY + 2, centerZ + halfWidth - 1));
        }

        // East and west walls
        for (int z = -2; z <= 2; z++) {
            world.setType(centerX - halfWidth + 1, centerY + 2, centerZ + z, Material.DISPENSER);
            world.setType(centerX + halfWidth - 1, centerY + 2, centerZ + z, Material.DISPENSER);
            placedPositions.add(new Vector(centerX - halfWidth + 1, centerY + 2, centerZ + z));
            placedPositions.add(new Vector(centerX + halfWidth - 1, centerY + 2, centerZ + z));
        }

        // Add pressure plates in the center
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setType(centerX + x, centerY + 1, centerZ + z, Material.STONE_PRESSURE_PLATE);
                placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));
            }
        }
    }

    /**
     * Place a lava trap
     */
    private void placeLavaTrap(World world, int centerX, int centerY, int centerZ, int width, Set<Vector> placedPositions) {
        // Place lava under iron trapdoors
        int radius = width / 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Place in a circular pattern
                if (x*x + z*z > radius*radius) continue;

                // Place lava below
                world.setType(centerX + x, centerY, centerZ + z, Material.LAVA);
                placedPositions.add(new Vector(centerX + x, centerY, centerZ + z));

                // Cover with iron trapdoors
                world.setType(centerX + x, centerY + 1, centerZ + z, Material.IRON_TRAPDOOR);
                placedPositions.add(new Vector(centerX + x, centerY + 1, centerZ + z));

                // Add pressure plates on some of them
                if (random.nextDouble() < 0.3) {
                    world.setType(centerX + x, centerY + 2, centerZ + z, Material.STONE_PRESSURE_PLATE);
                    placedPositions.add(new Vector(centerX + x, centerY + 2, centerZ + z));
                }
            }
        }
    }

    /**
     * Add features specific to boss rooms
     */
    private void addBossFeatures(World world, int centerX, int centerY, int centerZ, int width, int height, Set<Vector> placedPositions) {
        // Place a spawner in center
        world.setType(centerX, centerY + 1, centerZ, Material.SPAWNER);
        placedPositions.add(new Vector(centerX, centerY + 1, centerZ));

        // Add challenging terrain features

        // Lava pools
        int lavaPoolCount = 2 + random.nextInt(3);
        for (int i = 0; i < lavaPoolCount; i++) {
            int poolX = centerX + random.nextInt(width - 4) - (width - 4) / 2;
            int poolZ = centerZ + random.nextInt(width - 4) - (width - 4) / 2;

            // Skip if too close to center
            if (Math.abs(poolX - centerX) < 3 && Math.abs(poolZ - centerZ) < 3) continue;

            world.setType(poolX, centerY, poolZ, Material.LAVA);
            placedPositions.add(new Vector(poolX, centerY, poolZ));

            // Add some lava around the center pool
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;

                    if (random.nextDouble() < 0.4) {
                        world.setType(poolX + x, centerY, poolZ + z, Material.LAVA);
                        placedPositions.add(new Vector(poolX + x, centerY, poolZ + z));
                    }
                }
            }
        }

        // Battle arena features - pillars to hide behind
        int pillarCount = 3 + random.nextInt(3);
        for (int i = 0; i < pillarCount; i++) {
            int pillarX = centerX + random.nextInt(width - 4) - (width - 4) / 2;
            int pillarZ = centerZ + random.nextInt(width - 4) - (width - 4) / 2;

            // Skip if too close to center
            if (Math.abs(pillarX - centerX) < 3 && Math.abs(pillarZ - centerZ) < 3) continue;

            int pillarHeight = 3 + random.nextInt(2);
            for (int y = 1; y <= pillarHeight; y++) {
                world.setType(pillarX, centerY + y, pillarZ, Material.OBSIDIAN);
                placedPositions.add(new Vector(pillarX, centerY + y, pillarZ));
            }
        }
    }
}