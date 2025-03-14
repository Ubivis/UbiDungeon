package com.ubivismedia.dungeonlobby.party;

import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class PartyManager implements CommandExecutor {
    private final Map<UUID, List<UUID>> parties = new HashMap<>();
    private final LanguageManager languageManager;

    public PartyManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public void createParty(Player leader) {
        if (!parties.containsKey(leader.getUniqueId())) {
            parties.put(leader.getUniqueId(), new ArrayList<>(Collections.singletonList(leader.getUniqueId())));
            languageManager.sendMessage(leader, "party.created");
        } else {
            leader.sendMessage("§cYou are already in a party!");
        }
    }

    public void addPlayerToParty(Player leader, Player member) {
        if (parties.containsKey(leader.getUniqueId())) {
            List<UUID> party = parties.get(leader.getUniqueId());
            if (!party.contains(member.getUniqueId())) {
                party.add(member.getUniqueId());
                languageManager.sendMessage(member, "party.joined");
            } else {
                member.sendMessage("§cYou are already in this party!");
            }
        } else {
            leader.sendMessage("§cYou are not the leader of a party!");
        }
    }

    public List<Player> getPartyMembers(Player player) {
        for (Map.Entry<UUID, List<UUID>> entry : parties.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) {
                return getPlayersFromUUIDs(entry.getValue());
            }
        }
        return Collections.singletonList(player);
    }

    private List<Player> getPlayersFromUUIDs(List<UUID> uuids) {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    public void removePlayerFromParty(Player player) {
        for (Map.Entry<UUID, List<UUID>> entry : parties.entrySet()) {
            if (entry.getValue().contains(player.getUniqueId())) {
                entry.getValue().remove(player.getUniqueId());
                languageManager.sendMessage(player, "party.left");
                if (entry.getValue().isEmpty()) {
                    parties.remove(entry.getKey());
                }
                return;
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("party")) {
            if (args.length < 1) {
                player.sendMessage("§cUsage: /party <create|invite|leave>");
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                createParty(player);
            } else if (args[0].equalsIgnoreCase("invite")) {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /party invite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    addPlayerToParty(player, target);
                } else {
                    player.sendMessage("§cPlayer not found!");
                }
            } else if (args[0].equalsIgnoreCase("leave")) {
                removePlayerFromParty(player);
            } else {
                player.sendMessage("§cInvalid command. Use: /party <create|invite|leave>");
            }
            return true;
        }
        return false;
    }
}