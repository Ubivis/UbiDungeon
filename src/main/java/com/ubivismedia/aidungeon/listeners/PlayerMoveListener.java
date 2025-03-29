package com.ubivismedia.aidungeon.listeners;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.BiomeTracker;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import com.ubivismedia.aidungeon.quests.QuestSystem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Listens for player movement to detect biome changes
 * and record exploration for dungeon discovery
 */
public class PlayerMoveListener implements Listener {

    private final AIDungeonGenerator plugin;
    private final BiomeTracker biomeTracker;
    private final DungeonManager dungeonManager;
    private final QuestSystem questSystem;

    // Track chunks that have been recorded to avoid redundant processing
    private final Set<String> recordedChunks = new HashSet<>();

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

        // Track exploration in the current chunk
        trackExplorationAtLocation(player, player.getLocation());

        // Check if player has entered a new biome area - primarily for quest generation
        BiomeArea newArea = biomeTracker.checkPlayerBiomeChange(player);

        // If a new biome area was detected
        if (newArea != null) {
            // Check if entering an existing dungeon (for quest generation)
            BiomeArea existingDungeon = dungeonManager.getDungeonAreaAtLocation(player.getLocation());
            if (existingDungeon != null) {
                // Generate quest when entering a dungeon
                questSystem.generateQuestForPlayer(player, existingDungeon);
            }
        }
    }

    /**
     * Track exploration at the given location and surrounding chunks
     */
    private void trackExplorationAtLocation(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Get current chunk coordinates
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        // Track exploration in current chunk and surrounding chunks
        int radius = 1; // Track current chunk and immediate neighbors

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Calculate chunk coordinates
                int targetChunkX = chunkX + x;
                int targetChunkZ = chunkZ + z;

                // Create unique key for this chunk
                String chunkKey = world.getName() + ":" + targetChunkX + ":" + targetChunkZ;

                // Skip if we've already recorded this chunk recently
                if (recordedChunks.contains(chunkKey)) {
                    continue;
                }

                // Add to recorded chunks
                recordedChunks.add(chunkKey);

                // Keep recorded chunks set from growing too large
                if (recordedChunks.size() > 1000) {
                    recordedChunks.clear();
                }

                // Get center block position of this chunk
                int blockX = targetChunkX << 4;
                int blockZ = targetChunkZ << 4;

                // Sample the biome at multiple Y levels to handle overlapping biomes
                Biome primaryBiome = null;
                Map<Biome, Integer> biomeCounts = new HashMap<>();

                // Check multiple Y levels - ensure we capture the right biome
                int[] yLevels = {60, 80, 100, 120, 40, 20}; // Check common heights first

                for (int y : yLevels) {
                    // Skip if out of world bounds
                    if (y < world.getMinHeight() || y > world.getMaxHeight()) {
                        continue;
                    }

                    Biome biome = world.getBiome(blockX, y, blockZ);
                    biomeCounts.put(biome, biomeCounts.getOrDefault(biome, 0) + 1);

                    // If this is the first Y level or if this biome is seen more often
                    if (primaryBiome == null ||
                            biomeCounts.get(biome) > biomeCounts.get(primaryBiome)) {
                        primaryBiome = biome;
                    }
                }

                // If we found a biome, record exploration
                if (primaryBiome != null) {
                    plugin.getBiomeExplorationTracker().recordExploredChunk(
                            player,
                            world,
                            primaryBiome
                    );
                }
            }
        }
    }

    /**
     * Handle the debug command for checking exploration
     */
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // Debug command for checking exploration percentages
        if (event.getMessage().startsWith("/aidebug explore")) {
            if (event.getPlayer().hasPermission("aidungeon.admin")) {
                Player player = event.getPlayer();
                World world = player.getWorld();
                Biome biome = world.getBiome(
                        player.getLocation().getBlockX(),
                        player.getLocation().getBlockY(),
                        player.getLocation().getBlockZ()
                );

                // Get debug info
                String debugInfo = plugin.getBiomeExplorationTracker().getDebugInfo(player, world, biome);
                player.sendMessage("§7[Debug] " + debugInfo);

                // Show generation info
                boolean hasDungeon = plugin.getBiomeExplorationTracker().hasDungeonBeenGenerated(world, biome);
                double threshold = plugin.getConfig().getDouble("discovery.exploration-threshold", 0.1);

                player.sendMessage("§7[Debug] Dungeon generated: " + hasDungeon +
                        ", Threshold: " + (threshold * 100) + "%");

                // Cancel the event so it doesn't get processed by other handlers
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Clear any tracking data for this player
        // This will force a new biome check on their first movement
        biomeTracker.clearPlayer(event.getPlayer().getUniqueId());

        // Also clear exploration checker data
        plugin.getExplorationChecker().clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear tracking data to save memory
        biomeTracker.clearPlayer(event.getPlayer().getUniqueId());
        plugin.getBiomeExplorationTracker().resetPlayerData(event.getPlayer().getUniqueId());
        plugin.getExplorationChecker().clearPlayer(event.getPlayer().getUniqueId());
    }
}