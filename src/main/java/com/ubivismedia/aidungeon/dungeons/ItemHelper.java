package com.ubivismedia.aidungeon.dungeons;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
        ItemStack compass = createDungeonCompass(plugin, area);
        
        // Add to player inventory or drop at player location
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(compass);
        } else {
            player.getWorld().dropItem(player.getLocation(), compass);
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full, so the dungeon compass was dropped at your feet.");
        }
    }
    
    /**
     * Create a compass that points to a dungeon
     */
    public static ItemStack createDungeonCompass(AIDungeonGenerator plugin, BiomeArea area) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        
        if (meta != null) {
            // Set target location
            meta.setLodestoneTracked(false);
            
            // Set display name
            meta.setDisplayName(ChatColor.GOLD + "Dungeon Compass");
            
            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Points to a dungeon in a");
            lore.add(ChatColor.GRAY + area.getPrimaryBiome().toString() + " biome");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + 
                    area.getCenterX() + ", " + area.getCenterZ());
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
