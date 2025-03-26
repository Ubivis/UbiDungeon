package com.ubivismedia.aidungeon.dungeons;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for creating and managing items
 */
public class ItemHelper {
    
    /**
     * Create and give a dungeon compass to a player
     */
    public static void giveDungeonCompass(AIDungeonGenerator plugin, Player player, BiomeArea area) {
        LanguageManager lang = plugin.getLanguageManager();
        ItemStack compass = createDungeonCompass(plugin, area);
        
        // Add to player inventory or drop at player location
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(compass);
        } else {
            player.getWorld().dropItem(player.getLocation(), compass);
            player.sendMessage(lang.getMessage("compass.inventory_full"));
        }
    }

    /**
     * Find a suitable Y coordinate for the dungeon entrance
     * @param world The world to check
     * @param x X coordinate
     * @param z Z coordinate
     * @return The Y coordinate for the dungeon entrance
     */
    private static int findSuitableY(World world, int x, int z) {
        if (world == null) {
            return 64; // Default height if world not found
        }

        // Start from height 50, then work upwards to find the first non-air block
        for (int y = 40; y < world.getMaxHeight() - 10; y++) {
            if (world.getBlockAt(x, y, z).getType().isSolid() &&
                    world.getBlockAt(x, y + 1, z).getType().isAir()) {
                return y + 1; // Return the first air block above solid ground
            }
        }

        return 64; // Default if no suitable location found
    }

    /**
     * Create a compass that points to a dungeon
     */
    public static ItemStack createDungeonCompass(AIDungeonGenerator plugin, BiomeArea area) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();

        if (meta != null) {

            LanguageManager lang = plugin.getLanguageManager();
            // Set target location
            meta.setLodestoneTracked(false);

            // Get a world instance
            World world = Bukkit.getWorld(area.getWorldName());
            if (world != null) {
                // Find suitable Y level
                int y = findSuitableY(world, area.getCenterX(), area.getCenterZ());

                // Create location object for the dungeon entrance
                Location lodestoneLocation = new Location(world, area.getCenterX(), y, area.getCenterZ());

                // Set the lodestone location for the compass
                meta.setLodestone(lodestoneLocation);
            }

            // Set display name
            meta.setDisplayName(lang.getMessage("compass.name"));

            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add(lang.getMessage("compass.lore.biome", area.getPrimaryBiome().toString()));
            lore.add(lang.getMessage("compass.lore.location", area.getCenterX(), area.getCenterZ()));
            meta.setLore(lore);

            // Add persistent data
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey worldKey = new NamespacedKey(plugin, "dungeon_world");
            NamespacedKey xKey = new NamespacedKey(plugin, "dungeon_x");
            NamespacedKey zKey = new NamespacedKey(plugin, "dungeon_z");

            container.set(worldKey, PersistentDataType.STRING, area.getWorldName());
            container.set(xKey, PersistentDataType.INTEGER, area.getCenterX());
            container.set(zKey, PersistentDataType.INTEGER, area.getCenterZ());

            // Add item flags
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            compass.setItemMeta(meta);
        }

        return compass;
    }
    
    /**
     * Check if an item is a dungeon compass
     */
    public static boolean isDungeonCompass(AIDungeonGenerator plugin, ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey worldKey = new NamespacedKey(plugin, "dungeon_world");
        
        return container.has(worldKey, PersistentDataType.STRING);
    }
    
    /**
     * Get the dungeon area from a compass
     */
    @SuppressWarnings("unused")
    public static BiomeArea getDungeonAreaFromCompass(AIDungeonGenerator plugin, ItemStack compass) {
        if (!isDungeonCompass(plugin, compass)) {
            return null;
        }
        
        ItemMeta meta = compass.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey worldKey = new NamespacedKey(plugin, "dungeon_world");
        NamespacedKey xKey = new NamespacedKey(plugin, "dungeon_x");
        NamespacedKey zKey = new NamespacedKey(plugin, "dungeon_z");
        
        String worldName = container.get(worldKey, PersistentDataType.STRING);
        Integer x = container.get(xKey, PersistentDataType.INTEGER);
        Integer z = container.get(zKey, PersistentDataType.INTEGER);
        
        if (worldName == null || x == null || z == null) {
            return null;
        }
        
        // We don't know the biome here, but that's okay for navigation
        return new BiomeArea(worldName, x, z, 100, null);
    }
}
