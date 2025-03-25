package com.ubivismedia.aidungeon.storage;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonLayout;
import com.ubivismedia.aidungeon.dungeons.RoomType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles persistent storage of dungeon data
 */
public class DungeonStorage {
    
    private final AIDungeonGenerator plugin;
    private final File storageFile;
    private FileConfiguration storage;
    
    // In-memory cache of dungeon data
    private final Map<String, DungeonData> dungeonDataCache = new ConcurrentHashMap<>();
    
    /**
     * Create a new dungeon storage manager
     */
    public DungeonStorage(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "dungeons.yml");
    }
    
    /**
     * Initialize the storage
     */
    public void initialize() {
        if (!storageFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create dungeons.yml", e);
            }
        }
        
        this.storage = YamlConfiguration.loadConfiguration(storageFile);
    }
    
    /**
     * Save a dungeon to storage
     */
    public void saveDungeon(BiomeArea area, DungeonData data) {
        String key = getStorageKey(area);
        dungeonDataCache.put(key, data);
        
        // Save to configuration
        storage.set(key + ".biome", area.getPrimaryBiome().name());
        storage.set(key + ".x", area.getCenterX());
        storage.set(key + ".z", area.getCenterZ());
        storage.set(key + ".radius", area.getRadius());
        storage.set(key + ".discoverer", data.getDiscovererUUID().toString());
        storage.set(key + ".timestamp", data.getTimestamp());
        storage.set(key + ".theme", data.getTheme().getName());
        
        // Save layout data - only save essential information
        // We'll regenerate the full layout when needed
        saveLayoutData(key, data.getLayout());
        
        // Save to file asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveStorageFile);
    }
    
    /**
     * Save layout data to storage
     */
    private void saveLayoutData(String key, DungeonLayout layout) {
        // Save entrance position
        storage.set(key + ".layout.entrance.x", layout.getEntranceX());
        storage.set(key + ".layout.entrance.y", layout.getEntranceY());
        
        // Save special rooms
        List<Map<String, Object>> specialRooms = new ArrayList<>();
        
        // Save each room type
        for (RoomType type : RoomType.values()) {
            if (type.isSpecial()) {
                // Get positions for this type
                List<org.bukkit.util.Vector> positions = layout.getRoomPositions(type);
                
                for (org.bukkit.util.Vector pos : positions) {
                    Map<String, Object> room = new HashMap<>();
                    room.put("type", type.name());
                    room.put("x", pos.getBlockX());
                    room.put("z", pos.getBlockZ());
                    specialRooms.add(room);
                }
            }
        }
        
        // Save special rooms
        storage.set(key + ".layout.special_rooms", specialRooms);
    }
    
    /**
     * Save the storage file to disk
     */
    private void saveStorageFile() {
        try {
            storage.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save dungeons.yml", e);
        }
    }
    
    /**
     * Load existing dungeons from storage
     */
    public void loadExistingDungeons() {
        if (!storageFile.exists()) {
            return;
        }
        
        // Clear cache
        dungeonDataCache.clear();
        
        // Load all sections
        Set<String> keys = storage.getKeys(false);
        
        for (String key : keys) {
            try {
                ConfigurationSection section = storage.getConfigurationSection(key);
                if (section == null) continue;
                
                // Load BiomeArea
                String worldName = key.split("\\.")[0];
                World world = Bukkit.getWorld(worldName);
                
                if (world == null) {
                    plugin.getLogger().warning("World not found: " + worldName);
                    continue;
                }
                
                Biome biome = Biome.valueOf(section.getString("biome"));
                int x = section.getInt("x");
                int z = section.getInt("z");
                int radius = section.getInt("radius");
                
                BiomeArea area = new BiomeArea(worldName, x, z, radius, biome);
                
                // Load data
                UUID discoverer = UUID.fromString(section.getString("discoverer"));
                long timestamp = section.getLong("timestamp");
                String themeName = section.getString("theme");
                
                // Get theme
                DungeonTheme theme = plugin.getConfigManager().getThemeByName(themeName);
                if (theme == null) {
                    theme = plugin.getConfigManager().getDefaultTheme();
                }
                
                // Create a basic layout - we'll need to regenerate the full layout
                // when it's actually needed since it's complex and memory-intensive
                DungeonLayout layout = createBasicLayout(section, theme);
                
                // Create dungeon data
                DungeonData data = new DungeonData(layout, discoverer, timestamp);
                
                // Add to cache
                dungeonDataCache.put(key, data);
                
                plugin.getLogger().info("Loaded dungeon at " + x + "," + z + 
                        " in " + worldName + " (biome: " + biome + ")");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading dungeon data for " + key, e);
            }
        }
    }
    
    /**
     * Create a basic layout from storage data
     */
    private DungeonLayout createBasicLayout(ConfigurationSection section, DungeonTheme theme) {
        // Create generic sized layout
        DungeonLayout layout = new DungeonLayout(50, theme);
        
        // Set entrance
        ConfigurationSection entranceSection = section.getConfigurationSection("layout.entrance");
        if (entranceSection != null) {
            int entranceX = entranceSection.getInt("x");
            int entranceY = entranceSection.getInt("y");
            layout.setEntrancePosition(entranceX, entranceY);
        } else {
            // Default to center
            layout.setEntrancePosition(layout.getSize() / 2, layout.getSize() / 2);
        }
        
        // Load special rooms
        List<Map<?, ?>> specialRooms = section.getMapList("layout.special_rooms");
        
        for (Map<?, ?> roomData : specialRooms) {
            String typeName = (String) roomData.get("type");
            int x = (int) roomData.get("x");
            int z = (int) roomData.get("z");
            
            RoomType type = RoomType.valueOf(typeName);
            layout.setRoomType(x, z, type);
        }
        
        return layout;
    }
    
    /**
     * Get a dungeon from storage by area
     */
    public DungeonData getDungeon(BiomeArea area) {
        String key = getStorageKey(area);
        return dungeonDataCache.get(key);
    }
    
    /**
     * Get all dungeons from storage
     */
    public Map<BiomeArea, DungeonData> getAllDungeons() {
        Map<BiomeArea, DungeonData> dungeons = new HashMap<>();
        
        for (String key : dungeonDataCache.keySet()) {
            try {
                String[] parts = key.split("\\.");
                
                String worldName = parts[0];
                World world = Bukkit.getWorld(worldName);
                
                if (world == null) continue;
                
                // Get data from configuration
                int x = storage.getInt(key + ".x");
                int z = storage.getInt(key + ".z");
                int radius = storage.getInt(key + ".radius");
                Biome biome = Biome.valueOf(storage.getString(key + ".biome"));
                
                BiomeArea area = new BiomeArea(worldName, x, z, radius, biome);
                
                dungeons.put(area, dungeonDataCache.get(key));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing dungeon data for " + key, e);
            }
        }
        
        return dungeons;
    }
    
    /**
     * Get the storage key for a biome area
     */
    private String getStorageKey(BiomeArea area) {
        return area.getWorldName() + "." + area.getCenterX() + "." + area.getCenterZ();
    }
    
    /**
     * Save all dungeons to disk
     */
    public void saveAllDungeons() {
        saveStorageFile();
    }
}
