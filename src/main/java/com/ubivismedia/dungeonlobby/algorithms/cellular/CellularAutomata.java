package com.ubivismedia.aidungeon.algorithms.cellular;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;
import com.ubivismedia.aidungeon.dungeons.RoomType;

import java.util.Random;

/**
 * Implements a cellular automata algorithm for room generation.
 * This is based on Conway's Game of Life with modifications
 * for dungeon generation.
 */
public class CellularAutomata {
    
    private final AIDungeonGenerator plugin;
    private final Random random = new Random();
    
    // Configuration
    private final int initialFillPercent;
    private final int iterations;
    private final int birthLimit;
    private final int deathLimit;
    
    /**
     * Create a new cellular automata generator
     */
    public CellularAutomata(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        
        // Load configuration
        this.initialFillPercent = 45; // Default 45%
        this.iterations = 4;          // Default 4 iterations
        this.birthLimit = 4;          // Default birth at 4+ neighbors
        this.deathLimit = 3;          // Default death at <3 neighbors
    }
    
    /**
     * Apply the cellular automata algorithm to a dungeon layout
     */
    public void applyTo(DungeonLayout layout) {
        int size = layout.getSize();
        boolean[][] map = new boolean[size][size];
        
        // Initialize with random fill
        initializeRandomMap(map, size);
        
        // Run cellular automata iterations
        for (int i = 0; i < iterations; i++) {
            map = doSimulationStep(map, size);
        }
        
        // Convert the resulting map to dungeon rooms
        applyMapToLayout(map, layout);
        
        // Fix disconnected rooms
        connectRooms(layout);
    }
    
    /**
     * Initialize the map with random fill
     */
    private void initializeRandomMap(boolean[][] map, int size) {
        // Keep entrance position clear
        int midPoint = size / 2;
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Skip the center area (entrance)
                if (Math.abs(x - midPoint) <= 2 && Math.abs(y - midPoint) <= 2) {
                    map[x][y] = true; // Always make rooms near entrance
                } else {
                    // Randomly decide if this is a room
                    map[x][y] = random.nextInt(100) < initialFillPercent;
                }
            }
        }
    }
    
    /**
     * Run one step of the simulation
     */
    private boolean[][] doSimulationStep(boolean[][] oldMap, int size) {
        boolean[][] newMap = new boolean[size][size];
        
        // Evaluate each cell in the grid
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Count neighbors (including diagonals)
                int neighbors = countNeighbors(oldMap, x, y, size);
                
                // Apply cellular automata rules
                if (oldMap[x][y]) {
                    // Currently a room
                    newMap[x][y] = neighbors >= deathLimit;
                } else {
                    // Currently empty
                    newMap[x][y] = neighbors > birthLimit;
                }
            }
        }
        
        return newMap;
    }
    
    /**
     * Count the number of neighbors around a cell
     */
    private int countNeighbors(boolean[][] map, int x, int y, int size) {
        int count = 0;
        
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                // Skip the cell itself
                if (i == 0 && j == 0) continue;
                
                int nx = x + i;
                int ny = y + j;
                
                // Count edge cells as filled
                if (nx < 0 || ny < 0 || nx >= size || ny >= size) {
                    count++;
                } else if (map[nx][ny]) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Apply the boolean map to the dungeon layout
     */
    private void applyMapToLayout(boolean[][] map, DungeonLayout layout) {
        int size = layout.getSize();
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Skip entrance which is already set
                if (layout.getRoomType(x, y) == RoomType.ENTRANCE) {
                    continue;
                }
                
                // Apply map to layout
                if (map[x][y]) {
                    layout.setRoomType(x, y, RoomType.NORMAL);
                } else {
                    layout.setRoomType(x, y, RoomType.EMPTY);
                }
            }
        }
    }
    
    /**
     * Connect disconnected rooms to ensure the dungeon is fully navigable
     */
    private void connectRooms(DungeonLayout layout) {
        int size = layout.getSize();
        
        // Find the entrance
        int entranceX = layout.getEntranceX();
        int entranceY = layout.getEntranceY();
        
        // Simple flood fill from entrance to mark connected rooms
        boolean[][] connected = new boolean[size][size];
        floodFill(layout, entranceX, entranceY, connected);
        
        // Find disconnected rooms and connect them
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (layout.isRoom(x, y) && !connected[x][y]) {
                    connectRoomToNearestConnected(layout, x, y, connected);
                }
            }
        }
    }
    
    /**
     * Flood fill from a starting position to mark connected rooms
     */
    private void floodFill(DungeonLayout layout, int x, int y, boolean[][] connected) {
        // Boundary check
        if (x < 0 || y < 0 || x >= layout.getSize() || y >= layout.getSize()) {
            return;
        }
        
        // If not a room or already connected, skip
        if (!layout.isRoom(x, y) || connected[x][y]) {
            return;
        }
        
        // Mark as connected
        connected[x][y] = true;
        
        // Recursively check neighbors
        floodFill(layout, x + 1, y, connected);
        floodFill(layout, x - 1, y, connected);
        floodFill(layout, x, y + 1, connected);
        floodFill(layout, x, y - 1, connected);
    }
    
    /**
     * Connect a disconnected room to the nearest connected room
     */
    private void connectRoomToNearestConnected(DungeonLayout layout, int roomX, int roomY, boolean[][] connected) {
        int size = layout.getSize();
        int nearestX = -1;
        int nearestY = -1;
        int minDistance = Integer.MAX_VALUE;
        
        // Find the nearest connected room
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (connected[x][y]) {
                    int distance = Math.abs(x - roomX) + Math.abs(y - roomY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestX = x;
                        nearestY = y;
                    }
                }
            }
        }
        
        // If found a nearest connected room, create a corridor
        if (nearestX != -1) {
            createCorridor(layout, roomX, roomY, nearestX, nearestY);
            
            // Mark the new corridor and room as connected
            connected[roomX][roomY] = true;
            
            // Flood fill from the newly connected room to mark any other rooms it connects to
            floodFill(layout, roomX, roomY, connected);
        }
    }
    
    /**
     * Create a corridor between two rooms
     */
    private void createCorridor(DungeonLayout layout, int x1, int y1, int x2, int y2) {
        // Create an L-shaped corridor
        int currentX = x1;
        int currentY = y1;
        
        // Decide which direction to go first (horizontal or vertical)
        boolean horizontalFirst = random.nextBoolean();
        
        if (horizontalFirst) {
            // Go horizontal then vertical
            while (currentX != x2) {
                currentX += (currentX < x2) ? 1 : -1;
                layout.setRoomType(currentX, currentY, RoomType.NORMAL);
            }
            
            while (currentY != y2) {
                currentY += (currentY < y2) ? 1 : -1;
                layout.setRoomType(currentX, currentY, RoomType.NORMAL);
            }
        } else {
            // Go vertical then horizontal
            while (currentY != y2) {
                currentY += (currentY < y2) ? 1 : -1;
                layout.setRoomType(currentX, currentY, RoomType.NORMAL);
            }
            
            while (currentX != x2) {
                currentX += (currentX < x2) ? 1 : -1;
                layout.setRoomType(currentX, currentY, RoomType.NORMAL);
            }
        }
    }
