package com.ubivismedia.aidungeon.integration;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.quests.Quest;
import com.ubivismedia.aidungeon.quests.QuestSystem;
import com.ubivismedia.aidungeon.storage.DungeonData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for AI Dungeon Generator
 */
public class PlaceholderManager extends PlaceholderExpansion {

    private final AIDungeonGenerator plugin;
    
    public PlaceholderManager(AIDungeonGenerator plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "aidungeon";
    }
    
    @Override
    public String getAuthor() {
        return "UbivisMedia";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Persist across server reloads
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        // Check if the player parameter is needed for this placeholder
        if (player == null && (
                identifier.startsWith("player_") || 
                identifier.startsWith("quest_") || 
                identifier.startsWith("nearest_"))) {
            return null;
        }
        
        // General statistics
        if (identifier.equals("total_dungeons")) {
            return String.valueOf(getTotalDungeons());
        }
        
        // Player-specific statistics
        if (player != null) {
            // Completed dungeons count
            if (identifier.equals("player_completed_dungeons")) {
                return String.valueOf(getPlayerCompletedDungeons(player.getUniqueId()));
            }
            
            // Active quests count
            if (identifier.equals("player_active_quests")) {
                return String.valueOf(getPlayerActiveQuests(player.getUniqueId()));
            }
            
            // Completed quests count
            if (identifier.equals("player_completed_quests")) {
                return String.valueOf(getPlayerCompletedQuests(player.getUniqueId()));
            }
            
            // Nearest dungeon info
            if (identifier.startsWith("nearest_dungeon_")) {
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    String subIdentifier = identifier.substring("nearest_dungeon_".length());
                    return getNearestDungeonInfo(onlinePlayer, subIdentifier);
                }
                return "Unknown";
            }
            
            // Specific quest info
            if (identifier.startsWith("quest_")) {
                String[] parts = identifier.split("_", 3);
                if (parts.length >= 3) {
                    int index = 0;
                    try {
                        index = Integer.parseInt(parts[1]) - 1; // Convert to 0-based index
                        if (index < 0) index = 0;
                    } catch (NumberFormatException e) {
                        return "Invalid quest index";
                    }
                    
                    return getQuestInfo(player.getUniqueId(), index, parts[2]);
                }
            }
        }
        
        // Specific dungeon info
        if (identifier.startsWith("dungeon_")) {
            String[] parts = identifier.split("_", 3);
            if (parts.length >= 3) {
                int index = 0;
                try {
                    index = Integer.parseInt(parts[1]) - 1; // Convert to 0-based index
                    if (index < 0) index = 0;
                } catch (NumberFormatException e) {
                    return "Invalid dungeon index";
                }
                
                return getDungeonInfo(index, parts[2]);
            }
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Get the total number of generated dungeons
     */
    private int getTotalDungeons() {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        return dungeonManager.getAllDungeons().size();
    }
    
    /**
     * Get the number of completed dungeons for a player
     */
    private int getPlayerCompletedDungeons(UUID playerUuid) {
        QuestSystem questSystem = plugin.getQuestSystem();
        int count = 0;
        
        List<Quest> quests = questSystem.getPlayerQuests(playerUuid);
        for (Quest quest : quests) {
            if (quest.isCompleted() && quest.getTemplate().getType().name().equals("BOSS")) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Get the number of active quests for a player
     */
    private int getPlayerActiveQuests(UUID playerUuid) {
        QuestSystem questSystem = plugin.getQuestSystem();
        int count = 0;
        
        List<Quest> quests = questSystem.getPlayerQuests(playerUuid);
        for (Quest quest : quests) {
            if (!quest.isCompleted() && !quest.isRewardClaimed()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Get the number of completed quests for a player
     */
    private int getPlayerCompletedQuests(UUID playerUuid) {
        QuestSystem questSystem = plugin.getQuestSystem();
        int count = 0;
        
        List<Quest> quests = questSystem.getPlayerQuests(playerUuid);
        for (Quest quest : quests) {
            if (quest.isCompleted()) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Get information about a specific quest
     */
    private String getQuestInfo(UUID playerUuid, int index, String infoType) {
        QuestSystem questSystem = plugin.getQuestSystem();
        List<Quest> quests = questSystem.getPlayerQuests(playerUuid);
        
        if (index >= quests.size()) {
            return "N/A";
        }
        
        Quest quest = quests.get(index);
        
        switch (infoType.toLowerCase()) {
            case "name":
                return quest.getTemplate().getName();
            case "description":
                return quest.getTemplate().getDescription();
            case "type":
                return quest.getTemplate().getType().name();
            case "progress":
                return quest.getProgress() + "/" + quest.getTemplate().getRequiredAmount();
            case "percentage":
                return quest.getCompletionPercentage() + "%";
            case "completed":
                return quest.isCompleted() ? "Yes" : "No";
            case "dungeon":
                return quest.getDungeonId();
            default:
                return "Unknown info type";
        }
    }
    
    /**
     * Get information about a specific dungeon
     */
    private String getDungeonInfo(int index, String infoType) {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        if (index >= dungeons.size()) {
            return "N/A";
        }
        
        // Get the dungeon at specified index
        BiomeArea area = (BiomeArea) dungeons.keySet().toArray()[index];
        DungeonData data = dungeons.get(area);
        
        switch (infoType.toLowerCase()) {
            case "world":
                return area.getWorldName();
            case "biome":
                return area.getPrimaryBiome().name();
            case "x":
                return String.valueOf(area.getCenterX());
            case "y":
                // Find a suitable Y level for the entrance
                int y = getSuitableY(Bukkit.getWorld(area.getWorldName()), area.getCenterX(), area.getCenterZ());
                return String.valueOf(y);
            case "z":
                return String.valueOf(area.getCenterZ());
            case "coords":
                int entranceY = getSuitableY(Bukkit.getWorld(area.getWorldName()), area.getCenterX(), area.getCenterZ());
                return area.getCenterX() + "," + entranceY + "," + area.getCenterZ();
            case "theme":
                return data.getTheme().getName();
            case "age":
                // Calculate age in days
                long ageMs = System.currentTimeMillis() - data.getTimestamp();
                long days = ageMs / (1000 * 60 * 60 * 24);
                return String.valueOf(days);
            case "discoverer":
                OfflinePlayer discoverer = Bukkit.getOfflinePlayer(data.getDiscovererUUID());
                return discoverer.getName() != null ? discoverer.getName() : "Unknown";
            default:
                return "Unknown info type";
        }
    }
    
    /**
     * Get information about the nearest dungeon to a player
     */
    private String getNearestDungeonInfo(Player player, String infoType) {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        if (dungeons.isEmpty()) {
            return "No dungeons";
        }
        
        // Find nearest dungeon
        BiomeArea nearestArea = null;
        double minDistance = Double.MAX_VALUE;
        
        for (BiomeArea area : dungeons.keySet()) {
            if (!area.getWorldName().equals(player.getWorld().getName())) {
                continue;
            }
            
            double distance = Math.sqrt(
                    Math.pow(area.getCenterX() - player.getLocation().getX(), 2) +
                    Math.pow(area.getCenterZ() - player.getLocation().getZ(), 2)
            );
            
            if (distance < minDistance) {
                minDistance = distance;
                nearestArea = area;
            }
        }
        
        if (nearestArea == null) {
            return "No dungeons in world";
        }
        
        // Get dungeon data
        DungeonData data = dungeons.get(nearestArea);
        
        switch (infoType.toLowerCase()) {
            case "world":
                return nearestArea.getWorldName();
            case "biome":
                return nearestArea.getPrimaryBiome().name();
            case "x":
                return String.valueOf(nearestArea.getCenterX());
            case "z":
                return String.valueOf(nearestArea.getCenterZ());
            case "distance":
                return String.format("%.1f", minDistance);
            case "coords":
                int entranceY = getSuitableY(player.getWorld(), nearestArea.getCenterX(), nearestArea.getCenterZ());
                return nearestArea.getCenterX() + "," + entranceY + "," + nearestArea.getCenterZ();
            case "theme":
                return data.getTheme().getName();
            default:
                return "Unknown info type";
        }
    }
    
    /**
     * Find a suitable Y coordinate for dungeon entrance
     */
    private int getSuitableY(org.bukkit.World world, int x, int z) {
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
}
