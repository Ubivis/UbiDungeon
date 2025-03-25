package com.ubivismedia.aidungeon.dungeons;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.algorithms.DungeonGenerator;
import com.ubivismedia.aidungeon.algorithms.cellular.CellularAutomata;
import com.ubivismedia.aidungeon.algorithms.genetic.GeneticOptimizer;
import com.ubivismedia.aidungeon.algorithms.markov.MarkovChainModel;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.storage.DungeonData;
import com.ubivismedia.aidungeon.storage.DungeonStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DungeonManager {
    
    private final AIDungeonGenerator plugin;
    private final BiomeTracker biomeTracker;
    private final DungeonStorage dungeonStorage;
    
    private final Map<BiomeArea, DungeonData> generatedDungeons = new ConcurrentHashMap<>();
    private final Queue<GenerationTask> generationQueue = new ConcurrentLinkedQueue<>();
    private final Cache<UUID, Long> playerGenerationCooldown;
    
    private final DungeonGenerator dungeonGenerator;
    private final AtomicInteger activeGenerations = new AtomicInteger(0);
    private final int maxConcurrentGenerations;
    private final boolean asyncGenerationEnabled;
    
    public DungeonManager(AIDungeonGenerator plugin, BiomeTracker biomeTracker, DungeonStorage dungeonStorage) {
        this.plugin = plugin;
        this.biomeTracker = biomeTracker;
        this.dungeonStorage = dungeonStorage;
        
        // Initialize from config
        this.maxConcurrentGenerations = plugin.getConfig().getInt("generation.async.max-concurrent-generations", 3);
        this.asyncGenerationEnabled = plugin.getConfig().getBoolean("generation.async.enabled", true);
        
        // Initialize algorithm components
        CellularAutomata roomGenerator = new CellularAutomata(plugin);
        MarkovChainModel themeModel = new MarkovChainModel(plugin);
        GeneticOptimizer layoutOptimizer = new GeneticOptimizer(plugin);
        
        // Initialize generation systems
        this.dungeonGenerator = new DungeonGenerator(plugin, roomGenerator, themeModel, layoutOptimizer);
        
        // Initialize cooldown cache (5 minutes cooldown per player)
        this.playerGenerationCooldown = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        
        // Start async processing if enabled
        if (asyncGenerationEnabled) {
            startAsyncProcessing();
        }
    }
    
    /**
     * Checks if a dungeon can be generated for a player in a specific biome area
     */
    public boolean canGenerateDungeon(Player player, BiomeArea area) {
        // Check if already generated
        if (generatedDungeons.containsKey(area)) {
            return false;
        }
        
        // Check world blacklist
        if (plugin.getConfig().getStringList("settings.world-blacklist").contains(area.getWorldName())) {
            return false;
        }
        
        // Check player cooldown
        if (playerGenerationCooldown.getIfPresent(player.getUniqueId()) != null) {
            return false;
        }
        
        // Check minimum distance
        int minDistance = plugin.getConfig().getInt("settings.min-distance-between-dungeons", 1000);
        for (BiomeArea existingArea : generatedDungeons.keySet()) {
            if (existingArea.getWorldName().equals(area.getWorldName())) {
                double distance = Math.sqrt(
                        Math.pow(existingArea.getCenterX() - area.getCenterX(), 2) +
                        Math.pow(existingArea.getCenterZ() - area.getCenterZ(), 2)
                );
                if (distance < minDistance) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Queue a dungeon for generation
     */
    public void queueDungeonGeneration(BiomeArea area, Player discoverer) {
        if (!canGenerateDungeon(discoverer, area)) {
            return;
        }
        
        // Set cooldown for this player
        playerGenerationCooldown.put(discoverer.getUniqueId(), System.currentTimeMillis());
        
        // Add to generation queue
        GenerationTask task = new GenerationTask(area, discoverer.getUniqueId());
        generationQueue.add(task);
        
        // If async is disabled, process immediately on the main thread
        if (!asyncGenerationEnabled) {
            Bukkit.getScheduler().runTask(plugin, () -> processGenerationTask(task));
        }
        
        plugin.getLogger().info("Queued dungeon generation in " + area.getPrimaryBiome() + 
                " at " + area.getCenterX() + "," + area.getCenterZ() + 
                " discovered by " + discoverer.getName());
    }
    
    /**
     * Start the async processing task
     */
    private void startAsyncProcessing() {
        int tasksPerTick = plugin.getConfig().getInt("generation.async.tasks-per-tick", 2);
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Skip if we're at max concurrent generations
            if (activeGenerations.get() >= maxConcurrentGenerations) {
                return;
            }
            
            // Process up to tasksPerTick tasks
            for (int i = 0; i < tasksPerTick; i++) {
                GenerationTask task = generationQueue.poll();
                if (task == null) {
                    break;
                }
                
                // Increment counter and process
                activeGenerations.incrementAndGet();
                processGenerationTaskAsync(task);
            }
        }, 20L, 10L); // Check queue every 10 ticks
    }
    
    /**
     * Process a generation task asynchronously
     */
    private void processGenerationTaskAsync(GenerationTask task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Generate dungeon layout asynchronously
                DungeonLayout layout = dungeonGenerator.generateDungeonAsync(task.getArea());
                
                // Schedule synchronous placement in world
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        placeDungeonInWorld(layout, task.getArea());
                        
                        // Create dungeon data
                        DungeonData dungeonData = new DungeonData(
                                layout, 
                                task.getDiscovererUUID(), 
                                System.currentTimeMillis()
                        );
                        
                        // Store in memory and persistent storage
                        generatedDungeons.put(task.getArea(), dungeonData);
                        dungeonStorage.saveDungeon(task.getArea(), dungeonData);
                        
                        // Notify discoverer if online
                        notifyPlayer(task.getDiscovererUUID(), task.getArea());
                    } finally {
                        // Decrement counter when done
                        activeGenerations.decrementAndGet();
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Error generating dungeon: " + e.getMessage());
                e.printStackTrace();
                
                // Decrement counter even if failed
                activeGenerations.decrementAndGet();
            }
        });
    }
    
    /**
     * Process a generation task synchronously
     */
    private void processGenerationTask(GenerationTask task) {
        try {
            // Generate dungeon layout on main thread
            DungeonLayout layout = dungeonGenerator.generateDungeon(task.getArea());
            
            // Place dungeon in world
            placeDungeonInWorld(layout, task.getArea());
            
            // Create dungeon data
            DungeonData dungeonData = new DungeonData(
                    layout,
                    task.getDiscovererUUID(),
                    System.currentTimeMillis()
            );
            
            // Store in memory and persistent storage
            generatedDungeons.put(task.getArea(), dungeonData);
            dungeonStorage.saveDungeon(task.getArea(), dungeonData);
            
            // Notify discoverer if online
            notifyPlayer(task.getDiscovererUUID(), task.getArea());
        } catch (Exception e) {
            plugin.getLogger().severe("Error generating dungeon: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Place a dungeon in the world (must be called on main thread)
     */
    private void placeDungeonInWorld(DungeonLayout layout, BiomeArea area) {
        // Get world
        World world = Bukkit.getWorld(area.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("World not found: " + area.getWorldName());
            return;
        }
        
        // Find a suitable Y coordinate
        int baseY = findSuitableY(world, area.getCenterX(), area.getCenterZ());
        Location baseLocation = new Location(world, area.getCenterX(), baseY, area.getCenterZ());
        
        // Place dungeon blocks
        layout.placeInWorld(baseLocation);
        
        plugin.getLogger().info("Placed dungeon at " + baseLocation.getBlockX() + "," + 
                baseLocation.getBlockY() + "," + baseLocation.getBlockZ() + 
                " in world " + world.getName());
    }
    
    /**
     * Find a suitable Y coordinate for dungeon placement
     */
    private int findSuitableY(World world, int x, int z) {
        // Start from height 40 (avoid deep underground), then work upwards
        // to find the first non-air block, then go down a bit to place dungeon
        for (int y = 40; y < world.getMaxHeight() - 20; y++) {
            if (!world.getBlockAt(x, y, z).getType().isAir() && 
                world.getBlockAt(x, y + 1, z).getType().isAir()) {
                return y - 10; // Place dungeon below surface
            }
        }
        
        // Default if we can't find a good spot
        return 40;
    }
    
    /**
     * Notify a player about dungeon discovery
     */
    private void notifyPlayer(UUID playerUUID, BiomeArea area) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Get theme name
        DungeonTheme theme = plugin.getConfigManager().getThemeForBiome(area.getPrimaryBiome());
        
        if (plugin.getConfig().getBoolean("discovery.hint-message", true)) {
            // Send message
            player.sendMessage("§6You have discovered a " + theme.getName() + " dungeon!");
            player.sendMessage("§eExplore the area to find the entrance.");
        }
        
        // Give compass if enabled
        if (plugin.getConfig().getBoolean("discovery.enable-compass", true)) {
            ItemHelper.giveDungeonCompass(plugin, player, area);
        }
    }
    
    /**
     * Get a dungeon by area
     */
    public DungeonData getDungeon(BiomeArea area) {
        return generatedDungeons.get(area);
    }
    
    /**
     * Add a pre-loaded dungeon to the manager
     */
    public void addDungeon(BiomeArea area, DungeonData data) {
        generatedDungeons.put(area, data);
    }
    
    /**
     * Get the map of all dungeons
     */
    public Map<BiomeArea, DungeonData> getAllDungeons() {
        return generatedDungeons;
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        return generationQueue.size();
    }
    
    /**
     * Get number of active generations
     */
    public int getActiveGenerations() {
        return activeGenerations.get();
    }

        public BiomeArea getDungeonAreaAtLocation(Location location) {
        // Get all dungeons
        Map<BiomeArea, DungeonData> dungeons = getAllDungeons();
        
        for (BiomeArea area : dungeons.keySet()) {
            if (area.getWorldName().equals(location.getWorld().getName())) {
                double distance = Math.sqrt(
                        Math.pow(area.getCenterX() - location.getBlockX(), 2) +
                        Math.pow(area.getCenterZ() - location.getBlockZ(), 2)
                );
                
                if (distance <= area.getRadius()) {
                    return area;
                }
            }
        }
        
        return null;
    }
}
