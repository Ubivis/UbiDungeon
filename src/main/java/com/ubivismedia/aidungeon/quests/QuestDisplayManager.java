package com.ubivismedia.aidungeon.quests;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.storage.DungeonData;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the display of quest information in the player's HUD
 */
public class QuestDisplayManager {

    private final AIDungeonGenerator plugin;
    private final QuestSystem questSystem;

    // Map of player UUIDs to their active quest boss bars
    private final Map<UUID, Map<String, BossBar>> playerQuestBars = new ConcurrentHashMap<>();

    // Configuration for display settings
    private final boolean displayEnabled;
    private final int actionBarDuration;
    private final int maxDisplayedQuests;
    private final boolean bossBarEnabled;
    private final boolean showCompletedQuests;
    private final boolean actionBarEnabled;
    private final boolean soundProgressEnabled;
    private final boolean soundCompletionEnabled;

    public QuestDisplayManager(AIDungeonGenerator plugin, QuestSystem questSystem) {
        this.plugin = plugin;
        this.questSystem = questSystem;

        // Load configuration settings
        this.displayEnabled = plugin.getConfig().getBoolean("quests.display.enabled", true);
        this.maxDisplayedQuests = plugin.getConfig().getInt("quests.display.max_displayed_quests", 3);
        this.actionBarDuration = plugin.getConfig().getInt("quests.display.action_bar.duration", 60);
        this.bossBarEnabled = plugin.getConfig().getBoolean("quests.display.boss_bar.enabled", true);
        this.showCompletedQuests = plugin.getConfig().getBoolean("quests.display.boss_bar.show_completed", true);
        this.actionBarEnabled = plugin.getConfig().getBoolean("quests.display.action_bar.enabled", true);
        this.soundProgressEnabled = plugin.getConfig().getBoolean("quests.display.sound_effects.progress", true);
        this.soundCompletionEnabled = plugin.getConfig().getBoolean("quests.display.sound_effects.completion", true);

        // Start the update task to refresh quest displays if enabled
        if (displayEnabled) {
            startUpdateTask();
        }
    }

    /**
     * Start the task to periodically update quest displays
     */
    private void startUpdateTask() {
        // Run every 20 ticks (1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayerDisplays, 20L, 20L);
    }

    /**
     * Update quest displays for all online players
     */
    private void updateAllPlayerDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }

    /**
     * Update quest display for a specific player
     */
    public void updatePlayerDisplay(Player player) {
        // Skip if display is disabled
        if (!displayEnabled) {
            return;
        }

        UUID playerUuid = player.getUniqueId();

        // Get player's active quests
        List<Quest> quests = questSystem.getPlayerQuests(playerUuid);

        // Filter to show only non-claimed quests
        List<Quest> activeQuests = new ArrayList<>();
        for (Quest quest : quests) {
            if (!quest.isRewardClaimed()) {
                // If configured to not show completed quests, skip completed ones
                if (!showCompletedQuests && quest.isCompleted()) {
                    continue;
                }
                activeQuests.add(quest);
            }
        }

        // Sort quests by completion status and progress
        activeQuests.sort((a, b) -> {
            // Completed quests first
            if (a.isCompleted() && !b.isCompleted()) return -1;
            if (!a.isCompleted() && b.isCompleted()) return 1;

            // Then by progress percentage (descending)
            return Integer.compare(b.getCompletionPercentage(), a.getCompletionPercentage());
        });

        // Limit the number of displayed quests
        List<Quest> displayedQuests = activeQuests.size() <= maxDisplayedQuests ?
                activeQuests : activeQuests.subList(0, maxDisplayedQuests);

        // Skip boss bar updates if disabled
        if (!bossBarEnabled) {
            return;
        }

        // Get existing boss bars for this player
        Map<String, BossBar> questBars = playerQuestBars.computeIfAbsent(playerUuid, k -> new HashMap<>());

        // Create a set of quest IDs to track which ones to keep
        Set<String> activeQuestIds = new HashSet<>();

        // Update or create boss bars for displayed quests
        for (Quest quest : displayedQuests) {
            String questId = quest.getId();
            activeQuestIds.add(questId);

            BossBar bar = questBars.get(questId);
            if (bar == null) {
                // Create new boss bar for this quest
                bar = createQuestBossBar(quest);
                questBars.put(questId, bar);
                bar.addPlayer(player);
            } else {
                // Update existing boss bar
                updateQuestBossBar(bar, quest);
            }
        }

        // Remove boss bars for quests that are no longer active or displayed
        List<String> barsToRemove = new ArrayList<>();
        for (String questId : questBars.keySet()) {
            if (!activeQuestIds.contains(questId)) {
                BossBar bar = questBars.get(questId);
                bar.removePlayer(player);
                bar.setVisible(false);
                barsToRemove.add(questId);
            }
        }

        // Clean up removed bars
        for (String questId : barsToRemove) {
            questBars.remove(questId);
        }
    }

    /**
     * Create a new boss bar for a quest
     */
    private BossBar createQuestBossBar(Quest quest) {
        QuestTemplate template = quest.getTemplate();

        // Determine color based on quest type and completion
        BarColor color = getBarColorForQuest(quest);

        // Create title with quest name and progress
        String title = createQuestBarTitle(quest);

        // Create the boss bar
        BossBar bar = Bukkit.createBossBar(title, color, BarStyle.SOLID);

        // Set progress
        double progress = quest.getCompletionPercentage() / 100.0;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

        return bar;
    }

    /**
     * Update an existing boss bar for a quest
     */
    private void updateQuestBossBar(BossBar bar, Quest quest) {
        // Update title
        bar.setTitle(createQuestBarTitle(quest));

        // Update color
        bar.setColor(getBarColorForQuest(quest));

        // Update progress
        double progress = quest.getCompletionPercentage() / 100.0;
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    /**
     * Create the title text for a quest boss bar
     */
    private String createQuestBarTitle(Quest quest) {
        QuestTemplate template = quest.getTemplate();

        // Build the title with different formatting based on completion
        String title;
        if (quest.isCompleted()) {
            title = ChatColor.GREEN + "✓ " + ChatColor.WHITE + template.getName() +
                    ChatColor.GREEN + " - COMPLETED" + ChatColor.YELLOW + " (/quests claim " + quest.getId() + ")";
        } else {
            title = ChatColor.GOLD + template.getName() + ChatColor.WHITE +
                    " - " + quest.getProgress() + "/" + template.getRequiredAmount() +
                    " (" + quest.getCompletionPercentage() + "%)";
        }

        return title;
    }

    /**
     * Determine the appropriate color for a quest's boss bar
     */
    private BarColor getBarColorForQuest(Quest quest) {
        if (quest.isCompleted()) {
            return BarColor.GREEN;
        }

        // Color based on quest type
        switch (quest.getTemplate().getType()) {
            case KILL:
                return BarColor.RED;
            case COLLECT:
                return BarColor.YELLOW;
            case EXPLORE:
                return BarColor.BLUE;
            default:
                return BarColor.WHITE;
        }
    }

    /**
     * Show a temporary quest update message in the action bar
     */
    public void showQuestUpdateInActionBar(Player player, Quest quest, boolean completed) {
        // Skip if action bar is disabled or display is disabled
        if (!displayEnabled || !actionBarEnabled) {
            return;
        }

        QuestTemplate template = quest.getTemplate();

        String message;
        if (completed) {
            message = ChatColor.GREEN + "✓ Quest Completed: " + ChatColor.WHITE + template.getName() +
                    ChatColor.GREEN + " - Use /quests claim " + quest.getId() + " for rewards!";

            // Play completion sound if enabled
            if (soundCompletionEnabled) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        } else {
            message = ChatColor.GOLD + "Quest Progress: " + ChatColor.WHITE + template.getName() +
                    ChatColor.GOLD + " - " + quest.getProgress() + "/" + template.getRequiredAmount() +
                    " (" + quest.getCompletionPercentage() + "%)";

            // Play progress sound if enabled
            if (soundProgressEnabled) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            }
        }

        // Show in action bar
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    /**
     * Clean up quest displays for a player
     */
    public void cleanupPlayerDisplay(UUID playerUuid) {
        Map<String, BossBar> questBars = playerQuestBars.remove(playerUuid);

        if (questBars != null) {
            for (BossBar bar : questBars.values()) {
                bar.setVisible(false);

                // Remove all players
                for (Player player : bar.getPlayers()) {
                    bar.removePlayer(player);
                }
            }
        }
    }

    /**
     * Clean up all displays when the plugin is disabled
     */
    public void cleanupAllDisplays() {
        for (Map<String, BossBar> questBars : playerQuestBars.values()) {
            for (BossBar bar : questBars.values()) {
                bar.setVisible(false);

                // Remove all players
                for (Player player : bar.getPlayers()) {
                    bar.removePlayer(player);
                }
            }
        }

        playerQuestBars.clear();
    }

    /**
     * Show quest details to a player with visual formatting
     */
    public void showQuestDetails(Player player, Quest quest) {
        QuestTemplate template = quest.getTemplate();

        // Create a fancy quest detail message
        StringBuilder message = new StringBuilder();

        // Header
        message.append(ChatColor.GOLD).append("✦ ").append(ChatColor.BOLD).append("QUEST DETAILS")
                .append(ChatColor.GOLD).append(" ✦").append("\n");

        // Quest name
        message.append(ChatColor.YELLOW).append("Name: ").append(ChatColor.WHITE)
                .append(template.getName()).append("\n");

        // Description
        message.append(ChatColor.YELLOW).append("Description: ").append(ChatColor.GRAY)
                .append(template.getDescription()).append("\n");

        // Type
        message.append(ChatColor.YELLOW).append("Type: ").append(ChatColor.AQUA)
                .append(formatQuestType(template.getType())).append("\n");

        // Progress
        String progressColor = quest.isCompleted() ? ChatColor.GREEN.toString() : ChatColor.GOLD.toString();
        message.append(ChatColor.YELLOW).append("Progress: ").append(progressColor)
                .append(quest.getProgress()).append("/").append(template.getRequiredAmount())
                .append(" (").append(quest.getCompletionPercentage()).append("%)").append("\n");

        // Status
        if (quest.isCompleted()) {
            message.append(ChatColor.YELLOW).append("Status: ").append(ChatColor.GREEN)
                    .append("✓ COMPLETED").append(ChatColor.YELLOW).append(" - Use ")
                    .append(ChatColor.WHITE).append("/quests claim ").append(quest.getId())
                    .append(ChatColor.YELLOW).append(" to claim rewards!");
        } else {
            message.append(ChatColor.YELLOW).append("Status: ").append(ChatColor.GOLD)
                    .append("⧗ IN PROGRESS");
        }

        // Send message
        player.sendMessage(message.toString());

        // Play sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
    }

     /**
                * Format the quest type into a more friendly string
     */
    private String formatQuestType(QuestType type) {
        switch (type) {
            case KILL:
                return "Combat";
            case COLLECT:
                return "Collection";
            case EXPLORE:
                return "Exploration";
            default:
                return type.name();
        }
    }
    /**
     * Show quest notification as a title to a player
     */
    public void showQuestNotification(Player player, Quest quest, boolean isNew) {
        // Skip if display is disabled
        if (!displayEnabled) {
            return;
        }

        QuestTemplate template = quest.getTemplate();

        if (isNew) {
            // New quest notification
            player.sendTitle(
                    ChatColor.GOLD + "New Quest",
                    ChatColor.WHITE + template.getName(),
                    10, 40, 20
            );

            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        } else if (quest.isCompleted()) {
            // Completed quest notification
            player.sendTitle(
                    ChatColor.GREEN + "Quest Completed!",
                    ChatColor.WHITE + template.getName(),
                    10, 50, 20
            );

            // Play sound
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } else {
            // Progress update notification
            player.sendTitle(
                    ChatColor.GOLD + "Quest Progress",
                    ChatColor.WHITE.toString() + quest.getProgress() + "/" + template.getRequiredAmount(),
                    5, 30, 10
            );

            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        }
    }

    /**
     * Create a compass that points to quest objectives
     * @param player The player to give the compass to
     * @param quest The quest to track
     */
    public void createQuestTracker(Player player, Quest quest) {
        QuestTemplate template = quest.getTemplate();
        QuestType type = template.getType();

        // Get dungeon location
        String dungeonId = quest.getDungeonId();
        BiomeArea dungeonArea = getDungeonAreaFromId(dungeonId);

        if (dungeonArea == null) {
            player.sendMessage(ChatColor.RED + "Could not find the dungeon for this quest.");
            return;
        }

        // Create the compass item
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();

        if (meta != null) {
            // Set compass name
            meta.setDisplayName(ChatColor.GOLD + "Quest Tracker: " + ChatColor.WHITE + template.getName());

            // Add lore
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + template.getDescription());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE +
                    quest.getProgress() + "/" + template.getRequiredAmount() +
                    " (" + quest.getCompletionPercentage() + "%)");
            lore.add(ChatColor.YELLOW + "Type: " + ChatColor.AQUA + formatQuestType(type));
            lore.add("");
            lore.add(ChatColor.GRAY + "This compass points to your quest objective");
            meta.setLore(lore);

            // Store quest ID in compass metadata
            NamespacedKey questKey = new NamespacedKey(plugin, "quest_tracker");
            meta.getPersistentDataContainer().set(questKey, PersistentDataType.STRING, quest.getId());

            // Set compass to point to the dungeon
            if (meta instanceof CompassMeta) {
                CompassMeta compassMeta = (CompassMeta) meta;
                compassMeta.setLodestoneTracked(false);

                // Get suitable Y level
                int y = getSuitableY(Bukkit.getWorld(dungeonArea.getWorldName()),
                        dungeonArea.getCenterX(), dungeonArea.getCenterZ());

                // Set lodestone location
                Location targetLoc = new Location(
                        Bukkit.getWorld(dungeonArea.getWorldName()),
                        dungeonArea.getCenterX(),
                        y,
                        dungeonArea.getCenterZ()
                );
                compassMeta.setLodestone(targetLoc);
            }

            // Apply meta
            compass.setItemMeta(meta);

            // Give to player
            player.getInventory().addItem(compass);
            player.sendMessage(ChatColor.GREEN + "You received a Quest Tracker compass for: " +
                    ChatColor.WHITE + template.getName());
        }
    }

    /**
     * Find a suitable Y coordinate for the quest marker
     */
    private int getSuitableY(World world, int x, int z) {
        if (world == null) {
            return 64; // Default height if world not found
        }

        // Find the first non-air block from the top
        for (int y = world.getMaxHeight() - 10; y > 0; y--) {
            if (!world.getBlockAt(x, y, z).getType().isAir() &&
                    world.getBlockAt(x, y + 1, z).getType().isAir()) {
                return y + 1;
            }
        }

        return 64; // Default if no suitable Y found
    }

    /**
     * Get dungeon area from an ID
     */
    private BiomeArea getDungeonAreaFromId(String dungeonId) {
        String[] parts = dungeonId.split(":");
        if (parts.length < 3) {
            return null;
        }

        try {
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            // Get all dungeons
            Map<BiomeArea, DungeonData> dungeons = plugin.getDungeonManager().getAllDungeons();

            for (BiomeArea area : dungeons.keySet()) {
                if (area.getWorldName().equals(worldName) &&
                        area.getCenterX() == x &&
                        area.getCenterZ() == z) {
                    return area;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse dungeon ID: " + dungeonId);
        }

        return null;
    }
}