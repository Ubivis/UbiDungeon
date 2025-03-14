package com.ubivismedia.dungeonlobby.dungeon;

import com.ubivismedia.dungeonlobby.DungeonLobby;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import com.ubivismedia.dungeonlobby.traps.TrapManager;
import com.ubivismedia.dungeonlobby.core.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

public class DungeonManager extends PlaceholderExpansion implements Listener {
    private final DungeonLobby plugin;
    private final LanguageManager languageManager;
    private final TrapManager trapManager;
    private final DatabaseManager databaseManager;
    private final HashMap<UUID, String> playerDungeons = new HashMap<>();
    private final HashSet<String> activeDungeons = new HashSet<>();
    private final Random random = new Random();
    private final HashSet<UUID> selectingDifficulty = new HashSet<>();
    private final HashMap<String, String> dungeonDifficulties = new HashMap<>();

    public DungeonManager(DungeonLobby plugin, LanguageManager languageManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.databaseManager = databaseManager;
        this.trapManager = new TrapManager(languageManager);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.register();
        }
    }

    @Override
    public String getIdentifier() {
        return "dungeonlobby";
    }

    @Override
    public String getAuthor() {
        return "UbiVisMedia";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(Player player, String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }

        // Check if the request is for another player
        if (identifier.contains("_")) {
            String[] parts = identifier.split("_");
            if (parts.length == 2) {
                String targetName = parts[0];
                String statType = parts[1];
                Player target = Bukkit.getPlayerExact(targetName);
                if (target != null) {
                    return getStatValue(target, statType);
                }
            }
        }

        // Default: Request for the current player
        return getStatValue(player, identifier);
    }

    private String getStatValue(Player player, String statType) {
        if (player == null) {
            return "";
        }
        switch (statType.toLowerCase()) {
            case "dungeons_completed":
                return String.valueOf(databaseManager.getCompletedDungeons(player.getUniqueId().toString()));
            case "dungeons_failed":
                return String.valueOf(databaseManager.getFailedDungeons(player.getUniqueId().toString()));
            default:
                return null;
        }
    }

    public void openDifficultySelection(Player player) {
        selectingDifficulty.add(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 9, "Select Difficulty");

        gui.setItem(2, createGuiItem(Material.GREEN_WOOL, "difficulty.easy"));
        gui.setItem(4, createGuiItem(Material.YELLOW_WOOL, "difficulty.medium"));
        gui.setItem(6, createGuiItem(Material.RED_WOOL, "difficulty.hard"));
        gui.setItem(8, createGuiItem(Material.PURPLE_WOOL, "difficulty.epic"));
        gui.setItem(0, createGuiItem(Material.BARRIER, "dungeon.cancel"));

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String messageKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(null, messageKey));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Select Difficulty")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String itemName = clickedItem.getItemMeta().getDisplayName();
            if (itemName.equals(languageManager.getMessage(null, "dungeon.cancel"))) {
                selectingDifficulty.remove(player.getUniqueId());
                player.closeInventory();
                languageManager.sendMessage(player, "dungeon.selection_cancelled");
                return;
            }

            String difficulty = itemName.toLowerCase();            String dungeonId = createDungeonInstance(player, difficulty);
            teleportPlayerToDungeon(player, dungeonId);
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (selectingDifficulty.contains(player.getUniqueId())) {
            selectingDifficulty.remove(player.getUniqueId());
            languageManager.sendMessage(player, "dungeon.selection_cancelled");
        }
    }

    @EventHandler
    public void onPortalUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME) {
            openDifficultySelection(player);
        }
    }

    public String createDungeonInstance(Player player, String difficulty) {
        String dungeonId = "dungeon_" + player.getUniqueId();
        activeDungeons.add(dungeonId);
        playerDungeons.put(player.getUniqueId(), dungeonId);
        dungeonDifficulties.put(dungeonId, difficulty);
        plugin.getLogger().info("Dungeon instance created: " + dungeonId + " (Difficulty: " + difficulty + ")");

        generateDungeon(dungeonId, difficulty);

        return dungeonId;
    }

    public void teleportPlayerToDungeon(Player player, String dungeonId) {
        World dungeonWorld = Bukkit.createWorld(new WorldCreator(dungeonId));
        if (dungeonWorld != null) {
            Location dungeonSpawn = new Location(dungeonWorld, 0, 65, 0);
            player.teleport(dungeonSpawn);
            languageManager.sendMessage(player, "dungeon.enter");
        }
    }

    private void generateDungeon(String dungeonId, String difficulty) {
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld == null) return;

        int trapChance = getTrapChance(difficulty);

        for (int x = -5; x <= 5; x += 5) {
            for (int z = -5; z <= 5; z += 5) {
                if (random.nextInt(100) < trapChance) {
                    trapManager.placeRandomTrap(dungeonWorld, x, 65, z);
                }
            }
        }
    }

    private int getTrapChance(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "epic": return 90;
            case "hard": return 50;
            case "medium": return 20;
            case "easy": return 10;
            default: return 10;
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!playerDungeons.containsKey(player.getUniqueId())) return;

        String dungeonId = playerDungeons.get(player.getUniqueId());
        if (event.getFrom().getWorld().getName().equals(dungeonId)) {
            onPlayerLeaveDungeon(player, false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        selectingDifficulty.remove(player.getUniqueId());
        onPlayerLeaveDungeon(player, true);
    }

    public void onPlayerLeaveDungeon(Player player, boolean failed) {
        UUID playerId = player.getUniqueId();
        if (!playerDungeons.containsKey(playerId)) return;

        String dungeonId = playerDungeons.get(playerId);
        String difficulty = dungeonDifficulties.getOrDefault(dungeonId, "unknown");
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld == null) return;

        long playersInDungeon = dungeonWorld.getPlayers().stream().count();
        if (playersInDungeon <= 1) {
            removeDungeon(dungeonId);
        }

        databaseManager.logDungeonRun(player.getUniqueId().toString(), dungeonId, difficulty, !failed);
    }

    private void removeDungeon(String dungeonId) {
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld != null) {
            Bukkit.unloadWorld(dungeonWorld, false);
            activeDungeons.remove(dungeonId);
            dungeonDifficulties.remove(dungeonId);
            plugin.getLogger().info("Dungeon instance removed: " + dungeonId);
        }
    }
}