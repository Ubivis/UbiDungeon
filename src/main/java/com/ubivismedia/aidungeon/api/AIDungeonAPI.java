package com.ubivismedia.aidungeon.api;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.ItemHelper;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * API for other plugins to interact with AIDungeonGenerator
 */
public class AIDungeonAPI {

    private final AIDungeonGenerator plugin;

    public AIDungeonAPI(AIDungeonGenerator plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a dungeon compass for a specific dungeon
     * @param dungeonId The ID of the dungeon (worldName:x:z)
     * @return The compass ItemStack, or null if dungeon not found
     */
    public ItemStack getDungeonCompass(String dungeonId) {
        String[] parts = dungeonId.split(":");
        if (parts.length != 3) {
            return null;
        }

        String worldName = parts[0];
        int x, z;

        try {
            x = Integer.parseInt(parts[1]);
            z = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        // Create a temporary BiomeArea to create compass
        BiomeArea area = new BiomeArea(worldName, x, z, 100, world.getBiome(x, 64, z));
        return ItemHelper.createDungeonCompass(plugin, area);
    }

    /**
     * Get a compass to the nearest dungeon to a player
     * @param player The player
     * @return The compass ItemStack, or null if no dungeons found
     */
    public ItemStack getNearestDungeonCompass(Player player) {
        BiomeArea nearestDungeon = getNearestDungeon(player);
        if (nearestDungeon == null) {
            return null;
        }

        return ItemHelper.createDungeonCompass(plugin, nearestDungeon);
    }

    /**
     * Get a compass to a random dungeon
     * @return The compass ItemStack, or null if no dungeons found
     */
    public ItemStack getRandomDungeonCompass() {
        List<BiomeArea> dungeons = plugin.getDungeonManager().getAllDungeons().keySet()
                .stream()
                .toList();

        if (dungeons.isEmpty()) {
            return null;
        }

        // Select a random dungeon
        BiomeArea randomDungeon = dungeons.get((int)(Math.random() * dungeons.size()));
        return ItemHelper.createDungeonCompass(plugin, randomDungeon);
    }

    /**
     * Get a compass to a random dungeon of a specific biome type
     * @param biome The biome type
     * @return The compass ItemStack, or null if no dungeons of that biome found
     */
    public ItemStack getBiomeDungeonCompass(Biome biome) {
        List<BiomeArea> dungeons = plugin.getDungeonManager().getAllDungeons().keySet()
                .stream()
                .filter(area -> area.getPrimaryBiome() == biome)
                .toList();

        if (dungeons.isEmpty()) {
            return null;
        }

        // Select a random dungeon of the specified biome
        BiomeArea randomDungeon = dungeons.get((int)(Math.random() * dungeons.size()));
        return ItemHelper.createDungeonCompass(plugin, randomDungeon);
    }

    /**
     * Get the nearest dungeon to a player
     * @param player The player
     * @return The nearest dungeon BiomeArea, or null if none found
     */
    public BiomeArea getNearestDungeon(Player player) {
        BiomeArea nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (BiomeArea area : plugin.getDungeonManager().getAllDungeons().keySet()) {
            if (!area.getWorldName().equals(player.getWorld().getName())) {
                continue;
            }

            double distance = Math.sqrt(
                    Math.pow(area.getCenterX() - player.getLocation().getX(), 2) +
                            Math.pow(area.getCenterZ() - player.getLocation().getZ(), 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearest = area;
            }
        }

        return nearest;
    }

    /**
     * Get all dungeons
     * @return A list of all dungeon BiomeAreas
     */
    public List<BiomeArea> getAllDungeons() {
        return plugin.getDungeonManager().getAllDungeons().keySet()
                .stream()
                .toList();
    }
}