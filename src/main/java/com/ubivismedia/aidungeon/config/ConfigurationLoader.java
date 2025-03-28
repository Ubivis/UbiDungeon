package com.ubivismedia.aidungeon.config;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Advanced configuration loader that supports multi-file configurations
 */
public class ConfigurationLoader {
    
    private final AIDungeonGenerator plugin;
    private final Map<String, FileConfiguration> configFiles = new HashMap<>();
    
    // Configuration file paths
    private static final String[] CONFIG_FILES = {
        "config.yml",
        "conf/dungeon.yml",
        "conf/bosses.yml",
        "conf/env.yml",
        "conf/quest.yml"
    };
    
    public ConfigurationLoader(AIDungeonGenerator plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigurations() {
        // Clear existing configurations
        configFiles.clear();
        
        // Ensure configuration directory exists
        File configDir = new File(plugin.getDataFolder(), "conf");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        
        // Load each configuration file
        for (String configPath : CONFIG_FILES) {
            loadConfigFile(configPath);
        }
        
        // Merge configurations
        mergeConfigurations();
    }
    
    /**
     * Load a specific configuration file
     */
    private void loadConfigFile(String configPath) {
        File configFile = new File(plugin.getDataFolder(), configPath);
        
        // Create file from resources if it doesn't exist
        if (!configFile.exists()) {
            try {
                // Ensure parent directories exist
                configFile.getParentFile().mkdirs();
                
                // Copy from resources
                plugin.saveResource(configPath, false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not create config file: " + configPath, e);
                return;
            }
        }
        
        // Load configuration
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // Get filename without path for key
            String key = configPath.contains("/") 
                ? configPath.substring(configPath.lastIndexOf('/') + 1, configPath.lastIndexOf('.'))
                : configPath.substring(0, configPath.lastIndexOf('.'));
            
            configFiles.put(key, config);
            
            plugin.getLogger().info("Loaded configuration: " + configPath);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading configuration: " + configPath, e);
        }
    }
    
    /**
     * Merge configurations and set as plugin's configuration
     */
    private void mergeConfigurations() {
        // Start with main configuration
        FileConfiguration mainConfig = configFiles.get("config");
        if (mainConfig == null) {
            plugin.getLogger().severe("Main configuration file is missing!");
            return;
        }
        
        // Check configuration version
        int configVersion = mainConfig.getInt("config-version", 0);
        if (configVersion == 0) {
            plugin.getLogger().warning("Configuration version not found. Using default settings.");
        }
        
        // Process imports from main configuration
        ConfigurationSection importsSection = mainConfig.getConfigurationSection("imports");
        if (importsSection != null) {
            for (String key : importsSection.getKeys(false)) {
                // Get the corresponding configuration file
                FileConfiguration importedConfig = configFiles.get(key);
                if (importedConfig != null) {
                    // Merge the imported configuration into main config
                    mergeConfigurations(mainConfig, importedConfig, key);
                }
            }
        }
        
        // Set the merged configuration as the plugin's configuration
        plugin.saveConfig();
        plugin.reloadConfig();
    }
    
    /**
     * Merge two configurations
     */
    private void mergeConfigurations(FileConfiguration main, FileConfiguration imported, String importKey) {
        // Iterate through all keys in the imported configuration
        for (String key : imported.getKeys(true)) {
            // Skip section keys
            if (imported.isConfigurationSection(key)) continue;
            
            // Construct full path in main configuration
            String fullPath = importKey + "." + key;
            
            // Set value in main configuration
            main.set(fullPath, imported.get(key));
        }
    }
    
    /**
     * Get a specific configuration file
     */
    public FileConfiguration getConfig(String configName) {
        return configFiles.get(configName);
    }
    
    /**
     * Save a specific configuration file
     */
    public void saveConfig(String configName) {
        FileConfiguration config = configFiles.get(configName);
        if (config == null) {
            plugin.getLogger().warning("Cannot save unknown configuration: " + configName);
            return;
        }
        
        try {
            File configFile = new File(plugin.getDataFolder(), 
                configName.equals("config") ? "config.yml" : "conf/" + configName + ".yml");
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configuration: " + configName, e);
        }
    }
    
    /**
     * Reload all configurations
     */
    public void reloadConfigurations() {
        loadConfigurations();
    }
}