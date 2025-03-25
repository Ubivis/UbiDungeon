package com.ubivismedia.aidungeon.listeners;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.BiomeTracker;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens for player movement to detect biome changes
 * and trigger dungeon discovery
 */
public class PlayerMoveListener implements Listener {
    
    private final AIDungeonGenerator plugin;
    private final BiomeTracker biomeTracker;
    private final DungeonManager dungeonManager;
    
    public PlayerMoveListener(AIDungeonGenerator plugin, BiomeTracker biomeTracker, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.biomeTracker = biomeTracker;
        this.dungeonManager = dungeonManager;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip if only pitch/yaw changed
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() 
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Skip if player doesn't have permission
        if (!player.hasPermission("aidungeon.discover")) {
            return;
        }
        
        // Check if player has entered a new biome area
        BiomeArea newArea = biomeTracker.checkPlayerBiomeChange(player);
        
        // If a new biome area was detected
        if (newArea != null) {
            // Check if we should generate a dungeon here
            if (dungeonManager.canGenerateDungeon(player, newArea)) {
                // Queue dungeon generation
                dungeonManager.queueDungeonGeneration(newArea, player);
                
                // Debug message to player if enabled
                if (plugin.getConfig().getBoolean("settings.debug-mode", false)) {
                    player.sendMessage("§7[Debug] Dungeon generation queued in " 
                            + newArea.getPrimaryBiome() + " at " 
                            + newArea.getCenterX() + "," + newArea.getCenterZ());
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Clear any tracking data for this player
        // This will force a new biome check on their first movement
        biomeTracker.clearPlayer(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear tracking data to save memory
        biomeTracker.clearPlayer(event.getPlayer().getUniqueId());
    }
}
