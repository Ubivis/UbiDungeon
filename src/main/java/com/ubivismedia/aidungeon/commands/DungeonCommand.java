package com.ubivismedia.aidungeon.commands;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.storage.DungeonData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

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
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    /**
     * Send help information to a player
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== AI Dungeon Generator Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/aidungeon generate" + ChatColor.WHITE + " - Generate a dungeon at your location");
        sender.sendMessage(ChatColor.YELLOW + "/aidungeon info" + ChatColor.WHITE + " - Show information about nearby dungeons");
        sender.sendMessage(ChatColor.YELLOW + "/aidungeon list" + ChatColor.WHITE + " - List all generated dungeons");
        sender.sendMessage(ChatColor.YELLOW + "/aidungeon tp <id>" + ChatColor.WHITE + " - Teleport to a dungeon by ID");
        sender.sendMessage(ChatColor.YELLOW + "/aidungeon reload" + ChatColor.WHITE + " - Reload the plugin configuration");
    }
    
    /**
     * Handle the generate command
     */
    private boolean handleGenerate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aidungeon.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Create a biome area at the player's location
        BiomeArea area = BiomeArea.fromLocation(player.getLocation(), 100);
        
        // Check if already generated
        if (!dungeonManager.canGenerateDungeon(player, area)) {
            player.sendMessage(ChatColor.RED + "A dungeon already exists in this area or you are in a blacklisted world.");
            return true;
        }
        
        // Queue generation
        dungeonManager.queueDungeonGeneration(area, player);
        player.sendMessage(ChatColor.GREEN + "Generating dungeon in " + area.getPrimaryBiome() + " biome...");
        return true;
    }
    
    /**
     * Handle the info command
     */
    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Get all dungeons
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        if (dungeons.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No dungeons have been generated yet.");
            return true;
        }
        
        Location playerLoc = player.getLocation();
        boolean foundNearby = false;
        
        // Check for nearby dungeons
        for (Map.Entry<BiomeArea, DungeonData> entry : dungeons.entrySet()) {
            BiomeArea area = entry.getKey();
            DungeonData data = entry.getValue();
            
            // Skip if not in the same world
            if (!area.getWorldName().equals(playerLoc.getWorld().getName())) {
                continue;
            }
            
            // Calculate distance
            double distance = Math.sqrt(
                    Math.pow(area.getCenterX() - playerLoc.getBlockX(), 2) +
                    Math.pow(area.getCenterZ() - playerLoc.getBlockZ(), 2)
            );
            
            // Show info if within 500 blocks
            if (distance <= 500) {
                player.sendMessage(ChatColor.GOLD + "=== Nearby Dungeon ===");
                player.sendMessage(ChatColor.YELLOW + "Theme: " + ChatColor.WHITE + data.getTheme().getName());
                player.sendMessage(ChatColor.YELLOW + "Biome: " + ChatColor.WHITE + area.getPrimaryBiome());
                player.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + 
                        "X:" + area.getCenterX() + ", Z:" + area.getCenterZ());
                player.sendMessage(ChatColor.YELLOW + "Distance: " + ChatColor.WHITE + 
                        String.format("%.1f", distance) + " blocks");
                player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + 
                        area.getWorldName() + ":" + area.getCenterX() + ":" + area.getCenterZ());
                
                foundNearby = true;
            }
        }
        
        if (!foundNearby) {
            player.sendMessage(ChatColor.YELLOW + "No dungeons within 500 blocks of your location.");
        }
        
        return true;
    }
    
    /**
     * Handle the reload command
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("aidungeon.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getConfigManager().loadConfig();
        
        sender.sendMessage(ChatColor.GREEN + "AI Dungeon Generator configuration reloaded.");
        return true;
    }
    
    /**
     * Handle the list command
     */
    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("aidungeon.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Get all dungeons
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        if (dungeons.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No dungeons have been generated yet.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Generated Dungeons ===");
        
        int count = 0;
        for (Map.Entry<BiomeArea, DungeonData> entry : dungeons.entrySet()) {
            BiomeArea area = entry.getKey();
            DungeonData data = entry.getValue();
            
            sender.sendMessage(ChatColor.YELLOW + "" + (++count) + ". " + 
                    ChatColor.WHITE + area.getWorldName() + " [" + 
                    area.getCenterX() + ", " + area.getCenterZ() + "] - " + 
                    ChatColor.AQUA + data.getTheme().getName());
            
            // Limit to 10 dungeons per page
            if (count >= 10) {
                sender.sendMessage(ChatColor.YELLOW + "Use /aidungeon list <page> to see more dungeons.");
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
            String[] subCommands = {"generate", "info", "reload", "list", "tp"};
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
