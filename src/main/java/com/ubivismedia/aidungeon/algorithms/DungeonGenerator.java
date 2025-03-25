package com.ubivismedia.aidungeon.algorithms;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.algorithms.cellular.CellularAutomata;
import com.ubivismedia.aidungeon.algorithms.genetic.GeneticOptimizer;
import com.ubivismedia.aidungeon.algorithms.markov.MarkovChainModel;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;
import com.ubivismedia.aidungeon.dungeons.RoomType;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

/**
 * Core class that combines different algorithms to generate dungeons
 */
public class DungeonGenerator {
    
    private final AIDungeonGenerator plugin;
    private final CellularAutomata roomGenerator;
    private final MarkovChainModel themeModel;
    private final GeneticOptimizer layoutOptimizer;
    private final Random random;
    
    public DungeonGenerator(AIDungeonGenerator plugin, 
                            CellularAutomata roomGenerator,
                            MarkovChainModel themeModel,
                            GeneticOptimizer layoutOptimizer) {
        this.plugin = plugin;
        this.roomGenerator = roomGenerator;
        this.themeModel = themeModel;
        this.layoutOptimizer = layoutOptimizer;
        this.random = new Random();
    }
    
    /**
     * Generate a dungeon synchronously (called from main thread)
     */
    public DungeonLayout generateDungeon(BiomeArea area) {
        // Get dungeon size from config based on random chance
        int dungeonSize = getDungeonSize();
        
        // Get theme based on biome
        DungeonTheme theme = plugin.getConfigManager().getThemeForBiome(area.getPrimaryBiome());
        
        // Create initial empty layout
        DungeonLayout layout = new DungeonLayout(dungeonSize, theme);
        
        // Set entrance room
        layout.setEntrancePosition(dungeonSize / 2, dungeonSize / 2);
        
        // Apply room generation
        roomGenerator.applyTo(layout);
        
        // Apply room type transitions using Markov Chain
        themeModel.applyThemeTransitions(layout);
        
        // Optimize layout using genetic algorithm
        int generations = plugin.getConfig().getInt("generation.algorithm.optimization-generations", 10);
        layoutOptimizer.optimizeLayout(layout, generations);
        
        // Add final decorative elements and features
        addFeatures(layout);
        
        return layout;
    }
    
    /**
     * Generate a dungeon asynchronously (safe to call from async thread)
     */
    public DungeonLayout generateDungeonAsync(BiomeArea area) {
        // This method is identical to the synchronous one for now
        // In a real implementation, you might want to add progress callbacks or chunking
        return generateDungeon(area);
    }
    
    /**
     * Get random dungeon size from config
     */
    private int getDungeonSize() {
        ConfigurationSection sizeSection = plugin.getConfig().getConfigurationSection("generation.algorithm.dungeon-size");
        
        // Default sizes if config is missing
        int small = 25;
        int medium = 40;
        int large = 60;
        
        if (sizeSection != null) {
            small = sizeSection.getInt("small", small);
            medium = sizeSection.getInt("medium", medium);
            large = sizeSection.getInt("large", large);
        }
        
        // Randomly choose size based on probabilities (50% small, 30% medium, 20% large)
        double rand = random.nextDouble();
        
        if (rand < 0.5) {
            return small;
        } else if (rand < 0.8) {
            return medium;
        } else {
            return large;
        }
    }
    
    /**
     * Add features to the dungeon layout
     */
    private void addFeatures(DungeonLayout layout) {
        // Calculate number of treasure rooms based on dungeon size
        int size = layout.getSize();
        int treasureRooms = Math.max(1, size / 15);
        int trapRooms = Math.max(2, size / 10);
        
        // Place boss room (furthest from entrance)
        RoomPosition furthest = findFurthestRoom(layout);
        if (furthest != null) {
            layout.setRoomType(furthest.x, furthest.y, RoomType.BOSS);
        }
        
        // Place treasure rooms
        int treasuresPlaced = 0;
        for (int i = 0; i < 100 && treasuresPlaced < treasureRooms; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            
            if (layout.isRoom(x, y) && layout.getRoomType(x, y) == RoomType.NORMAL) {
                layout.setRoomType(x, y, RoomType.TREASURE);
                treasuresPlaced++;
            }
        }
        
        // Place trap rooms (not too close to entrance)
        int trapsPlaced = 0;
        for (int i = 0; i < 100 && trapsPlaced < trapRooms; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            
            if (layout.isRoom(x, y) && layout.getRoomType(x, y) == RoomType.NORMAL) {
                // Check distance from entrance
                int entranceX = layout.getEntranceX();
                int entranceY = layout.getEntranceY();
                double distance = Math.sqrt(Math.pow(x - entranceX, 2) + Math.pow(y - entranceY, 2));
                
                if (distance > size / 5) {
                    layout.setRoomType(x, y, RoomType.TRAP);
                    trapsPlaced++;
                }
            }
        }
    }
    
    /**
     * Find the room furthest from the entrance
     */
    private RoomPosition findFurthestRoom(DungeonLayout layout) {
        int entranceX = layout.getEntranceX();
        int entranceY = layout.getEntranceY();
        int size = layout.getSize();
        
        RoomPosition furthest = null;
        double maxDistance = 0;
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (layout.isRoom(x, y) && layout.getRoomType(x, y) == RoomType.NORMAL) {
                    double distance = Math.sqrt(Math.pow(x - entranceX, 2) + Math.pow(y - entranceY, 2));
                    
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        furthest = new RoomPosition(x, y);
                    }
                }
            }
        }
        
        return furthest;
    }
    
    /**
     * Helper class for room positions
     */
    private static class RoomPosition {
        final int x;
        final int y;
        
        RoomPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}