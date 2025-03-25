package com.yourdomain.aidungeon.algorithms.markov;

import com.yourdomain.aidungeon.AIDungeonGenerator;
import com.yourdomain.aidungeon.dungeons.DungeonLayout;
import com.yourdomain.aidungeon.dungeons.RoomType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implements a Markov Chain model for room type transitions
 * to create more coherent and natural-feeling dungeons
 */
public class MarkovChainModel {
    
    private final AIDungeonGenerator plugin;
    private final Random random = new Random();
    
    // Transition probabilities for room types
    private final Map<RoomType, Map<RoomType, Double>> transitionProbabilities;
    
    /**
     * Create a new Markov Chain model
     */
    public MarkovChainModel(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        this.transitionProbabilities = new HashMap<>();
        
        // Initialize transition probabilities
        initializeTransitionProbabilities();
    }
    
    /**
     * Initialize the transition probability matrix with default values
     */
    private void initializeTransitionProbabilities() {
        // For each source room type
        for (RoomType sourceType : RoomType.values()) {
            Map<RoomType, Double> transitions = new HashMap<>();
            transitionProbabilities.put(sourceType, transitions);
            
            // For each destination room type
            for (RoomType destType : RoomType.values()) {
                // Set default probabilities
                double probability = 0.0;
                
                if (sourceType == RoomType.EMPTY) {
                    // Empty cells should stay empty
                    probability = (destType == RoomType.EMPTY) ? 1.0 : 0.0;
                } else if (sourceType == RoomType.ENTRANCE) {
                    // Entrance should connect to normal rooms
                    probability = (destType == RoomType.NORMAL) ? 0.9 : 0.1;
                } else if (sourceType == RoomType.NORMAL) {
                    // Normal rooms can connect to various types
                    switch (destType) {
                        case NORMAL:
                            probability = 0.7;  // Most likely another normal room
                            break;
                        case TREASURE:
                            probability = 0.1;  // Sometimes treasure
                            break;
                        case TRAP:
                            probability = 0.15; // Sometimes traps
                            break;
                        case BOSS:
                            probability = 0.05; // Rarely boss rooms
                            break;
                        default:
                            probability = 0.0;  // Never others
                    }
                } else if (sourceType.isSpecial()) {
                    // Special rooms mostly connect back to normal rooms
                    probability = (destType == RoomType.NORMAL) ? 0.9 : 0.1;
                }
                
                transitions.put(destType, probability);
            }
        }
    }
    
    /**
     * Apply the Markov Chain model to a dungeon layout
     */
    public void applyThemeTransitions(DungeonLayout layout) {
        int size = layout.getSize();
        
        // Create a copy of the current layout to sample from
        RoomType[][] originalLayout = new RoomType[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                originalLayout[x][y] = layout.getRoomType(x, y);
            }
        }
        
        // Apply transitions to normal rooms based on neighbors
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Only consider transforming normal rooms
                if (originalLayout[x][y] == RoomType.NORMAL) {
                    transformRoom(layout, originalLayout, x, y);
                }
            }
        }
        
        // Ensure we have at least one of each special room type
        ensureSpecialRoomTypes(layout);
    }
    
    /**
     * Transform a room based on Markov transitions
     */
    private void transformRoom(DungeonLayout layout, RoomType[][] originalLayout, int x, int y) {
        int size = layout.getSize();
        
        // Get the most common neighboring room type
        Map<RoomType, Integer> neighborCounts = new HashMap<>();
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip self
                
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < size && ny >= 0 && ny < size) {
                    RoomType neighborType = originalLayout[nx][ny];
                    neighborCounts.put(neighborType, neighborCounts.getOrDefault(neighborType, 0) + 1);
                }
            }
        }
        
        // Find the most common neighbor
        RoomType mostCommonNeighbor = RoomType.NORMAL; // Default
        int maxCount = 0;
        
        for (Map.Entry<RoomType, Integer> entry : neighborCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommonNeighbor = entry.getKey();
            }
        }
        
        // Get transition probabilities from this neighbor type
        Map<RoomType, Double> transitions = transitionProbabilities.get(mostCommonNeighbor);
        
        // Select a new room type based on transitions
        double rand = random.nextDouble();
        double cumulativeProbability = 0.0;
        
        for (Map.Entry<RoomType, Double> entry : transitions.entrySet()) {
            cumulativeProbability += entry.getValue();
            
            if (rand <= cumulativeProbability) {
                RoomType newType = entry.getKey();
                
                // Don't change to EMPTY (we want to keep rooms)
                if (newType != RoomType.EMPTY) {
                    layout.setRoomType(x, y, newType);
                }
                break;
            }
        }
    }
    
    /**
     * Ensure we have at least one of each special room type
     */
    private void ensureSpecialRoomTypes(DungeonLayout layout) {
        // Check for boss room
        if (layout.getRoomPositions(RoomType.BOSS).isEmpty()) {
            // Find a room far from entrance to set as boss room
            setFarthestRoomAsType(layout, RoomType.BOSS);
        }
        
        // Check for treasure rooms
        if (layout.getRoomPositions(RoomType.TREASURE).isEmpty()) {
            // Set some random rooms as treasure
            int treasureCount = Math.max(1, layout.getSize() / 10);
            setRandomRoomsAsType(layout, RoomType.TREASURE, treasureCount);
        }
        
        // Check for trap rooms
        if (layout.getRoomPositions(RoomType.TRAP).isEmpty()) {
            // Set some random rooms as traps
            int trapCount = Math.max(2, layout.getSize() / 8);
            setRandomRoomsAsType(layout, RoomType.TRAP, trapCount);
        }
    }
    
    /**
     * Set the farthest room from entrance as a specific type
     */
    private void setFarthestRoomAsType(DungeonLayout layout, RoomType type) {
        int size = layout.getSize();
        int entranceX = layout.getEntranceX();
        int entranceY = layout.getEntranceY();
        
        int farthestX = -1;
        int farthestY = -1;
        double maxDistance = 0;
        
        // Find the farthest normal room
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (layout.getRoomType(x, y) == RoomType.NORMAL) {
                    double distance = Math.sqrt(Math.pow(x - entranceX, 2) + Math.pow(y - entranceY, 2));
                    
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        farthestX = x;
                        farthestY = y;
                    }
                }
            }
        }
        
        // Set the farthest room as the specified type
        if (farthestX != -1) {
            layout.setRoomType(farthestX, farthestY, type);
        }
    }
    
    /**
     * Set random normal rooms as a specific type
     */
    private void setRandomRoomsAsType(DungeonLayout layout, RoomType type, int count) {
        int size = layout.getSize();
        int remaining = count;
        
        // Try up to 100 times to place rooms
        for (int attempt = 0; attempt < 100 && remaining > 0; attempt++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            
            if (layout.getRoomType(x, y) == RoomType.NORMAL) {
                layout.setRoomType(x, y, type);
                remaining--;
            }
        }
    }
}
