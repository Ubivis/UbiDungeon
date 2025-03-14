package com.ubivismedia.dungeonlobby;

import com.ubivismedia.dungeonlobby.core.DatabaseManager;
import com.ubivismedia.dungeonlobby.party.PartyManager;
import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import com.ubivismedia.dungeonlobby.dungeon.DungeonManager;
import com.ubivismedia.dungeonlobby.portal.PortalListener;
import org.bukkit.plugin.java.JavaPlugin;

public class DungeonLobby extends JavaPlugin {

    private PartyManager partyManager;
    private LanguageManager languageManager;
    private DungeonManager dungeonManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        getLogger().info("DungeonLobby Plugin enabled!");

        // Initialize Language Manager
        languageManager = new LanguageManager(this);

        // Initialize Party Manager
        partyManager = new PartyManager(languageManager);
        getCommand("party").setExecutor(partyManager);

        // Initialize Dungeon Manager
        dungeonManager = new DungeonManager(this, languageManager, databaseManager);

        // Register Portal Listener
        getServer().getPluginManager().registerEvents(new PortalListener(dungeonManager, partyManager), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("DungeonLobby Plugin disabled!");
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}