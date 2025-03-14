package com.ubivismedia.dungeonlobby.dungeon;

import com.ubivismedia.dungeonlobby.DungeonLobby;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import com.ubivismedia.dungeonlobby.traps.TrapManager;
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

public class DungeonManager implements Listener {
    private final DungeonLobby plugin;
    private final LanguageManager languageManager;
    private final TrapManager trapManager;
    private final HashMap<UUID, String> playerDungeons = new HashMap<>();
    private final HashSet<String> activeDungeons = new HashSet<>();
    private final Random random = new Random();
    private final HashSet<UUID> selectingDifficulty = new HashSet<>();

    public DungeonManager(DungeonLobby plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.trapManager = new TrapManager(languageManager);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openDifficultySelection(Player player) {
        selectingDifficulty.add(player.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 9, "Select Difficulty");

        gui.setItem(2, createGuiItem(Material.GREEN_WOOL, "difficulty.easy"));
        gui.setItem(4, createGuiItem(Material.YELLOW_WOOL, "difficulty.medium"));
        gui.setItem(6, createGuiItem(Material.RED_WOOL, "difficulty.hard"));
        gui.setItem(8, createGuiItem(Material.PURPLE_WOOL, "difficulty.epic"));

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
            selectingDifficulty.remove(player.getUniqueId());
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            String difficulty = clickedItem.getItemMeta().getDisplayName().toLowerCase();
            String dungeonId = createDungeonInstance(player, difficulty);
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
        plugin.getLogger().info("Dungeon instance created: " + dungeonId);

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
            onPlayerLeaveDungeon(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        selectingDifficulty.remove(player.getUniqueId());
        onPlayerLeaveDungeon(player);
    }

    public void onPlayerLeaveDungeon(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerDungeons.containsKey(playerId)) return;

        String dungeonId = playerDungeons.get(playerId);
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld == null) return;

        long playersInDungeon = dungeonWorld.getPlayers().stream().count();
        if (playersInDungeon <= 1) {
            removeDungeon(dungeonId);
        }
    }

    private void removeDungeon(String dungeonId) {
        World dungeonWorld = Bukkit.getWorld(dungeonId);
        if (dungeonWorld != null) {
            Bukkit.unloadWorld(dungeonWorld, false);
            activeDungeons.remove(dungeonId);
            plugin.getLogger().info("Dungeon instance removed: " + dungeonId);
        }
    }
}