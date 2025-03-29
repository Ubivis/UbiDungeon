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

    // Flag to prevent infinite config loading loops
    private boolean isLoading = false;

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
        if (!isLoading) {
            configLoader.loadConfigurations();
        } else {
            // If we're already loading, just reload individual files
            configLoader.reloadConfigurations();
        }

        // Get the main configuration and dungeon configuration
        FileConfiguration mainConfig = plugin.getConfig();
        FileConfiguration dungeonConfig = configLoader.getConfig("dungeon");

        // Load themes from dungeon.yml
        ConfigurationSection themesSection = dungeonConfig.getConfigurationSection("themes");
        if (themesSection != null) {
            loadThemes(themesSection);
        } else {
            plugin.getLogger().warning("No themes section found in dungeon.yml");
            createDefaultThemes();
        }

        // Load biome-theme mappings from dungeon.yml
        ConfigurationSection biomesSection = dungeonConfig.getConfigurationSection("biome-themes");
        if (biomesSection != null) {
            loadBiomeThemeMappings(biomesSection);
        } else {
            plugin.getLogger().warning("No biome-themes section found in dungeon.yml");
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

        // Create PYRAMID theme
        List<Material> pyramidPrimaryBlocks = Arrays.asList(
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.CUT_SANDSTONE
        );

        List<Material> pyramidAccentBlocks = Arrays.asList(
                Material.GOLD_BLOCK,
                Material.CHISELED_SANDSTONE
        );

        List<Material> pyramidFloorBlocks = Arrays.asList(
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE
        );

        List<Material> pyramidCeilingBlocks = Arrays.asList(
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE
        );

        List<Material> pyramidLightBlocks = Arrays.asList(
                Material.TORCH,
                Material.LANTERN
        );

        DungeonTheme pyramidTheme = new DungeonTheme(
                "PYRAMID",
                pyramidPrimaryBlocks,
                pyramidAccentBlocks,
                pyramidFloorBlocks,
                pyramidCeilingBlocks,
                pyramidLightBlocks
        );

        themes.put("PYRAMID", pyramidTheme);

        // Save the themes to the dungeon.yml file
        saveThemeData();
    }

    /**
     * Create default biome mappings if none were loaded
     */
    private void createDefaultBiomeMappings() {
        plugin.getLogger().info("Creating default biome mappings");

        // Map major biomes to appropriate themes
        Map<Biome, String> specialMappings = new HashMap<>();
        specialMappings.put(Biome.DESERT, "PYRAMID");
        specialMappings.put(Biome.BADLANDS, "RUINS");
        specialMappings.put(Biome.ERODED_BADLANDS, "RUINS");

        // The default theme for all other biomes
        String defaultTheme = "RUINS";

        // Apply the mappings
        for (Biome biome : Biome.values()) {
            if (specialMappings.containsKey(biome)) {
                biomeThemeMap.put(biome, specialMappings.get(biome));
            } else {
                biomeThemeMap.put(biome, defaultTheme);
            }
        }

        // Save to the config
        saveThemeData();
    }

    /**
     * Get the theme for a specific biome
     */
    public DungeonTheme getThemeForBiome(Biome biome) {
        try {
            // First, try to get the theme from the main configuration
            String themeName = plugin.getConfig().getString("biome-themes." + biome.name());

            // If not found in main config, try dungeon config
            if (themeName == null) {
                ConfigurationSection dungeonConfig = configLoader.getConfig("dungeon");
                themeName = dungeonConfig.getString("biome-themes." + biome.name());
            }

            // If still not found, log a warning and use default
            if (themeName == null) {
                plugin.getLogger().warning("No theme found for biome: " + biome.name() + ". Using default.");
                themeName = "RUINS"; // Fallback default theme
            }

            // Get theme by name
            DungeonTheme theme = getThemeByName(themeName);

            // Additional logging for debugging
            if (theme == null) {
                plugin.getLogger().severe("Could not find theme: " + themeName + " for biome: " + biome.name());
                // Fallback to default theme
                theme = getDefaultTheme();
            }

            return theme;
        } catch (Exception e) {
            // Log the full exception for debugging
            plugin.getLogger().log(Level.SEVERE, "Error getting theme for biome: " + biome.name(), e);

            // Return default theme in case of any error
            return getDefaultTheme();
        }
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

    /**
     * Save theme data to the dungeon config file
     */
    public void saveThemeData() {
        // Get the dungeon config
        FileConfiguration dungeonConfig = this.configLoader.getConfig("dungeon");

        // Save themes
        for (DungeonTheme theme : themes.values()) {
            String path = "themes." + theme.getName() + ".";

            // Save block materials
            dungeonConfig.set(path + "primary-blocks", getMaterialNames(theme.getPrimaryBlocks()));
            dungeonConfig.set(path + "accent-blocks", getMaterialNames(theme.getAccentBlocks()));
            dungeonConfig.set(path + "floor-blocks", getMaterialNames(theme.getFloorBlocks()));
            dungeonConfig.set(path + "ceiling-blocks", getMaterialNames(theme.getCeilingBlocks()));
            dungeonConfig.set(path + "light-blocks", getMaterialNames(theme.getLightBlocks()));
        }

        // Save biome mappings
        for (Map.Entry<Biome, String> entry : biomeThemeMap.entrySet()) {
            dungeonConfig.set("biome-themes." + entry.getKey().name(), entry.getValue());
        }

        // Save the dungeon config
        this.configLoader.saveConfig("dungeon");
        plugin.getLogger().info("Saved theme data to dungeon.yml");
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

    /**
     * Get biome themes from configuration with robust error handling
     */
    public Map<Biome, String> getBiomeThemes() {
        Map<Biome, String> biomeThemes = new HashMap<>();

        try {
            // Try main configuration first
            ConfigurationSection mainBiomeThemes = plugin.getConfig().getConfigurationSection("biome-themes");
            if (mainBiomeThemes != null) {
                for (String biomeKey : mainBiomeThemes.getKeys(false)) {
                    try {
                        Biome biome = Biome.valueOf(biomeKey);
                        String theme = mainBiomeThemes.getString(biomeKey);
                        if (theme != null) {
                            biomeThemes.put(biome, theme);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid biome in configuration: " + biomeKey);
                    }
                }
            }

            // If no themes found, try dungeon configuration
            if (biomeThemes.isEmpty()) {
                ConfigurationSection dungeonConfig = configLoader.getConfig("dungeon");
                ConfigurationSection dungeonBiomeThemes = dungeonConfig.getConfigurationSection("biome-themes");

                if (dungeonBiomeThemes != null) {
                    for (String biomeKey : dungeonBiomeThemes.getKeys(false)) {
                        try {
                            Biome biome = Biome.valueOf(biomeKey);
                            String theme = dungeonBiomeThemes.getString(biomeKey);
                            if (theme != null) {
                                biomeThemes.put(biome, theme);
                            }
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid biome in dungeon configuration: " + biomeKey);
                        }
                    }
                }
            }

            // Log themes found
            plugin.getLogger().info("Loaded Biome Themes: " + biomeThemes);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading biome themes", e);
        }

        return biomeThemes;
    }

    /**
     * Get a configuration value with priority for dungeon config
     */
    public double getDouble(String path, double defaultValue) {
        // First, check dungeon configuration
        ConfigurationSection dungeonConfig = configLoader.getConfig("dungeon");
        if (dungeonConfig.contains(path)) {
            return dungeonConfig.getDouble(path, defaultValue);
        }

        // If not in dungeon config, check main config
        return plugin.getConfig().getDouble(path, defaultValue);
    }
}