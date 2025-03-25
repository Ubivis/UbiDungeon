package com.ubivismedia.aidungeon.algorithms.genetic;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;
import com.ubivismedia.aidungeon.dungeons.RoomType;

import java.util.*;

/**
 * Implements a genetic algorithm to optimize dungeon layouts
 * for better playability, aesthetics, and challenge balance
 */
public class GeneticOptimizer {
    
    private final AIDungeonGenerator plugin;
    private final Random random = new Random();
    
    // Genetic algorithm parameters
    private final int populationSize = 10;
    private final double mutationRate = 0.2;
    private final double crossoverRate = 0.7;
    
    public GeneticOptimizer(AIDungeonGenerator plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Optimize a dungeon layout using genetic algorithm
     * @param layout The layout to optimize
     * @param generations Number of generations to run
     */
    public void optimizeLayout(DungeonLayout layout, int generations) {
        int size = layout.getSize();
        
        // Create initial population based on the input layout
        List<Individual> population = createInitialPopulation(layout);
        
        // Run the genetic algorithm for specified generations
        for (int generation = 0; generation < generations; generation++) {
            // Evaluate fitness for each individual
            for (Individual individual : population) {
                individual.calculateFitness();
            }
            
            // Sort by fitness (descending)
            population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
            
            // If this is the last generation, apply the best individual to the layout
            if (generation == generations - 1) {
                Individual best = population.get(0);
                applyIndividualToLayout(best, layout);
                break;
            }
            
            // Create new population
            List<Individual> newPopulation = new ArrayList<>();
            
            // Elitism: keep top 20% of individuals
            int eliteCount = populationSize / 5;
            for (int i = 0; i < eliteCount; i++) {
                newPopulation.add(population.get(i));
            }
            
            // Fill the rest with crossover and mutation
            while (newPopulation.size() < populationSize) {
                // Select parents
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);
                
                // Crossover
                Individual child;
                if (random.nextDouble() < crossoverRate) {
                    child = crossover(parent1, parent2);
                } else {
                    // No crossover, just clone parent1
                    child = parent1.clone();
                }
                
                // Mutation
                if (random.nextDouble() < mutationRate) {
                    mutate(child);
                }
                
                newPopulation.add(child);
            }
            
            // Replace old population
            population = newPopulation;
        }
    }
    
    /**
     * Create initial population based on the input layout
     */
    private List<Individual> createInitialPopulation(DungeonLayout layout) {
        List<Individual> population = new ArrayList<>();
        
        // First individual is the original layout
        population.add(new Individual(layout));
        
        // The rest are variations
        for (int i = 1; i < populationSize; i++) {
            Individual individual = new Individual(layout);
            mutate(individual); // Apply more mutations to create diversity
            mutate(individual);
            population.add(individual);
        }
        
        return population;
    }
    
    /**
     * Select a parent using tournament selection
     */
    private Individual selectParent(List<Individual> population) {
        // Tournament size
        int tournamentSize = 3;
        
        // Select random individuals for tournament
        List<Individual> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = random.nextInt(population.size());
            tournament.add(population.get(randomIndex));
        }
        
        // Return the best from tournament
        return Collections.max(tournament, Comparator.comparingDouble(a -> a.fitness));
    }
    
    /**
     * Perform crossover between two parents to create a child
     */
    private Individual crossover(Individual parent1, Individual parent2) {
        Individual child = new Individual(parent1.size);
        
        // Pick a random crossover point
        int crossoverPoint = random.nextInt(parent1.size);
        
        // Copy from parent1 up to crossover point
        for (int x = 0; x < crossoverPoint; x++) {
            for (int y = 0; y < parent1.size; y++) {
                child.grid[x][y] = parent1.grid[x][y];
            }
        }
        
        // Copy from parent2 after crossover point
        for (int x = crossoverPoint; x < parent1.size; x++) {
            for (int y = 0; y < parent1.size; y++) {
                child.grid[x][y] = parent2.grid[x][y];
            }
        }
        
        // Special rooms handling
        child.entranceX = parent1.entranceX;
        child.entranceY = parent1.entranceY;
        
        return child;
    }
    
    /**
     * Mutate an individual by making random changes
     */
    private void mutate(Individual individual) {
        int size = individual.size;
        
        // Number of mutations based on dungeon size
        int mutations = size / 5;
        
        for (int i = 0; i < mutations; i++) {
            int x = random.nextInt(size);
            int y = random.nextInt(size);
            
            // Don't mutate the entrance
            if (x == individual.entranceX && y == individual.entranceY) {
                continue;
            }
            
            // Mutation types:
            int mutationType = random.nextInt(3);
            
            switch (mutationType) {
                case 0:
                    // Change room to empty or vice versa
                    if (individual.grid[x][y] == RoomType.EMPTY) {
                        individual.grid[x][y] = RoomType.NORMAL;
                    } else if (individual.grid[x][y] == RoomType.NORMAL) {
                        individual.grid[x][y] = RoomType.EMPTY;
                    }
                    break;
                case 1:
                    // Change room type
                    if (individual.grid[x][y] != RoomType.EMPTY && 
                        individual.grid[x][y] != RoomType.ENTRANCE) {
                        
                        RoomType[] possibleTypes = {
                            RoomType.NORMAL, RoomType.TREASURE, RoomType.TRAP
                        };
                        individual.grid[x][y] = possibleTypes[random.nextInt(possibleTypes.length)];
                    }
                    break;
                case 2:
                    // Connect to nearest room if this is an isolated room
                    if (individual.grid[x][y] != RoomType.EMPTY && isIsolated(individual, x, y)) {
                        connectToNearestRoom(individual, x, y);
                    }
                    break;
            }
        }
    }
    
    /**
     * Check if a room is isolated (has no adjacent rooms)
     */
    private boolean isIsolated(Individual individual, int x, int y) {
        int size = individual.size;
        
        // Check all four directions
        if (x > 0 && individual.grid[x-1][y] != RoomType.EMPTY) return false;
        if (x < size-1 && individual.grid[x+1][y] != RoomType.EMPTY) return false;
        if (y > 0 && individual.grid[x][y-1] != RoomType.EMPTY) return false;
        if (y < size-1 && individual.grid[x][y+1] != RoomType.EMPTY) return false;
        
        return true;
    }
    
    /**
     * Connect an isolated room to the nearest non-empty room
     */
    private void connectToNearestRoom(Individual individual, int roomX, int roomY) {
        int size = individual.size;
        int nearestX = -1;
        int nearestY = -1;
        int minDistance = Integer.MAX_VALUE;
        
        // Find the nearest non-empty room
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if ((x != roomX || y != roomY) && individual.grid[x][y] != RoomType.EMPTY) {
                    int distance = Math.abs(x - roomX) + Math.abs(y - roomY);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestX = x;
                        nearestY = y;
                    }
                }
            }
        }
        
        // If found a nearest room, create a corridor
        if (nearestX != -1) {
            createCorridor(individual, roomX, roomY, nearestX, nearestY);
        }
    }
    
    /**
     * Create a corridor between two rooms
     */
    private void createCorridor(Individual individual, int x1, int y1, int x2, int y2) {
        // Create an L-shaped corridor
        int currentX = x1;
        int currentY = y1;
        
        // Decide which direction to go first (horizontal or vertical)
        boolean horizontalFirst = random.nextBoolean();
        
        if (horizontalFirst) {
            // Go horizontal then vertical
            while (currentX != x2) {
                currentX += (currentX < x2) ? 1 : -1;
                individual.grid[currentX][currentY] = RoomType.NORMAL;
            }
            
            while (currentY != y2) {
                currentY += (currentY < y2) ? 1 : -1;
                individual.grid[currentX][currentY] = RoomType.NORMAL;
            }
        } else {
            // Go vertical then horizontal
            while (currentY != y2) {
                currentY += (currentY < y2) ? 1 : -1;
                individual.grid[currentX][currentY] = RoomType.NORMAL;
            }
            
            while (currentX != x2) {
                currentX += (currentX < x2) ? 1 : -1;
                individual.grid[currentX][currentY] = RoomType.NORMAL;
            }
        }
    }
    
    /**
     * Apply the optimized individual back to the dungeon layout
     */
    private void applyIndividualToLayout(Individual individual, DungeonLayout layout) {
        int size = layout.getSize();
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // Skip the entrance which should already be set
                if (x == individual.entranceX && y == individual.entranceY) {
                    continue;
                }
                
                layout.setRoomType(x, y, individual.grid[x][y]);
            }
        }
    }
    
    /**
     * Inner class representing an individual in the genetic algorithm
     */
    private class Individual {
        final int size;
        final RoomType[][] grid;
        int entranceX;
        int entranceY;
        double fitness;
        
        /**
         * Create a new individual based on an existing layout
         */
        Individual(DungeonLayout layout) {
            this.size = layout.getSize();
            this.grid = new RoomType[size][size];
            this.entranceX = layout.getEntranceX();
            this.entranceY = layout.getEntranceY();
            
            // Copy layout grid
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    grid[x][y] = layout.getRoomType(x, y);
                }
            }
        }
        
        /**
         * Create a new individual with empty grid
         */
        Individual(int size) {
            this.size = size;
            this.grid = new RoomType[size][size];
            
            // Initialize with empty cells
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    grid[x][y] = RoomType.EMPTY;
                }
            }
        }
        
        /**
         * Calculate fitness score for this individual
         */
        void calculateFitness() {
            double score = 0.0;
            
            // 1. Connectivity - reward well-connected layouts
            score += evaluateConnectivity() * 0.4;
            
            // 2. Room distribution - reward good room type distribution
            score += evaluateRoomDistribution() * 0.3;
            
            // 3. Aesthetics - reward interesting shapes
            score += evaluateAesthetics() * 0.2;
            
            // 4. Challenge balance - reward good difficulty progression
            score += evaluateChallenge() * 0.1;
            
            this.fitness = score;
        }
        
        /**
         * Evaluate connectivity of the dungeon
         * (how well rooms are connected)
         */
        private double evaluateConnectivity() {
            // Count accessible rooms using flood fill
            boolean[][] visited = new boolean[size][size];
            int accessibleRooms = floodFill(entranceX, entranceY, visited);
            
            // Count total rooms
            int totalRooms = 0;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (grid[x][y] != RoomType.EMPTY) {
                        totalRooms++;
                    }
                }
            }
            
            // If no rooms, return 0
            if (totalRooms == 0) return 0;
            
            // Return percentage of accessible rooms
            return (double) accessibleRooms / totalRooms;
        }
        
        /**
         * Flood fill to count accessible rooms from entrance
         */
        private int floodFill(int x, int y, boolean[][] visited) {
            // Check boundaries
            if (x < 0 || x >= size || y < 0 || y >= size) {
                return 0;
            }
            
            // Skip if already visited or empty
            if (visited[x][y] || grid[x][y] == RoomType.EMPTY) {
                return 0;
            }
            
            // Mark as visited
            visited[x][y] = true;
            
            // Recursively check neighbors and count
            return 1 + floodFill(x+1, y, visited) +
                   floodFill(x-1, y, visited) +
                   floodFill(x, y+1, visited) +
                   floodFill(x, y-1, visited);
        }
        
        /**
         * Evaluate room type distribution
         */
        private double evaluateRoomDistribution() {
            int normalRooms = 0;
            int treasureRooms = 0;
            int trapRooms = 0;
            int bossRooms = 0;
            
            // Count room types
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    switch (grid[x][y]) {
                        case NORMAL:
                            normalRooms++;
                            break;
                        case TREASURE:
                            treasureRooms++;
                            break;
                        case TRAP:
                            trapRooms++;
                            break;
                        case BOSS:
                            bossRooms++;
                            break;
                    }
                }
            }
            
            // Calculate total non-empty rooms
            int totalRooms = normalRooms + treasureRooms + trapRooms + bossRooms + 1; // +1 for entrance
            
            // If no rooms, return 0
            if (totalRooms <= 1) return 0;
            
            // Calculate ideal distributions
            int idealTreasureRooms = Math.max(1, totalRooms / 10);
            int idealTrapRooms = Math.max(1, totalRooms / 8);
            int idealBossRooms = 1;
            
            // Calculate score based on how close to ideal
            double treasureScore = 1.0 - Math.abs(treasureRooms - idealTreasureRooms) / (double)idealTreasureRooms;
            double trapScore = 1.0 - Math.abs(trapRooms - idealTrapRooms) / (double)idealTrapRooms;
            double bossScore = (bossRooms == idealBossRooms) ? 1.0 : 0.0;
            
            // Weight and combine scores
            return (treasureScore * 0.4 + trapScore * 0.3 + bossScore * 0.3);
        }
        
        /**
         * Evaluate aesthetic qualities of the dungeon
         */
        private double evaluateAesthetics() {
            // Count number of different room shapes
            int shapes = countRoomShapes();
            
            // Calculate ideal number of shapes based on size
            int idealShapes = size / 5;
            
            // Calculate score based on how close to ideal
            double shapeScore = 1.0 - Math.abs(shapes - idealShapes) / (double)idealShapes;
            if (shapeScore < 0) shapeScore = 0;
            
            return shapeScore;
        }
        
        /**
         * Count different room shapes in the dungeon
         */
        private int countRoomShapes() {
            Set<String> shapes = new HashSet<>();
            
            // Look for room clusters
            boolean[][] visited = new boolean[size][size];
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (!visited[x][y] && grid[x][y] != RoomType.EMPTY) {
                        // Find this room cluster and get its shape signature
                        StringBuilder signature = new StringBuilder();
                        findRoomCluster(x, y, visited, 0, 0, signature);
                        shapes.add(signature.toString());
                    }
                }
            }
            
            return shapes.size();
        }
        
        /**
         * Recursively find a room cluster and build its shape signature
         */
        private void findRoomCluster(int x, int y, boolean[][] visited, int relX, int relY, StringBuilder signature) {
            // Check boundaries
            if (x < 0 || x >= size || y < 0 || y >= size) {
                return;
            }
            
            // Skip if already visited or empty
            if (visited[x][y] || grid[x][y] == RoomType.EMPTY) {
                return;
            }
            
            // Mark as visited
            visited[x][y] = true;
            
            // Add to signature
            signature.append(relX).append(",").append(relY).append(";");
            
            // Recursively check neighbors
            findRoomCluster(x+1, y, visited, relX+1, relY, signature);
            findRoomCluster(x-1, y, visited, relX-1, relY, signature);
            findRoomCluster(x, y+1, visited, relX, relY+1, signature);
            findRoomCluster(x, y-1, visited, relX, relY-1, signature);
        }
        
        /**
         * Evaluate challenge balance of the dungeon
         */
        private double evaluateChallenge() {
            // Check if boss room is far from entrance
            double bossDistanceScore = evaluateBossDistance();
            
            // Check if traps are distributed well
            double trapDistributionScore = evaluateTrapDistribution();
            
            // Combine scores
            return bossDistanceScore * 0.6 + trapDistributionScore * 0.4;
        }
        
        /**
         * Evaluate boss room distance from entrance
         */
        private double evaluateBossDistance() {
            // Find boss room
            int bossX = -1;
            int bossY = -1;
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (grid[x][y] == RoomType.BOSS) {
                        bossX = x;
                        bossY = y;
                        break;
                    }
                }
                if (bossX != -1) break;
            }
            
            // If no boss room, return 0
            if (bossX == -1) return 0;
            
            // Calculate distance from entrance
            double distance = Math.sqrt(Math.pow(bossX - entranceX, 2) + Math.pow(bossY - entranceY, 2));
            
            // Calculate ideal distance (about 70% of the way across the dungeon)
            double idealDistance = size * 0.7;
            
            // Calculate score based on how close to ideal
            double distanceScore = 1.0 - Math.abs(distance - idealDistance) / idealDistance;
            if (distanceScore < 0) distanceScore = 0;
            
            return distanceScore;
        }
        
        /**
         * Evaluate trap distribution
         */
        private double evaluateTrapDistribution() {
            // Count traps by distance from entrance
            int[] trapsByDistance = new int[size];
            
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (grid[x][y] == RoomType.TRAP) {
                        int distance = (int)Math.sqrt(Math.pow(x - entranceX, 2) + Math.pow(y - entranceY, 2));
                        if (distance < size) {
                            trapsByDistance[distance]++;
                        }
                    }
                }
            }
            
            // Calculate trap distribution score (lower is better)
            double distributionScore = 0;
            int totalTraps = 0;
            
            for (int i = 0; i < size; i++) {
                totalTraps += trapsByDistance[i];
            }
            
            // If no traps, return 0
            if (totalTraps == 0) return 0;
            
            // Ideal trap distribution increases with distance
            double idealDistribution = 0;
            for (int i = 0; i < size; i++) {
                // Ideal distribution function (more traps further from entrance)
                idealDistribution = (double)i / size;
                
                // Actual distribution
                double actualDistribution = (double)trapsByDistance[i] / totalTraps;
                
                // Add weighted difference to score
                distributionScore += Math.abs(actualDistribution - idealDistribution) * ((double)i / size);
            }
            
            // Normalize score (lower is better, so invert)
            return 1.0 - Math.min(1.0, distributionScore);
        }
        
        /**
         * Create a clone of this individual
         */
        @Override
        protected Individual clone() {
            Individual clone = new Individual(size);
            clone.entranceX = this.entranceX;
            clone.entranceY = this.entranceY;
            
            // Copy grid
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    clone.grid[x][y] = this.grid[x][y];
                }
            }
            
            return clone;
        }
    }

}
