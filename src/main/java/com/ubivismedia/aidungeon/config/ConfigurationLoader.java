package com.ubivismedia.aidungeon.config;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // Config keys for easier reference
    private static final String[] CONFIG_KEYS = {
            "config",
            "dungeon",
            "bosses",
            "env",
            "quest"
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
        for (int i = 0; i < CONFIG_FILES.length; i++) {
            String configPath = CONFIG_FILES[i];
            String configKey = CONFIG_KEYS[i];
            loadConfigFile(configPath, configKey);
        }

        // Merge configurations
        mergeConfigurations();
    }

    /**
     * Load a specific configuration file
     */
    private void loadConfigFile(String configPath, String configKey) {
        File configFile = new File(plugin.getDataFolder(), configPath);

        // Create file from resources if it doesn't exist
        if (!configFile.exists()) {
            try {
                // Ensure parent directories exist
                configFile.getParentFile().mkdirs();

                // Copy from resources
                plugin.saveResource(configPath, false);
                plugin.getLogger().info("Created default configuration: " + configPath);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not create config file: " + configPath, e);

                // Try to create an empty file if the resource doesn't exist
                try {
                    configFile.createNewFile();
                    plugin.getLogger().info("Created empty configuration file: " + configPath);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create even an empty file: " + configPath, ex);
                }
            }
        }

        // Load configuration
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            configFiles.put(configKey, config);

            plugin.getLogger().info("Loaded configuration: " + configPath + " as " + configKey);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading configuration: " + configPath, e);

            // Add empty config to prevent null pointer exceptions
            configFiles.put(configKey, new YamlConfiguration());
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
            mainConfig = new YamlConfiguration();
            configFiles.put("config", mainConfig);
        }

        // Explicitly log and override configuration values
        plugin.getLogger().info("Merging configurations...");

        // Process imports from main configuration
        ConfigurationSection importsSection = mainConfig.getConfigurationSection("imports");
        if (importsSection != null) {
            for (String key : importsSection.getKeys(false)) {
                // Get the corresponding configuration file
                FileConfiguration importedConfig = configFiles.get(key);
                if (importedConfig != null) {
                    // Explicitly log which configuration is being imported
                    plugin.getLogger().info("Importing configuration for: " + key);

                    // Import specific configuration sections
                    if (key.equals("dungeon")) {
                        // Specifically handle dungeon configuration
                        importDungeonConfig(mainConfig, importedConfig);
                    }
                }
            }
        }

        // Save the main config to disk to ensure it's properly updated
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            mainConfig.save(configFile);
            plugin.getLogger().info("Updated configuration saved to disk");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving updated configuration", e);
        }
    }

    /**
     * Explicitly import dungeon configuration
     */
    private void importDungeonConfig(FileConfiguration mainConfig, FileConfiguration dungeonConfig) {
        // List of paths to import from dungeon configuration
        String[] pathsToImport = {
                "discovery.exploration-threshold",
                "discovery.discovery-radius",
                "discovery.enable-compass",
                "discovery.show-on-map",
                "discovery.hint-message"
        };

        // Import each specified path
        for (String path : pathsToImport) {
            if (dungeonConfig.contains(path)) {
                Object value = dungeonConfig.get(path);
                plugin.getLogger().info("Importing config: " + path + " = " + value);
                mainConfig.set(path, value);
            } else {
                plugin.getLogger().warning("Path not found in dungeon config: " + path);
            }
        }
    }

    /**
     * Get a specific configuration file
     */
    public FileConfiguration getConfig(String configName) {
        FileConfiguration config = configFiles.get(configName);
        if (config == null) {
            plugin.getLogger().warning("Requested unknown configuration: " + configName + ", creating empty config");
            config = new YamlConfiguration();
            configFiles.put(configName, config);
        }
        return config;
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
            // Determine the correct file path based on config name
            String filePath;
            if (configName.equals("config")) {
                filePath = "config.yml";
            } else {
                filePath = "conf/" + configName + ".yml";
            }

            File configFile = new File(plugin.getDataFolder(), filePath);
            // Ensure parent directory exists
            configFile.getParentFile().mkdirs();

            config.save(configFile);
            plugin.getLogger().info("Saved configuration: " + filePath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save configuration: " + configName, e);
        }
    }

    /**
     * Initialize all configuration files
     * This is called during plugin startup to ensure all files exist
     */
    public void initializeConfigs() {
        // Ensure configuration directory exists
        File configDir = new File(plugin.getDataFolder(), "conf");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Create each config file if it doesn't exist
        for (int i = 0; i < CONFIG_FILES.length; i++) {
            String configPath = CONFIG_FILES[i];
            File configFile = new File(plugin.getDataFolder(), configPath);

            if (!configFile.exists()) {
                try {
                    configFile.getParentFile().mkdirs();
                    plugin.saveResource(configPath, false);
                    plugin.getLogger().info("Created default configuration: " + configPath);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not create config from resource: " + configPath);

                    // Try creating an empty file instead
                    try {
                        configFile.createNewFile();
                        plugin.getLogger().info("Created empty config file: " + configPath);
                    } catch (IOException ex) {
                        plugin.getLogger().severe("Failed to create empty config file: " + configPath);
                    }
                }
            }
        }
    }

    /**
     * Reload all configurations
     */
    public void reloadConfigurations() {
        // Don't call loadConfigurations() directly to avoid potential loops
        // Instead, just reload each file individually without merging
        for (int i = 0; i < CONFIG_FILES.length; i++) {
            String configPath = CONFIG_FILES[i];
            String configKey = CONFIG_KEYS[i];
            reloadConfigFile(configPath, configKey);
        }

        plugin.getLogger().info("Individual configuration files reloaded");
    }

    /**
     * Reload a specific configuration file
     */
    private void reloadConfigFile(String configPath, String configKey) {
        File configFile = new File(plugin.getDataFolder(), configPath);

        if (configFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                configFiles.put(configKey, config);

                plugin.getLogger().info("Reloaded configuration: " + configPath);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error reloading configuration: " + configPath, e);
            }
        } else {
            plugin.getLogger().warning("Config file does not exist: " + configPath);
        }
    }

    /**
     * Save all configuration files
     */
    public void saveAllConfigs() {
        for (String configKey : CONFIG_KEYS) {
            saveConfig(configKey);
        }
        plugin.getLogger().info("All configuration files saved");
    }

    /**
     * Convert a list of Materials to a list of names
     */
    private List<String> getMaterialNames(List<Material> materials) {
        List<String> names = new ArrayList<>();
        for (Material material : materials) {
            names.add(material.name());
        }
        return names;
    }
}