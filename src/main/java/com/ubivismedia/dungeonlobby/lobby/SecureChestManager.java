package com.ubivismedia.dungeonlobby.lobby;

import com.ubivismedia.dungeonlobby.DungeonLobby;
import com.ubivismedia.dungeonlobby.core.DatabaseManager;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class SecureChestManager implements Listener {
    private final DungeonLobby plugin;
    private final DatabaseManager databaseManager;
    private final LanguageManager languageManager;
    private final HashMap<Location, UUID> secureChests = new HashMap<>();

    public SecureChestManager(DungeonLobby plugin, DatabaseManager databaseManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.languageManager = languageManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadSecureChests();
    }

    private void loadSecureChests() {
        // Läd Kisten aus der Datenbank in den Speicher
        secureChests.putAll(databaseManager.loadSecureChests());
    }

    public void addSecureChest(Location location, UUID playerUUID) {
        secureChests.put(location, playerUUID);
        databaseManager.saveSecureChest(location, playerUUID);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        Player player = event.getPlayer();
        UUID chestOwner = secureChests.get(block.getLocation());
        String playerLocale = player.getLocale(); // Holt die Client-Sprache des Spielers

        if (chestOwner == null) {
            // Falls die Kiste noch keinem Spieler gehört, wird sie beim ersten Öffnen registriert
            addSecureChest(block.getLocation(), player.getUniqueId());
            languageManager.sendMessage(player, "chest.ownership_set");
        } else if (!chestOwner.equals(player.getUniqueId())) {
            // Falls die Kiste einem anderen Spieler gehört, verweigern wir den Zugriff
            languageManager.sendMessage(player, "chest.access_denied");
            event.setCancelled(true);
            return;
        }

        // Lade gespeicherten Inhalt aus der Datenbank
        Chest chest = (Chest) block.getState();
        ItemStack[] storedItems = databaseManager.loadChestContents(block.getLocation());
        chest.getBlockInventory().setContents(storedItems);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Block block = event.getInventory().getLocation().getBlock();
        if (block == null || block.getType() != Material.CHEST) return;

        // Speichere den aktuellen Inhalt der Kiste, wenn sie geschlossen wird
        databaseManager.saveChestContents(block.getLocation(), event.getInventory());
    }
}
