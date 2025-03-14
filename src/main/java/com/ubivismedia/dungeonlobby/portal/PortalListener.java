package com.ubivismedia.dungeonlobby.portal;

import com.ubivismedia.dungeonlobby.dungeon.DungeonManager;
import com.ubivismedia.dungeonlobby.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class PortalListener implements Listener {
    private final DungeonManager dungeonManager;
    private final PartyManager partyManager;
    private static final Material PORTAL_BLOCK = Material.END_PORTAL_FRAME;

    public PortalListener(DungeonManager dungeonManager, PartyManager partyManager) {
        this.dungeonManager = dungeonManager;
        this.partyManager = partyManager;
    }

    @EventHandler
    public void onPortalEnter(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        Player player = event.getPlayer();
        Location location = player.getLocation();
        Material blockType = location.getBlock().getType();

        if (blockType == PORTAL_BLOCK) {
            List<Player> partyMembers = partyManager.getPartyMembers(player);
            String dungeonId = dungeonManager.createDungeonInstance(player);

            for (Player member : partyMembers) {
                dungeonManager.teleportPlayerToDungeon(member, dungeonId);
            }
        }
    }
}