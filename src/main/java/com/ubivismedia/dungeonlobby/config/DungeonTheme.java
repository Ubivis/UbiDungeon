package com.ubivismedia.aidungeon.config;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a dungeon theme with associated block types and decorations
 */
public class DungeonTheme {
    
    private final String name;
    private final List<Material> primaryBlocks;
    private final List<Material> accentBlocks;
    private final List<Material> floorBlocks;
    private final List<Material> ceilingBlocks;
    private final List<Material> lightBlocks;
    
    /**
     * Create a new dungeon theme
     */
    public DungeonTheme(String name, 
                        List<Material> primaryBlocks, 
                        List<Material> accentBlocks, 
                        List<Material> floorBlocks, 
                        List<Material> ceilingBlocks, 
                        List<Material> lightBlocks) {
        this.name = name;
        this.primaryBlocks = new ArrayList<>(primaryBlocks);
        this.accentBlocks = new ArrayList<>(accentBlocks);
        this.floorBlocks = new ArrayList<>(floorBlocks);
        this.ceilingBlocks = new ArrayList<>(ceilingBlocks);
        this.lightBlocks = new ArrayList<>(lightBlocks);
    }
    
    /**
     * Get the theme name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get primary building blocks for this theme
     */
    public List<Material> getPrimaryBlocks() {
        return Collections.unmodifiableList(primaryBlocks);
    }
    
    /**
     * Get accent blocks for this theme
     */
    public List<Material> getAccentBlocks() {
        return Collections.unmodifiableList(accentBlocks);
    }
    
    /**
     * Get floor blocks for this theme
     */
    public List<Material> getFloorBlocks() {
        return Collections.unmodifiableList(floorBlocks);
    }
    
    /**
     * Get ceiling blocks for this theme
     */
    public List<Material> getCeilingBlocks() {
        return Collections.unmodifiableList(ceilingBlocks);
    }
    
    /**
     * Get light source blocks for this theme
     */
    public List<Material> getLightBlocks() {
        return Collections.unmodifiableList(lightBlocks);
    }
    
    @Override
    public String toString() {
        return "DungeonTheme{" + name + "}";
    }
}
