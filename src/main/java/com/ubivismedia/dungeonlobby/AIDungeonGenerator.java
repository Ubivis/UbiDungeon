package com.yourdomain.aidungeon;

import com.yourdomain.aidungeon.commands.DungeonCommand;
import com.yourdomain.aidungeon.config.ConfigManager;
import com.yourdomain.aidungeon.dungeons.BiomeTracker;
import com.yourdomain.aidungeon.dungeons.DungeonManager;
import com.yourdomain.aidungeon.listeners.PlayerMoveListener;
import com.yourdomain.aidungeon.storage.DungeonStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class AIDungeonGenerator extends JavaPlugin {
    
    private ConfigManager configManager;
    private DungeonManager dungeonManager;
    private BiomeTracker biomeTracker;
    private DungeonStorage dungeonStorage;
    
    @Override
    public void onEnable() {
        // Initialize config
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize storage
        dungeonStorage = new DungeonStorage(this);
        dungeonStorage.initialize();
        
        // Initialize components
        biomeTracker = new BiomeTracker(this);
        dungeonManager = new DungeonManager(this, biomeTracker, dungeonStorage);
        
        // Initialize trap and mob handlers
        TrapHandler trapHandler = new TrapHandler(this);
        MobHandler mobHandler = new MobHandler(this);
        
        // Register commands
        getCommand("aidungeon").setExecutor(new DungeonCommand(this, dungeonManager));
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this, biomeTracker, dungeonManager), this);
        Bukkit.getPluginManager().registerEvents(trapHandler, this);
        Bukkit.getPluginManager().registerEvents(mobHandler, this);
        
        // Load existing dungeons
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                dungeonStorage.loadExistingDungeons();
                getLogger().info("Loaded existing dungeon data");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to load dungeon data", e);
            }
        });
        
        getLogger().info("AI Dungeon Generator has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save any pending dungeon data
        if (dungeonStorage != null) {
            dungeonStorage.saveAllDungeons();
        }
        
        // Cancel any pending tasks
        Bukkit.getScheduler().cancelTasks(this);
        
        getLogger().info("AI Dungeon Generator has been disabled!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }
    
    public BiomeTracker getBiomeTracker() {
        return biomeTracker;
    }
    
    public DungeonStorage getDungeonStorage() {
        return dungeonStorage;
    }
    
    /**
     * Reload the plugin configuration
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (configManager != null) {
            configManager.loadConfig();
        }
        getLogger().info("Configuration reloaded");
    }
}
