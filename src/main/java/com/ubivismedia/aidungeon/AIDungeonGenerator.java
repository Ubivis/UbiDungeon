package com.ubivismedia.aidungeon;

import com.ubivismedia.aidungeon.commands.DungeonCommand;
import com.ubivismedia.aidungeon.commands.QuestCommand;
import com.ubivismedia.aidungeon.config.ConfigManager;
import com.ubivismedia.aidungeon.dungeons.BiomeTracker;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.handlers.MobHandler;
import com.ubivismedia.aidungeon.handlers.TrapHandler;
import com.ubivismedia.aidungeon.listeners.PlayerMoveListener;
import com.ubivismedia.aidungeon.quests.QuestSystem;
import com.ubivismedia.aidungeon.storage.DungeonStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class AIDungeonGenerator extends JavaPlugin {
    
    private ConfigManager configManager;
    private DungeonManager dungeonManager;
    private BiomeTracker biomeTracker;
    private DungeonStorage dungeonStorage;
    private QuestSystem questSystem;
    
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
        
        // Initialize quest system
        questSystem = new QuestSystem(this);
        
        // Register commands
        getCommand("aidungeon").setExecutor(new DungeonCommand(this, dungeonManager));
        getCommand("quests").setExecutor(new QuestCommand(this, questSystem));
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this, biomeTracker, dungeonManager, questSystem), this);
        Bukkit.getPluginManager().registerEvents(trapHandler, this);
        Bukkit.getPluginManager().registerEvents(mobHandler, this);
        Bukkit.getPluginManager().registerEvents(questSystem, this);
        
        // Load existing dungeons
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                dungeonStorage.loadExistingDungeons();
                getLogger().info("Loaded existing dungeon data");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to load dungeon data", e);
            }
        });
        
        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Found PlaceholderAPI! Registering placeholders...");
            new com.ubivismedia.aidungeon.integration.PlaceholderManager(this).register();
        }
        
        getLogger().info("AI Dungeon Generator has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save any pending dungeon data
        if (dungeonStorage != null) {
            dungeonStorage.saveAllDungeons();
        }
        
        // Save quest data
        if (questSystem != null) {
            questSystem.savePlayerQuests();
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
    
    public QuestSystem getQuestSystem() {
        return questSystem;
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
