package com.ubivismedia.aidungeon.quests;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.storage.DungeonData;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles the quest system for AI dungeons
 */
public class QuestSystem implements Listener {

    private final AIDungeonGenerator plugin;
    private final Random random = new Random();

    // Quest related keys for persistent data
    private final NamespacedKey questItemKey;
    private final NamespacedKey questKillKey;
    private final NamespacedKey questChestKey;
    
    // Active quests per player
    private final Map<UUID, Map<String, Quest>> playerQuests = new ConcurrentHashMap<>();
    
    // Quest templates
    private final List<QuestTemplate> questTemplates = new ArrayList<>();

    // Quest Display Manager
    private QuestDisplayManager displayManager;
    
    public QuestSystem(AIDungeonGenerator plugin) {
        this.plugin = plugin;

        // Initialize keys
        this.questItemKey = new NamespacedKey(plugin, "quest_item");
        this.questKillKey = new NamespacedKey(plugin, "quest_kill");
        this.questChestKey = new NamespacedKey(plugin, "quest_chest");
        
        // Load quest templates
        loadQuestTemplates();

        // Initialize display manager
        this.displayManager = new QuestDisplayManager(plugin, this);

        // Load player quests from storage
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::loadPlayerQuests);
    }
    
    /**
     * Load quest templates from configuration
     */
    public void loadQuestTemplates() {
        questTemplates.clear();
        
        // Check if quest configuration section exists
        if (!plugin.getConfig().isConfigurationSection("quests.templates")) {
            createDefaultQuestTemplates();
            return;
        }
        
        // Load from config
        for (String key : plugin.getConfig().getConfigurationSection("quests.templates").getKeys(false)) {
            try {
                String basePath = "quests.templates." + key + ".";
                
                // Load basic info
                String name = plugin.getConfig().getString(basePath + "name", "Unknown Quest");
                String description = plugin.getConfig().getString(basePath + "description", "No description");
                QuestType type = QuestType.valueOf(plugin.getConfig().getString(basePath + "type", "KILL"));
                
                // Load requirements
                int requiredAmount = plugin.getConfig().getInt(basePath + "required_amount", 1);
                String targetEntityType = plugin.getConfig().getString(basePath + "target_entity", "ZOMBIE");
                String targetItemType = plugin.getConfig().getString(basePath + "target_item", "DIAMOND");
                
                // Load rewards
                List<String> rewardCommands = plugin.getConfig().getStringList(basePath + "reward_commands");
                List<String> rewardMessages = plugin.getConfig().getStringList(basePath + "reward_messages");
                List<String> rewardItems = plugin.getConfig().getStringList(basePath + "reward_items");
                
                // Create template
                QuestTemplate template = new QuestTemplate(
                        key, name, description, type, requiredAmount,
                        targetEntityType, targetItemType, rewardCommands,
                        rewardMessages, rewardItems
                );
                
                questTemplates.add(template);
                plugin.getLogger().info("Loaded quest template: " + key);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading quest template: " + key, e);
            }
        }
        
        // If no templates were loaded, create defaults
        if (questTemplates.isEmpty()) {
            createDefaultQuestTemplates();
        }
    }
    
    /**
     * Create default quest templates
     */
    private void createDefaultQuestTemplates() {
        plugin.getLogger().info("Creating default quest templates");
        
        // KILL_BOSS quest
        QuestTemplate bossQuest = new QuestTemplate(
                "boss_slayer",
                "Boss Slayer",
                "Defeat the dungeon boss to claim your reward",
                QuestType.KILL,
                1,
                "BOSS", // Special marker for boss mobs
                "",
                Arrays.asList("give %player% diamond 5", "xp add %player% 500"),
                Arrays.asList("You have defeated the dungeon boss!", "The cursed spirits can now rest."),
                Arrays.asList("DIAMOND:5", "EXPERIENCE_BOTTLE:10")
        );
        
        // COLLECT quest
        QuestTemplate collectQuest = new QuestTemplate(
                "treasure_hunter",
                "Treasure Hunter",
                "Collect the lost artifacts scattered throughout the dungeon",
                QuestType.COLLECT,
                3,
                "",
                "EMERALD",
                Arrays.asList("give %player% gold_ingot 10", "xp add %player% 300"),
                Arrays.asList("You have collected all the lost artifacts!", "Your archaeological skills are impressive."),
                Arrays.asList("GOLD_INGOT:10", "ENCHANTED_BOOK:1")
        );
        
        // EXPLORE quest
        QuestTemplate exploreQuest = new QuestTemplate(
                "dungeon_explorer",
                "Dungeon Explorer",
                "Discover the hidden treasure chambers of the dungeon",
                QuestType.EXPLORE,
                2,
                "",
                "",
                Arrays.asList("give %player% iron_ingot 15", "xp add %player% 200"),
                Arrays.asList("You have explored the dungeon thoroughly!", "Your cartography skills are impressive."),
                Arrays.asList("MAP:1", "COMPASS:1", "IRON_INGOT:15")
        );
        
        // Add templates
        questTemplates.add(bossQuest);
        questTemplates.add(collectQuest);
        questTemplates.add(exploreQuest);
        
        // Save to config
        saveQuestTemplatesToConfig();
    }
    
    /**
     * Save quest templates to configuration
     */
    private void saveQuestTemplatesToConfig() {
        for (QuestTemplate template : questTemplates) {
            String basePath = "quests.templates." + template.getId() + ".";
            
            plugin.getConfig().set(basePath + "name", template.getName());
            plugin.getConfig().set(basePath + "description", template.getDescription());
            plugin.getConfig().set(basePath + "type", template.getType().name());
            plugin.getConfig().set(basePath + "required_amount", template.getRequiredAmount());
            plugin.getConfig().set(basePath + "target_entity", template.getTargetEntity());
            plugin.getConfig().set(basePath + "target_item", template.getTargetItem());
            plugin.getConfig().set(basePath + "reward_commands", template.getRewardCommands());
            plugin.getConfig().set(basePath + "reward_messages", template.getRewardMessages());
            plugin.getConfig().set(basePath + "reward_items", template.getRewardItems());
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Load player quests from storage
     */
    private void loadPlayerQuests() {
        playerQuests.clear();
        
        if (!plugin.getConfig().isConfigurationSection("quests.player_quests")) {
            return;
        }
        
        // Load each player's quests
        for (String uuidStr : plugin.getConfig().getConfigurationSection("quests.player_quests").getKeys(false)) {
            try {
                UUID playerUuid = UUID.fromString(uuidStr);
                Map<String, Quest> quests = new HashMap<>();
                
                String basePath = "quests.player_quests." + uuidStr + ".";
                
                for (String questId : plugin.getConfig().getConfigurationSection(basePath).getKeys(false)) {
                    String questPath = basePath + questId + ".";
                    
                    // Load quest data
                    String templateId = plugin.getConfig().getString(questPath + "template_id");
                    QuestTemplate template = getQuestTemplate(templateId);
                    
                    if (template != null) {
                        String dungeonId = plugin.getConfig().getString(questPath + "dungeon_id");
                        int progress = plugin.getConfig().getInt(questPath + "progress", 0);
                        boolean completed = plugin.getConfig().getBoolean(questPath + "completed", false);
                        boolean rewardClaimed = plugin.getConfig().getBoolean(questPath + "reward_claimed", false);
                        
                        // Create quest
                        Quest quest = new Quest(
                                questId,
                                template,
                                dungeonId,
                                progress,
                                completed,
                                rewardClaimed
                        );
                        
                        quests.put(questId, quest);
                    }
                }
                
                playerQuests.put(playerUuid, quests);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading player quests: " + uuidStr, e);
            }
        }
    }
    
    /**
     * Save player quests to storage
     */
    public void savePlayerQuests() {
        // Clear existing data
        plugin.getConfig().set("quests.player_quests", null);
        
        // Save each player's quests
        for (Map.Entry<UUID, Map<String, Quest>> entry : playerQuests.entrySet()) {
            UUID playerUuid = entry.getKey();
            Map<String, Quest> quests = entry.getValue();
            
            for (Quest quest : quests.values()) {
                String basePath = "quests.player_quests." + playerUuid.toString() + "." + quest.getId() + ".";
                
                plugin.getConfig().set(basePath + "template_id", quest.getTemplate().getId());
                plugin.getConfig().set(basePath + "dungeon_id", quest.getDungeonId());
                plugin.getConfig().set(basePath + "progress", quest.getProgress());
                plugin.getConfig().set(basePath + "completed", quest.isCompleted());
                plugin.getConfig().set(basePath + "reward_claimed", quest.isRewardClaimed());
            }
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Get a quest template by ID
     */
    private QuestTemplate getQuestTemplate(String id) {
        for (QuestTemplate template : questTemplates) {
            if (template.getId().equals(id)) {
                return template;
            }
        }
        return null;
    }
    
    /**
     * Generate quest for a player when entering a dungeon
     */
    public void generateQuestForPlayer(Player player, BiomeArea dungeonArea) {
        // Skip if quests are disabled
        if (!plugin.getConfig().getBoolean("quests.enabled", true)) {
            return;
        }

        // Get dungeon ID
        String dungeonId = dungeonArea.getUniqueId();

        // Check if player already has this dungeon's quest
        if (hasQuestForDungeon(player.getUniqueId(), dungeonId)) {
            return;
        }

        // Get player quests map
        Map<String, Quest> quests = playerQuests.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        // Check if player has max quests
        int maxQuests = plugin.getConfig().getInt("quests.max_quests_per_player", 5);
        if (quests.size() >= maxQuests) {
            // Find a completed quest to replace
            Optional<String> completedQuestId = quests.values().stream()
                    .filter(Quest::isCompleted)
                    .map(Quest::getId)
                    .findFirst();

            if (completedQuestId.isPresent()) {
                quests.remove(completedQuestId.get());
            } else {
                // Don't give a new quest if at max and no completed quests
                player.sendMessage(ChatColor.YELLOW + "Your quest log is full. Complete some quests first.");
                return;
            }
        }

        // Get dungeon theme
        DungeonTheme theme = null;
        DungeonData dungeonData = plugin.getDungeonManager().getDungeon(dungeonArea);
        if (dungeonData != null) {
            theme = dungeonData.getTheme();
        }

        // Select appropriate quest templates for this theme
        List<QuestTemplate> appropriateTemplates = new ArrayList<>(questTemplates);

        // Filter by theme if needed
        if (theme != null) {
            // Logic to filter templates by theme
            // This is where you could implement theme-specific quests
        }

        // Skip if no appropriate templates
        if (appropriateTemplates.isEmpty()) {
            return;
        }

        // Choose a random template
        QuestTemplate template = appropriateTemplates.get(random.nextInt(appropriateTemplates.size()));

        // Create a new quest ID
        String questId = UUID.randomUUID().toString();

        // Create the quest
        Quest quest = new Quest(questId, template, dungeonId, 0, false, false);

        // Add to player quests
        quests.put(questId, quest);

        // Save to storage
        savePlayerQuests();

        // Notify player
        player.sendMessage(ChatColor.GOLD + "New Quest: " + ChatColor.WHITE + template.getName());
        player.sendMessage(ChatColor.GRAY + template.getDescription());
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);

        // Update the quest display (add this line)
        displayManager.updatePlayerDisplay(player);

        // Spawn quest-related markers in the dungeon
        spawnQuestMarkers(quest, dungeonArea);
    }
    
    /**
     * Check if a player has a quest for a specific dungeon
     */
    private boolean hasQuestForDungeon(UUID playerUuid, String dungeonId) {
        Map<String, Quest> quests = playerQuests.get(playerUuid);
        if (quests == null) {
            return false;
        }
        
        for (Quest quest : quests.values()) {
            if (quest.getDungeonId().equals(dungeonId) && !quest.isRewardClaimed()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Spawn markers and items in the dungeon for a quest
     */
    private void spawnQuestMarkers(Quest quest, BiomeArea dungeonArea) {
        DungeonData dungeonData = plugin.getDungeonManager().getDungeon(dungeonArea);
        if (dungeonData == null) {
            return;
        }
        
        QuestTemplate template = quest.getTemplate();
        QuestType type = template.getType();
        
        // Get the world
        org.bukkit.World world = Bukkit.getWorld(dungeonArea.getWorldName());
        if (world == null) {
            return;
        }
        
        // Get dungeon center
        Location center = new Location(world, dungeonArea.getCenterX(), 0, dungeonArea.getCenterZ());
        
        // Spawn appropriate markers based on quest type
        switch (type) {
            case KILL:
                // No special markers needed - just tag the mobs
                // This is handled in MobHandler
                break;
                
            case COLLECT:
                // Spawn collection items in chests
                spawnCollectionItems(quest, dungeonArea);
                break;
                
            case EXPLORE:
                // Add markers for explorer quests
                spawnExplorerMarkers(quest, dungeonArea);
                break;
        }
    }
    
    /**
     * Spawn collection items for a quest
     */
    private void spawnCollectionItems(Quest quest, BiomeArea dungeonArea) {
        QuestTemplate template = quest.getTemplate();
        
        // Get the world
        org.bukkit.World world = Bukkit.getWorld(dungeonArea.getWorldName());
        if (world == null) {
            return;
        }
        
        // Find chest blocks in the dungeon area
        int centerX = dungeonArea.getCenterX();
        int centerZ = dungeonArea.getCenterZ();
        int radius = dungeonArea.getRadius();
        
        // List to store found chests
        List<Chest> chests = new ArrayList<>();
        
        // Scan loaded chunks for chests (more efficient than scanning all blocks)
        for (int cx = centerX - radius; cx <= centerX + radius; cx += 16) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz += 16) {
                if (world.isChunkLoaded(cx >> 4, cz >> 4)) {
                    for (int bx = cx; bx < cx + 16; bx++) {
                        for (int bz = cz; bz < cz + 16; bz++) {
                            // Skip if outside radius
                            if (Math.sqrt(Math.pow(bx - centerX, 2) + Math.pow(bz - centerZ, 2)) > radius) {
                                continue;
                            }
                            
                            // Scan vertical slice for chests
                            for (int y = 40; y < 120; y++) {
                                Block block = world.getBlockAt(bx, y, bz);
                                if (block.getType() == Material.CHEST) {
                                    if (block.getState() instanceof Chest) {
                                        chests.add((Chest) block.getState());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Skip if no chests found
        if (chests.isEmpty()) {
            return;
        }
        
        // Parse target item
        Material itemMaterial;
        try {
            itemMaterial = Material.valueOf(template.getTargetItem());
        } catch (IllegalArgumentException e) {
            itemMaterial = Material.PAPER; // Default to paper if invalid
        }
        
        // Create quest items
        ItemStack questItem = new ItemStack(itemMaterial);
        ItemMeta meta = questItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Quest Item: " + template.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + template.getDescription());
            lore.add(ChatColor.YELLOW + "Quest Item");
            meta.setLore(lore);
            
            // Store quest ID in item
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(questItemKey, PersistentDataType.STRING, quest.getId());
            
            questItem.setItemMeta(meta);
        }
        
        // Determine how many items to place
        int requiredAmount = template.getRequiredAmount();
        
        // Choose random chests to place items in
        Collections.shuffle(chests);
        for (int i = 0; i < Math.min(requiredAmount, chests.size()); i++) {
            Chest chest = chests.get(i);
            chest.getInventory().addItem(questItem.clone());
            chest.update();
        }
    }
    
    /**
     * Spawn exploration markers for a quest
     */
    private void spawnExplorerMarkers(Quest quest, BiomeArea dungeonArea) {
        QuestTemplate template = quest.getTemplate();
        
        // Get the world
        org.bukkit.World world = Bukkit.getWorld(dungeonArea.getWorldName());
        if (world == null) {
            return;
        }
        
        // Get dungeon center and radius
        int centerX = dungeonArea.getCenterX();
        int centerZ = dungeonArea.getCenterZ();
        int radius = dungeonArea.getRadius();
        
        // Number of marker points to create
        int markerCount = template.getRequiredAmount();
        
        // List to store potential marker locations
        List<Location> potentialLocations = new ArrayList<>();
        
        // Scan loaded chunks for suitable locations
        for (int cx = centerX - radius; cx <= centerX + radius; cx += 16) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz += 16) {
                if (world.isChunkLoaded(cx >> 4, cz >> 4)) {
                    for (int bx = cx; bx < cx + 16; bx += 4) {
                        for (int bz = cz; bz < cz + 16; bz += 4) {
                            // Skip if outside radius
                            if (Math.sqrt(Math.pow(bx - centerX, 2) + Math.pow(bz - centerZ, 2)) > radius * 0.8) {
                                continue;
                            }
                            
                            // Find a suitable Y level
                            for (int y = 40; y < 120; y++) {
                                Block block = world.getBlockAt(bx, y, bz);
                                Block above = world.getBlockAt(bx, y + 1, bz);
                                Block below = world.getBlockAt(bx, y - 1, bz);
                                
                                if (below.getType().isSolid() && 
                                    block.getType() == Material.AIR && 
                                    above.getType() == Material.AIR) {
                                    
                                    potentialLocations.add(new Location(world, bx, y, bz));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Skip if not enough locations found
        if (potentialLocations.size() < markerCount) {
            return;
        }
        
        // Shuffle locations and select the required number
        Collections.shuffle(potentialLocations);
        List<Location> markerLocations = potentialLocations.subList(0, markerCount);
        
        // Place markers at each location
        for (Location loc : markerLocations) {
            // Place glowing block or sign
            Block block = loc.getBlock();
            
            // Choose marker type (sign or glowing block)
            if (random.nextBoolean()) {
                // Create a sign
                block.setType(Material.OAK_SIGN);
                
                if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    sign.setLine(0, ChatColor.GOLD + "[Quest]");
                    sign.setLine(1, ChatColor.WHITE + template.getName());
                    sign.setLine(2, ChatColor.GRAY + "Explorer Point");
                    sign.setLine(3, ChatColor.AQUA + "Interact to mark");
                    
                    // Store quest ID in sign
                    sign.getPersistentDataContainer().set(questChestKey, PersistentDataType.STRING, quest.getId());
                    
                    sign.update();
                }
            } else {
                // Create a glowing block (visible through walls)
                block.setType(Material.SEA_LANTERN);
                
                // Add a pressure plate on top for interaction
                Block plateBlock = block.getRelative(0, 1, 0);
                plateBlock.setType(Material.STONE_PRESSURE_PLATE);
            }
        }
    }
    
    /**
     * Process quest progress for a player
     */
    public void processQuestProgress(Player player, QuestType type, String targetId) {
        // Skip if quests are disabled
        if (!plugin.getConfig().getBoolean("quests.enabled", true)) {
            return;
        }

        UUID playerUuid = player.getUniqueId();
        Map<String, Quest> quests = playerQuests.get(playerUuid);

        if (quests == null || quests.isEmpty()) {
            return;
        }

        // Find active quests matching this type and target
        for (Quest quest : quests.values()) {
            if (quest.isCompleted() || quest.isRewardClaimed()) {
                continue;
            }

            QuestTemplate template = quest.getTemplate();

            // Check if quest type matches
            if (template.getType() != type) {
                continue;
            }

            // Check target specifics based on quest type
            boolean matches = false;

            switch (type) {
                case KILL:
                    matches = template.getTargetEntity().equals(targetId) ||
                            (targetId.equals("BOSS") && template.getTargetEntity().equals("BOSS"));
                    break;

                case COLLECT:
                    matches = template.getTargetItem().equals(targetId);
                    break;

                case EXPLORE:
                    matches = true; // Explorer quests don't have specific targets
                    break;
            }

            // Update progress if matched
            if (matches) {
                int newProgress = quest.getProgress() + 1;
                quest.setProgress(newProgress);

                // Check if completed
                boolean completed = newProgress >= template.getRequiredAmount();
                if (completed) {
                    quest.setCompleted(true);

                    // Notify player
                    player.sendMessage(ChatColor.GREEN + "Quest Completed: " + ChatColor.WHITE + template.getName());
                    player.sendMessage(ChatColor.YELLOW + "Return to the Dungeon Gate to claim your reward!");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                    // Show quest completion in action bar (add this line)
                    displayManager.showQuestUpdateInActionBar(player, quest, true);
                } else {
                    // Progress notification
                    player.sendMessage(ChatColor.YELLOW + "Quest Progress: " + newProgress + "/" + template.getRequiredAmount() +
                            " - " + template.getName());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);

                    // Show quest progress in action bar (add this line)
                    displayManager.showQuestUpdateInActionBar(player, quest, false);
                }

                // Update the quest display (add this line)
                displayManager.updatePlayerDisplay(player);

                // Save to storage
                savePlayerQuests();
            }
        }
    }
    
    /**
     * Award quest rewards to a player
     */
    public void awardQuestRewards(Player player, String questId) {
        UUID playerUuid = player.getUniqueId();
        Map<String, Quest> quests = playerQuests.get(playerUuid);
        
        if (quests == null || !quests.containsKey(questId)) {
            return;
        }
        
        Quest quest = quests.get(questId);
        
        // Skip if not completed or already claimed
        if (!quest.isCompleted() || quest.isRewardClaimed()) {
            return;
        }
        
        QuestTemplate template = quest.getTemplate();
        
        // Mark as claimed
        quest.setRewardClaimed(true);
        
        // Save to storage
        savePlayerQuests();
        
        // Award XP and items
        // Execute reward commands
        for (String command : template.getRewardCommands()) {
            String formattedCommand = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
        }
        
        // Give reward items
        for (String itemString : template.getRewardItems()) {
            String[] parts = itemString.split(":");
            try {
                Material material = Material.valueOf(parts[0]);
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                
                ItemStack item = new ItemStack(material, amount);
                
                // Add to inventory or drop at feet if full
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack remaining : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), remaining);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error giving reward item: " + itemString);
            }
        }
        
        // Show reward messages
        player.sendMessage(ChatColor.GREEN + "Quest Rewards Received!");
        for (String message : template.getRewardMessages()) {
            player.sendMessage(ChatColor.YELLOW + message);
        }
        
        // Play sound effect
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player quests (if not already loaded)
        UUID playerUuid = event.getPlayer().getUniqueId();
        if (!playerQuests.containsKey(playerUuid)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // Load from storage
                if (plugin.getConfig().isConfigurationSection("quests.player_quests." + playerUuid.toString())) {
                    Map<String, Quest> quests = new HashMap<>();

                    String basePath = "quests.player_quests." + playerUuid.toString() + ".";

                    for (String questId : plugin.getConfig().getConfigurationSection(basePath).getKeys(false)) {
                        String questPath = basePath + questId + ".";

                        // Load quest data
                        String templateId = plugin.getConfig().getString(questPath + "template_id");
                        QuestTemplate template = getQuestTemplate(templateId);

                        if (template != null) {
                            String dungeonId = plugin.getConfig().getString(questPath + "dungeon_id");
                            int progress = plugin.getConfig().getInt(questPath + "progress", 0);
                            boolean completed = plugin.getConfig().getBoolean(questPath + "completed", false);
                            boolean rewardClaimed = plugin.getConfig().getBoolean(questPath + "reward_claimed", false);

                            // Create quest
                            Quest quest = new Quest(
                                    questId,
                                    template,
                                    dungeonId,
                                    progress,
                                    completed,
                                    rewardClaimed
                            );

                            quests.put(questId, quest);
                        }
                    }

                    playerQuests.put(playerUuid, quests);

                    // Show active quest status to player
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = event.getPlayer();

                        // Show uncompleted quests
                        boolean hasActiveQuests = false;
                        for (Quest quest : quests.values()) {
                            if (!quest.isRewardClaimed()) {
                                hasActiveQuests = true;
                                QuestTemplate template = quest.getTemplate();

                                if (quest.isCompleted()) {
                                    player.sendMessage(ChatColor.GREEN + "Completed Quest: " + ChatColor.WHITE +
                                            template.getName() + ChatColor.YELLOW + " (Reward Available)");
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "Active Quest: " + ChatColor.WHITE +
                                            template.getName() + ChatColor.GRAY + " (" + quest.getProgress() +
                                            "/" + template.getRequiredAmount() + ")");
                                }
                            }
                        }

                        if (hasActiveQuests) {
                            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/quests" +
                                    ChatColor.GRAY + " to view your quests");

                            // Update the quest display (add this line)
                            displayManager.updatePlayerDisplay(player);
                        }
                    });
                }
            });
        } else {
            // If the quests are already loaded, just update the display
            displayManager.updatePlayerDisplay(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up the quest display
        displayManager.cleanupPlayerDisplay(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if killed by a player
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        Entity entity = event.getEntity();
        
        // Check if entity has boss tag
        boolean isBoss = false;
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (container.has(new NamespacedKey(plugin, "dungeon_boss"), PersistentDataType.BYTE)) {
            isBoss = true;
        }
        
        // Process kill quest
        if (isBoss) {
            processQuestProgress(player, QuestType.KILL, "BOSS");
        } else {
            processQuestProgress(player, QuestType.KILL, entity.getType().name());
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Process item collection quests
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer container = meta.getPersistentDataContainer();
                    
                    if (container.has(questItemKey, PersistentDataType.STRING)) {
                        String questId = container.get(questItemKey, PersistentDataType.STRING);
                        
                        // Process the collection
                        processQuestProgress(player, QuestType.COLLECT, item.getType().name());
                        
                        // Remove one quest item
                        if (item.getAmount() > 1) {
                            item.setAmount(item.getAmount() - 1);
                        } else {
                            player.getInventory().remove(item);
                        }
                        
                        // Cancel the event to prevent normal item use
                        event.setCancelled(true);
                    }
                }
            }
        }
        
        // Process exploration quests
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            
            if (block != null) {
                // Check if it's a quest marker
                if (block.getState() instanceof Sign) {
                    Sign sign = (Sign) block.getState();
                    PersistentDataContainer container = sign.getPersistentDataContainer();
                    
                    if (container.has(questChestKey, PersistentDataType.STRING)) {
                        String questId = container.get(questChestKey, PersistentDataType.STRING);
                        
                        // Process the exploration progress
                        processQuestProgress(player, QuestType.EXPLORE, "MARKER");
                        
                        // Remove the sign to mark as found
                        block.setType(Material.AIR);
                        
                        // Cancel the event
                        event.setCancelled(true);
                    }
                }
                // Check for sea lantern markers
                else if (block.getType() == Material.SEA_LANTERN || 
                         block.getType() == Material.STONE_PRESSURE_PLATE) {
                    // Check if it's a quest marker (check the block below if it's a pressure plate)
                    Block checkBlock = block.getType() == Material.STONE_PRESSURE_PLATE ? 
                                      block.getRelative(0, -1, 0) : block;
                    
                    if (checkBlock.getType() == Material.SEA_LANTERN) {
                        // Process the exploration progress
                        processQuestProgress(player, QuestType.EXPLORE, "MARKER");
                        
                        // Replace with normal block
                        checkBlock.setType(Material.GLOWSTONE);
                        
                        // Send message
                        player.sendMessage(ChatColor.YELLOW + "You discovered a hidden explorer's marker!");
                        
                        // Cancel the event
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
    
    /**
     * Get active quests for a player
     */
    public List<Quest> getPlayerQuests(UUID playerUuid) {
        Map<String, Quest> quests = playerQuests.get(playerUuid);
        if (quests == null) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(quests.values());
    }
    
    /**
     * Show quest status to a player
     */
    public void showQuestStatus(Player player) {
        LanguageManager lang = plugin.getLanguageManager();
        List<Quest> quests = getPlayerQuests(player.getUniqueId());

        if (quests.isEmpty()) {
            player.sendMessage(lang.getMessage("quest.status.no_quests"));
            return;
        }

        player.sendMessage(lang.getMessage("quest.status.header"));

        // Group by completed status
        List<Quest> activeQuests = new ArrayList<>();
        List<Quest> completedQuests = new ArrayList<>();
        List<Quest> claimedQuests = new ArrayList<>();

        for (Quest quest : quests) {
            if (quest.isRewardClaimed()) {
                claimedQuests.add(quest);
            } else if (quest.isCompleted()) {
                completedQuests.add(quest);
            } else {
                activeQuests.add(quest);
            }
        }

        // Show active quests
        if (!activeQuests.isEmpty()) {
            player.sendMessage(lang.getMessage("quest.status.active_header"));
            for (Quest quest : activeQuests) {
                QuestTemplate template = quest.getTemplate();
                player.sendMessage(lang.getMessage("quest.status.active_format",
                        template.getName(),
                        quest.getProgress(),
                        template.getRequiredAmount(),
                        template.getDescription()
                ));
            }
        }

        // Show completed quests
        if (!completedQuests.isEmpty()) {
            player.sendMessage(lang.getMessage("quest.status.completed_header"));
            for (Quest quest : completedQuests) {
                QuestTemplate template = quest.getTemplate();
                player.sendMessage(lang.getMessage("quest.status.completed_format",
                        template.getName(),
                        quest.getId()
                ));
            }
        }

        // Show claimed quests
        if (!claimedQuests.isEmpty() && plugin.getConfig().getBoolean("quests.show_claimed", false)) {
            player.sendMessage(lang.getMessage("quest.status.claimed_header"));
            for (Quest quest : claimedQuests) {
                QuestTemplate template = quest.getTemplate();
                player.sendMessage(lang.getMessage("quest.status.claimed_format",
                        template.getName()
                ));
            }
        }
    }
    
    /**
     * Abandon a quest for a player
     */
    public boolean abandonQuest(Player player, String questId) {
        UUID playerUuid = player.getUniqueId();
        Map<String, Quest> quests = playerQuests.get(playerUuid);

        if (quests == null || !quests.containsKey(questId)) {
            return false;
        }

        // Remove quest
        quests.remove(questId);

        // Save to storage
        savePlayerQuests();

        // Update the quest display (add this line)
        displayManager.updatePlayerDisplay(player);

        return true;
    }

    public void cleanupDisplays() {
        if (displayManager != null) {
            displayManager.cleanupAllDisplays();
        }
    }

    /**
     * Get the display manager for quests
     * @return The QuestDisplayManager instance
     */
    public QuestDisplayManager getDisplayManager() {
        return displayManager;
    }
}
