package com.ubivismedia.aidungeon.listeners;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.BiomeTracker;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import com.ubivismedia.aidungeon.quests.QuestSystem;
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
    private final QuestSystem questSystem;
    
    public PlayerMoveListener(AIDungeonGenerator plugin, BiomeTracker biomeTracker, 
                             DungeonManager dungeonManager, QuestSystem questSystem) {
        this.plugin = plugin;
        this.biomeTracker = biomeTracker;
        this.dungeonManager = dungeonManager;
        this.questSystem = questSystem;
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
            LanguageManager lang = plugin.getLanguageManager();
            // Track biome exploration
            double explorationPercentage = plugin.getBiomeExplorationTracker().recordExploredChunk(
                    player,
                    player.getWorld(),
                    newArea.getPrimaryBiome()
            );

            // Get configuration values
            double threshold = plugin.getConfig().getDouble("discovery.exploration-threshold", 0.1);
            boolean debug = plugin.getConfig().getBoolean("settings.debug-mode", false);

            // Log debug information if enabled
            if (debug) {
                player.sendMessage("§7[Debug] Biome: " + newArea.getPrimaryBiome() +
                        ", Exploration: " + String.format("%.1f", explorationPercentage * 100) + "%" +
                        ", Generated: " + plugin.getBiomeExplorationTracker().hasDungeonBeenGenerated(
                        player.getWorld(), newArea.getPrimaryBiome()));
            }

            // Check if we've explored enough of this biome and no dungeon has been generated yet
            boolean shouldGenerateDungeon = explorationPercentage >= threshold &&
                    !plugin.getBiomeExplorationTracker().hasDungeonBeenGenerated(
                            player.getWorld(),
                            newArea.getPrimaryBiome()
                    );

            // Check if we should generate a dungeon
            if (shouldGenerateDungeon && dungeonManager.canGenerateDungeon(player, newArea)) {
                // Queue dungeon generation
                dungeonManager.queueDungeonGeneration(newArea, player);

                // Mark this biome as having a dungeon
                plugin.getBiomeExplorationTracker().markDungeonGenerated(
                        player.getWorld(),
                        newArea.getPrimaryBiome()
                );

                // Debug message to player if enabled
                if (debug) {
                    player.sendMessage(lang.getMessage("dungeon.debug_generation",
                            newArea.getPrimaryBiome(),
                            newArea.getCenterX(),
                            newArea.getCenterZ(),
                            String.format("%.1f", explorationPercentage * 100)
                    ));
                }
            } else {
                // Check if entering an existing dungeon
                BiomeArea existingDungeon = dungeonManager.getDungeonAreaAtLocation(player.getLocation());
                if (existingDungeon != null) {
                    // Generate quest when entering a dungeon
                    questSystem.generateQuestForPlayer(player, existingDungeon);
                }

                // Debug why dungeon wasn't generated if in debug mode
                if (debug && explorationPercentage >= threshold) {
                    if (plugin.getBiomeExplorationTracker().hasDungeonBeenGenerated(player.getWorld(), newArea.getPrimaryBiome())) {
                        player.sendMessage("§7[Debug] Dungeon already exists for biome " + newArea.getPrimaryBiome());
                    } else if (!dungeonManager.canGenerateDungeon(player, newArea)) {
                        player.sendMessage("§7[Debug] Cannot generate dungeon: too close to existing dungeon or other restriction");
                    }
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
        plugin.getBiomeExplorationTracker().resetPlayerData(event.getPlayer().getUniqueId());
    }
}
