package com.ubivismedia.aidungeon.config;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.config.ConfigurationLoader;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages plugin configuration and themes
 */
public class ConfigManager {
    
    private final AIDungeonGenerator plugin;
    private final ConfigurationLoader configLoader;

    private final Map<String, DungeonTheme> themes = new HashMap<>();
    private final Map<Biome, String> biomeThemeMap = new HashMap<>();
    
    public ConfigManager(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        this.configLoader = new ConfigurationLoader(plugin);
    }
    
    /**
     * Load configuration from config.yml
     */
    public void loadConfig() {
        // Clear existing data
        themes.clear();
        biomeThemeMap.clear();

        // Load configurations
        configLoader.loadConfigurations();
        
        // Get the merged configuration
        FileConfiguration config = plugin.getConfig();
        
        // Load themes
        ConfigurationSection themesSection = config.getConfigurationSection("dungeon.themes");
        if (themesSection != null) {
            loadThemes(themesSection);
        } else {
            plugin.getLogger().warning("No themes section found in config.yml");
            createDefaultThemes();
        }
        
        // Load biome-theme mappings
        ConfigurationSection biomesSection = config.getConfigurationSection("dungeon.biome-themes");
        if (biomesSection != null) {
            loadBiomeThemeMappings(biomesSection);
        } else {
            plugin.getLogger().warning("No biome-themes section found in config.yml");
            createDefaultBiomeMappings();
        }
    }
    
    /**
     * Load themes from configuration
     */
    private void loadThemes(ConfigurationSection themesSection) {
        for (String themeKey : themesSection.getKeys(false)) {
            try {
                ConfigurationSection themeSection = themesSection.getConfigurationSection(themeKey);
                if (themeSection == null) continue;
                
                // Primary blocks
                List<Material> primaryBlocks = parseMaterialList(themeSection, "primary-blocks");
                
                // Accent blocks
                List<Material> accentBlocks = parseMaterialList(themeSection, "accent-blocks");
                
                // Floor blocks
                List<Material> floorBlocks = parseMaterialList(themeSection, "floor-blocks");
                
                // Ceiling blocks
                List<Material> ceilingBlocks = parseMaterialList(themeSection, "ceiling-blocks");
                
                // Light blocks
                List<Material> lightBlocks = parseMaterialList(themeSection, "light-blocks");
                
                // Create theme
                DungeonTheme theme = new DungeonTheme(
                        themeKey, 
                        primaryBlocks, 
                        accentBlocks, 
                        floorBlocks, 
                        ceilingBlocks, 
                        lightBlocks
                );
                
                // Add to themes map
                themes.put(themeKey, theme);
                
                plugin.getLogger().info("Loaded theme: " + themeKey);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading theme: " + themeKey, e);
            }
        }
        
        // If no themes were loaded, create defaults
        if (themes.isEmpty()) {
            createDefaultThemes();
        }
    }
    
    /**
     * Parse a list of material names from configuration
     */
    private List<Material> parseMaterialList(ConfigurationSection section, String key) {
        List<Material> materials = new ArrayList<>();
        
        List<String> materialNames = section.getStringList(key);
        for (String name : materialNames) {
            try {
                Material material = Material.valueOf(name);
                materials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material name: " + name);
            }
        }
        
        // If no materials were parsed, add a default
        if (materials.isEmpty()) {
            materials.add(Material.STONE);
        }
        
        return materials;
    }
    
    /**
     * Load biome-theme mappings from configuration
     */
    private void loadBiomeThemeMappings(ConfigurationSection biomesSection) {
        for (String biomeKey : biomesSection.getKeys(false)) {
            try {
                Biome biome = Biome.valueOf(biomeKey);
                String themeName = biomesSection.getString(biomeKey);
                
                // Make sure the theme exists
                if (themes.containsKey(themeName)) {
                    biomeThemeMap.put(biome, themeName);
                } else {
                    plugin.getLogger().warning("Theme not found for biome mapping: " + biomeKey + " -> " + themeName);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid biome name: " + biomeKey);
            }
        }
    }
    
    /**
     * Create default themes if none were loaded
     */
    private void createDefaultThemes() {
        plugin.getLogger().info("Creating default themes");
        
        // Default theme: RUINS
        List<Material> ruinsPrimaryBlocks = Arrays.asList(
                Material.COBBLESTONE, 
                Material.MOSSY_COBBLESTONE, 
                Material.STONE_BRICKS
        );
        
        List<Material> ruinsAccentBlocks = Arrays.asList(
                Material.CRACKED_STONE_BRICKS, 
                Material.MOSSY_STONE_BRICKS
        );
        
        List<Material> ruinsFloorBlocks = Arrays.asList(
                Material.COBBLESTONE, 
                Material.DIRT, 
                Material.GRAVEL
        );
        
        List<Material> ruinsCeilingBlocks = Arrays.asList(
                Material.COBBLESTONE, 
                Material.STONE_BRICKS
        );
        
        List<Material> ruinsLightBlocks = Collections.singletonList(
                Material.TORCH
        );
        
        DungeonTheme ruinsTheme = new DungeonTheme(
                "RUINS", 
                ruinsPrimaryBlocks, 
                ruinsAccentBlocks, 
                ruinsFloorBlocks, 
                ruinsCeilingBlocks, 
                ruinsLightBlocks
        );
        
        // Add to themes map
        themes.put("RUINS", ruinsTheme);
    }
    
    /**
     * Create default biome mappings if none were loaded
     */
    private void createDefaultBiomeMappings() {
        plugin.getLogger().info("Creating default biome mappings");
        
        // Map all biomes to the default RUINS theme
        String defaultTheme = "RUINS";
        
        for (Biome biome : Biome.values()) {
            biomeThemeMap.put(biome, defaultTheme);
        }
    }
    
    /**
     * Get the theme for a specific biome
     */
    public DungeonTheme getThemeForBiome(Biome biome) {
        String themeName = biomeThemeMap.getOrDefault(biome, "RUINS");
        return themes.getOrDefault(themeName, getDefaultTheme());
    }
    
    /**
     * Get a theme by name
     */
    public DungeonTheme getThemeByName(String name) {
        return themes.get(name);
    }
    
    /**
     * Get the default theme
     */
    public DungeonTheme getDefaultTheme() {
        // Try to get RUINS theme first
        DungeonTheme defaultTheme = themes.get("RUINS");
        
        // If not found, just get the first theme
        if (defaultTheme == null && !themes.isEmpty()) {
            defaultTheme = themes.values().iterator().next();
        }
        
        // If still null, create a basic theme
        if (defaultTheme == null) {
            defaultTheme = new DungeonTheme(
                    "BASIC",
                    Collections.singletonList(Material.STONE),
                    Collections.singletonList(Material.COBBLESTONE),
                    Collections.singletonList(Material.STONE),
                    Collections.singletonList(Material.STONE),
                    Collections.singletonList(Material.TORCH)
            );
            
            themes.put("BASIC", defaultTheme);
        }
        
        return defaultTheme;
    }
    
    /**
     * Get all themes
     */
    public Collection<DungeonTheme> getAllThemes() {
        return themes.values();
    }

    /**
     * Access the configuration loader
     */
    public ConfigurationLoader getConfigLoader() {
        return configLoader;
    }
}
