package com.ubivismedia.aidungeon.commands;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.localization.LanguageManager;
import com.ubivismedia.aidungeon.storage.DungeonData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles commands for the AI Dungeon Generator
 */
public class DungeonCommand implements CommandExecutor, TabCompleter {
    
    private final AIDungeonGenerator plugin;
    private final DungeonManager dungeonManager;
    
    public DungeonCommand(AIDungeonGenerator plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "generate":
                return handleGenerate(sender, args);
            case "info":
                return handleInfo(sender);
            case "reload":
                return handleReload(sender);
            case "list":
                return handleList(sender);
            case "teleport":
            case "tp":
                return handleTeleport(sender, args);
            case "debug":
                return handleDebug(sender);
            case "check":
                return handleCheck(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * Handle the check command
     */
    private boolean handleCheck(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("dungeon.errors.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aidungeon.admin")) {
            player.sendMessage(lang.getMessage("dungeon.errors.no_permission"));
            return true;
        }

        // Manually trigger an exploration check for this player
        plugin.getExplorationChecker().checkPlayerExploration(player);
        player.sendMessage(lang.getMessage("dungeon.check.triggered"));

        return true;
    }

    /**
     * Handle debug command to show detailed location information
     */
    private boolean handleDebug(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();

        // Ensure command is used by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("dungeon.errors.player_only"));
            return true;
        }

        Player player = (Player) sender;
        Location location = player.getLocation();

        // Detailed configuration debugging
        sender.sendMessage("§6==== Configuration Debugging ====");

        // Check exploration threshold from various sources
        sender.sendMessage("§eExploration Threshold Lookup:");

        // Method 1: Direct config method
        sender.sendMessage("§f - Plugin Config Method:");
        try {
            double configThreshold = plugin.getConfig().getDouble("discovery.exploration-threshold", -1);
            sender.sendMessage("§f   - Threshold: " + configThreshold);
        } catch (Exception e) {
            sender.sendMessage("§c   - Error: " + e.getMessage());
        }

        // Method 2: Direct dungeon config
        sender.sendMessage("§f - Dungeon Config Direct:");
        try {
            ConfigurationSection dungeonConfig = plugin.getConfigManager().getConfigLoader().getConfig("dungeon");
            double dungeonConfigThreshold = dungeonConfig.getDouble("discovery.exploration-threshold", -1);
            sender.sendMessage("§f   - Threshold: " + dungeonConfigThreshold);
        } catch (Exception e) {
            sender.sendMessage("§c   - Error: " + e.getMessage());
        }

        // Method 3: Configuration Manager method
        sender.sendMessage("§f - Configuration Manager Method:");
        try {
            double managerThreshold = plugin.getConfigManager().getDouble("discovery.exploration-threshold", 0.1);
            sender.sendMessage("§f   - Threshold: " + managerThreshold);
        } catch (Exception e) {
            sender.sendMessage("§c   - Error: " + e.getMessage());
        }

        // Get world and biome
        World world = location.getWorld();
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        // Check if this is in a dungeon
        BiomeArea dungeonArea = plugin.getDungeonManager().getDungeonAreaAtLocation(location);

        // Explore the biome
        double explorationPercentage = plugin.getBiomeExplorationTracker().recordExploredChunk(
                player,
                world,
                biome
        );

        // Check dungeon generation conditions
        boolean hasDungeonBeenGenerated = plugin.getBiomeExplorationTracker()
                .hasDungeonBeenGenerated(world, biome);

        // Use the configuration manager method to get threshold
        double explorationThreshold = plugin.getConfigManager().getDouble("discovery.exploration-threshold", 0.1);

        // Debugging messages
        sender.sendMessage("§6==== Dungeon Debug Information ====");
        sender.sendMessage("§eWorld: §f" + world.getName());
        sender.sendMessage("§eBiome: §f" + biome.name());
        sender.sendMessage("§eLocation: §fX:" + location.getBlockX() +
                " Y:" + location.getBlockY() +
                " Z:" + location.getBlockZ());
        sender.sendMessage("§eExploration Percentage: §f" +
                String.format("%.4f", explorationPercentage * 100) + "%");
        sender.sendMessage("§eExploration Threshold: §f" +
                String.format("%.4f", explorationThreshold * 100) + "%");
        sender.sendMessage("§eDungeon Generated: §f" + hasDungeonBeenGenerated);

        return true;
    }

    /**
     * Send help information to a player
     */
    private void sendHelp(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        sender.sendMessage(lang.getMessage("dungeon.help.header"));
        sender.sendMessage(lang.getMessage("dungeon.help.generate"));
        sender.sendMessage(lang.getMessage("dungeon.help.info"));
        sender.sendMessage(lang.getMessage("dungeon.help.list"));
        sender.sendMessage(lang.getMessage("dungeon.help.tp"));
        sender.sendMessage(lang.getMessage("dungeon.help.reload"));
        sender.sendMessage(lang.getMessage("dungeon.help.check"));
    }
    
    /**
     * Handle the generate command
     */
    private boolean handleGenerate(CommandSender sender, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("dungeon.errors.player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("aidungeon.admin")) {
            player.sendMessage(lang.getMessage("dungeon.errors.no_permission"));
            return true;
        }

        BiomeArea area = BiomeArea.fromLocation(player.getLocation(), 100);

        if (!dungeonManager.canGenerateDungeon(player, area)) {
            player.sendMessage(lang.getMessage("dungeon.generate.exists"));
            return true;
        }

        dungeonManager.queueDungeonGeneration(area, player);
        player.sendMessage(lang.getMessage("dungeon.generate.queued", area.getPrimaryBiome()));
        return true;
    }
    
    /**
     * Handle the info command
     */
    private boolean handleInfo(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("dungeon.errors.player_only"));
            return true;
        }

        Player player = (Player) sender;
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();

        if (dungeons.isEmpty()) {
            player.sendMessage(lang.getMessage("dungeon.info.no_dungeons"));
            return true;
        }

        Location playerLoc = player.getLocation();
        boolean foundNearby = false;

        for (Map.Entry<BiomeArea, DungeonData> entry : dungeons.entrySet()) {
            BiomeArea area = entry.getKey();
            DungeonData data = entry.getValue();

            if (!area.getWorldName().equals(playerLoc.getWorld().getName())) {
                continue;
            }

            double distance = Math.sqrt(
                    Math.pow(area.getCenterX() - playerLoc.getBlockX(), 2) +
                            Math.pow(area.getCenterZ() - playerLoc.getBlockZ(), 2)
            );

            if (distance <= 500) {
                player.sendMessage(lang.getMessage("dungeon.info.nearby_header"));
                player.sendMessage(lang.getMessage("dungeon.info.theme", data.getTheme().getName()));
                player.sendMessage(lang.getMessage("dungeon.info.biome", area.getPrimaryBiome()));
                player.sendMessage(lang.getMessage("dungeon.info.location", area.getCenterX(), area.getCenterZ()));
                player.sendMessage(lang.getMessage("dungeon.info.distance", String.format("%.1f", distance)));
                player.sendMessage(lang.getMessage("dungeon.info.id", area.getWorldName(), area.getCenterX(), area.getCenterZ()));

                foundNearby = true;
            }
        }

        if (!foundNearby) {
            player.sendMessage(lang.getMessage("dungeon.info.no_nearby"));
        }

        return true;
    }


    /**
     * Handle the reload command
     */
    private boolean handleReload(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!sender.hasPermission("aidungeon.admin")) {
            sender.sendMessage(lang.getMessage("dungeon.errors.no_permission"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfigManager().loadConfig();

        sender.sendMessage(lang.getMessage("dungeon.reload.success"));
        return true;
    }
    
    /**
     * Handle the list command
     */
    private boolean handleList(CommandSender sender) {
        LanguageManager lang = plugin.getLanguageManager();
        if (!sender.hasPermission("aidungeon.admin")) {
            sender.sendMessage(lang.getMessage("dungeon.errors.no_permission"));
            return true;
        }

        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();

        if (dungeons.isEmpty()) {
            sender.sendMessage(lang.getMessage("dungeon.list.no_dungeons"));
            return true;
        }

        sender.sendMessage(lang.getMessage("dungeon.list.header"));

        int count = 0;
        for (Map.Entry<BiomeArea, DungeonData> entry : dungeons.entrySet()) {
            BiomeArea area = entry.getKey();
            DungeonData data = entry.getValue();

            sender.sendMessage(lang.getMessage("dungeon.list.entry",
                    ++count,
                    area.getWorldName(),
                    area.getCenterX(),
                    area.getCenterZ(),
                    data.getTheme().getName()
            ));

            if (count >= 10) {
                sender.sendMessage(lang.getMessage("dungeon.list.more"));
                break;
            }
        }

        return true;
    }
    
    /**
     * Handle the teleport command
     */
    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aidungeon.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /aidungeon tp <id>");
            return true;
        }
        
        // Parse ID (world:x:z)
        String[] idParts = args[1].split(":");
        
        if (idParts.length != 3) {
            player.sendMessage(ChatColor.RED + "Invalid dungeon ID. Format: world:x:z");
            return true;
        }
        
        try {
            String worldName = idParts[0];
            int x = Integer.parseInt(idParts[1]);
            int z = Integer.parseInt(idParts[2]);
            
            // Find the matching dungeon
            BiomeArea targetArea = null;
            
            for (BiomeArea area : dungeonManager.getAllDungeons().keySet()) {
                if (area.getWorldName().equals(worldName) && 
                        area.getCenterX() == x && 
                        area.getCenterZ() == z) {
                    targetArea = area;
                    break;
                }
            }
            
            if (targetArea == null) {
                player.sendMessage(ChatColor.RED + "No dungeon found with that ID.");
                return true;
            }
            
            // Teleport player to the dungeon
            Location loc = targetArea.getCenterLocation();
            player.teleport(loc);
            player.sendMessage(ChatColor.GREEN + "Teleported to dungeon at " + 
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid dungeon ID. Format: world:x:z");
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"generate", "info", "reload", "list", "tp", "check"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            // For tp command, offer dungeon IDs
            if (sender instanceof Player && sender.hasPermission("aidungeon.admin")) {
                Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
                
                completions = dungeons.keySet().stream()
                        .map(area -> area.getWorldName() + ":" + area.getCenterX() + ":" + area.getCenterZ())
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}
