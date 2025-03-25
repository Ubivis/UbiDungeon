package com.ubivismedia.aidungeon.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a region of the world containing one primary biome
 * where a dungeon can be generated
 */
public class BiomeArea {
    
    private final String worldName;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final Biome primaryBiome;
    private final String uniqueId;
    
    /**
     * Create a new BiomeArea
     */
    public BiomeArea(String worldName, int centerX, int centerZ, int radius, Biome primaryBiome) {
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.primaryBiome = primaryBiome;
        this.uniqueId = worldName + ":" + centerX + ":" + centerZ;
    }
    
    /**
     * Create a BiomeArea from a Location
     */
    public static BiomeArea fromLocation(Location location, int radius) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location must have a valid world");
        }
        
        int centerX = location.getBlockX();
        int centerZ = location.getBlockZ();
        Biome biome = world.getBiome(centerX, location.getBlockY(), centerZ);
        
        return new BiomeArea(world.getName(), centerX, centerZ, radius, biome);
    }
    
    /**
     * Determine if a location is within this biome area
     */
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        return Math.abs(x - centerX) <= radius && Math.abs(z - centerZ) <= radius;
    }
    
    /**
     * Get the center location of this area
     */
    public Location getCenterLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("World not found: " + worldName);
        }
        
        return new Location(world, centerX, 64, centerZ);
    }
    
    /**
     * Get the world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Get the center X coordinate
     */
    public int getCenterX() {
        return centerX;
    }
    
    /**
     * Get the center Z coordinate
     */
    public int getCenterZ() {
        return centerZ;
    }
    
    /**
     * Get the radius
     */
    public int getRadius() {
        return radius;
    }
    
    /**
     * Get the primary biome
     */
    public Biome getPrimaryBiome() {
        return primaryBiome;
    }
    
    /**
     * Get the unique ID of this area
     */
    public String getUniqueId() {
        return uniqueId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiomeArea biomeArea = (BiomeArea) o;
        return centerX == biomeArea.centerX &&
                centerZ == biomeArea.centerZ &&
                radius == biomeArea.radius &&
                Objects.equals(worldName, biomeArea.worldName) &&
                primaryBiome == biomeArea.primaryBiome;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(worldName, centerX, centerZ, radius, primaryBiome);
    }
    
    @Override
    public String toString() {
        return "BiomeArea{" +
                "world='" + worldName + '\'' +
                ", center=[" + centerX + "," + centerZ + "]" +
                ", biome=" + primaryBiome +
                '}';
    }
}
