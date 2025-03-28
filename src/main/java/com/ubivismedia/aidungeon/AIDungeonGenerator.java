package com.ubivismedia.aidungeon;

import com.ubivismedia.aidungeon.commands.DungeonCommand;
import com.ubivismedia.aidungeon.commands.QuestCommand;
import com.ubivismedia.aidungeon.config.ConfigManager;
import com.ubivismedia.aidungeon.config.ConfigurationLoader;
import com.ubivismedia.aidungeon.dungeons.BiomeTracker;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.dungeons.BiomeExplorationTracker;
import com.ubivismedia.aidungeon.handlers.MobHandler;
import com.ubivismedia.aidungeon.handlers.TrapHandler;
import com.ubivismedia.aidungeon.listeners.PlayerMoveListener;
import com.ubivismedia.aidungeon.quests.QuestSystem;
import com.ubivismedia.aidungeon.storage.DungeonStorage;
import com.ubivismedia.aidungeon.boss.BossManager;

import com.ubivismedia.aidungeon.api.AIDungeonAPI;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.logging.Level;

public class AIDungeonGenerator extends JavaPlugin {
    
    private ConfigManager configManager;
    private DungeonManager dungeonManager;
    private BiomeTracker biomeTracker;
    private DungeonStorage dungeonStorage;
    private QuestSystem questSystem;
    private BiomeExplorationTracker biomeExplorationTracker;
    private AIDungeonAPI api;
    private LanguageManager languageManager;
    private BossManager bossManager;

    @Override
    public void onEnable() {
        // Initialize config
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        this.languageManager = new LanguageManager(this);
        
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

        // Initialize biome exploration tracker
        biomeExplorationTracker = new BiomeExplorationTracker();

        // Initialize boss manager
        this.bossManager = new BossManager(this);

        this.api = new AIDungeonAPI(this);

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
            questSystem.cleanupDisplays();
        }
        
        // Cancel any pending tasks
        Bukkit.getScheduler().cancelTasks(this);
        
        getLogger().info("AI Dungeon Generator has been disabled!");
    }

    /**
     * Save default configurations to plugin data folder
     */
    private void saveDefaultConfigurations() {
        // Configuration file paths
        String[] configFiles = {
            "config.yml",
            "conf/dungeon.yml",
            "conf/bosses.yml", 
            "conf/env.yml", 
            "conf/quest.yml"
        };
        
        // Save each configuration file
        for (String configPath : configFiles) {
            File configFile = new File(getDataFolder(), configPath);
            
            // Only save if file doesn't exist
            if (!configFile.exists()) {
                // Ensure parent directory exists
                configFile.getParentFile().mkdirs();
                
                // Save from resources
                saveResource(configPath, false);
                getLogger().info("Created default configuration: " + configPath);
            }
        }
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

    public BossManager getBossManager() {
        return bossManager;
    }
    
    public QuestSystem getQuestSystem() {
        return questSystem;
    }

    public BiomeExplorationTracker getBiomeExplorationTracker() {
        return biomeExplorationTracker;
    }

    public AIDungeonAPI getAPI() {
        return api;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public NamespacedKey getNamespacedKey(String key) {
        return new NamespacedKey(this, key);
    }
    
    /**
     * Reload the plugin configuration
     */
    @Override
    public void reloadConfig() {
        // Use configuration loader to reload all configurations
        configManager.loadConfig();
        
        // Additional reload logic if needed
        getLogger().info("Configurations reloaded");
    }

    /**
     * Override default config saving to support multi-file configuration
     */
    @Override
    public void saveConfig() {
        ConfigurationLoader loader = configManager.getConfigLoader();
        
        // Save each configuration file
        loader.saveConfig("config");
        loader.saveConfig("dungeon");
        loader.saveConfig("bosses");
        loader.saveConfig("env");
        loader.saveConfig("quest");
    }

}
