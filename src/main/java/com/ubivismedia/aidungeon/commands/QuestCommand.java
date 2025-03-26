package com.ubivismedia.aidungeon.commands;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.quests.Quest;
import com.ubivismedia.aidungeon.quests.QuestSystem;
import com.ubivismedia.aidungeon.quests.QuestDisplayManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for quest-related commands
 */
public class QuestCommand implements CommandExecutor, TabCompleter {

    private final AIDungeonGenerator plugin;
    private final QuestSystem questSystem;

    /**
     * Create a new quest command handler
     */
    public QuestCommand(AIDungeonGenerator plugin, QuestSystem questSystem) {
        this.plugin = plugin;
        this.questSystem = questSystem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Show quest list
            questSystem.showQuestStatus(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                // Show quest list
                questSystem.showQuestStatus(player);
                return true;

            case "claim":
                // Claim quest rewards
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /quests claim <quest_id>");
                    return true;
                }

                String questId = args[1];
                questSystem.awardQuestRewards(player, questId);
                return true;

            case "abandon":
                // Abandon a quest
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /quests abandon <quest_id>");
                    return true;
                }

                String abandonQuestId = args[1];
                if (questSystem.abandonQuest(player, abandonQuestId)) {
                    player.sendMessage(ChatColor.YELLOW + "Quest abandoned.");
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                return true;

            case "details":
                // Show detailed information about a quest
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /quests details <quest_id>");
                    return true;
                }

                String detailQuestId = args[1];
                Quest detailQuest = getQuestById(player, detailQuestId);

                if (detailQuest != null) {
                    questSystem.getDisplayManager().showQuestDetails(player, detailQuest);
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                return true;

            case "track":
                // Create a compass that tracks a quest
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /quests track <quest_id>");
                    return true;
                }

                String trackQuestId = args[1];
                Quest trackQuest = getQuestById(player, trackQuestId);

                if (trackQuest != null) {
                    questSystem.getDisplayManager().createQuestTracker(player, trackQuest);
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                return true;

            case "reload":
                // Reload quests (admin only)
                if (!player.hasPermission("aidungeon.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                // Reload configuration and quest templates
                plugin.reloadConfig();
                questSystem.loadQuestTemplates();
                player.sendMessage(ChatColor.GREEN + "Quest system reloaded.");
                return true;

            default:
                // Unknown command
                player.sendMessage(ChatColor.RED + "Unknown quest command: " + subCommand);
                showHelp(player);
                return true;
        }
    }

    /**
     * Get a quest by ID for a player
     */
    private Quest getQuestById(Player player, String questId) {
        List<Quest> quests = questSystem.getPlayerQuests(player.getUniqueId());

        for (Quest quest : quests) {
            if (quest.getId().equals(questId)) {
                return quest;
            }
        }

        return null;
    }

    /**
     * Show command help
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "==== Quest Commands ====");
        player.sendMessage(ChatColor.YELLOW + "/quests" + ChatColor.WHITE + " - Show your active quests");
        player.sendMessage(ChatColor.YELLOW + "/quests details <id>" + ChatColor.WHITE + " - Show detailed information about a quest");
        player.sendMessage(ChatColor.YELLOW + "/quests track <id>" + ChatColor.WHITE + " - Get a compass that tracks this quest");
        player.sendMessage(ChatColor.YELLOW + "/quests claim <id>" + ChatColor.WHITE + " - Claim rewards for a completed quest");
        player.sendMessage(ChatColor.YELLOW + "/quests abandon <id>" + ChatColor.WHITE + " - Abandon a quest");

        if (player.hasPermission("aidungeon.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/quests reload" + ChatColor.WHITE + " - Reload quest system");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            // Subcommands
            List<String> subCommands = new ArrayList<>(Arrays.asList("list", "details", "track", "claim", "abandon"));

            if (player.hasPermission("aidungeon.admin")) {
                subCommands.add("reload");
            }

            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("claim") ||
                    args[0].equalsIgnoreCase("abandon") ||
                    args[0].equalsIgnoreCase("details") ||
                    args[0].equalsIgnoreCase("track")) {
                // Quest IDs
                return questSystem.getPlayerQuests(player.getUniqueId()).stream()
                        .map(Quest::getId)
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}