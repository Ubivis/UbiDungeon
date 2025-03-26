package com.ubivismedia.aidungeon.handlers;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.storage.DungeonData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.entity.Warden;
import org.bukkit.entity.EntityType;
import org.bukkit.block.data.BlockData;
import java.lang.reflect.Method;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles trap mechanics in dungeons
 */
public class TrapHandler implements Listener {
    
    private final AIDungeonGenerator plugin;
    private final Random random = new Random();
    
    // Cache triggered traps to prevent multiple activations
    private final Map<Location, Long> triggeredTraps = new ConcurrentHashMap<>();
    
    // Trap cooldown (in milliseconds)
    private static final long TRAP_COOLDOWN = 60000; // 1 minute
    
    // Defines trap types available in the plugin
    public enum TrapType {
        ARROW,
        PIT,
        LAVA,
        POISON_GAS,
        CAVE_IN,
        FLAME_JET,
        TELEPORTER,
        FREEZING,
        WARDEN_SUMMON,
        SCULK_SHRIEKER
    }
    
    public TrapHandler(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        
        // Clean up old trap triggers periodically
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                triggeredTraps.entrySet().removeIf(entry -> 
                    currentTime - entry.getValue() > TRAP_COOLDOWN
                );
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Run every minute
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip if only head rotation changed
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() 
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location location = player.getLocation();
        Block block = location.getBlock();
        
        // Check if player is standing on a pressure plate
        if (isPressurePlate(block.getRelative(BlockFace.DOWN).getType())) {
            // Check if this is in a dungeon
            if (isInDungeon(location)) {
                // Check if this trap has been triggered recently
                if (!hasBeenTriggeredRecently(location)) {
                    // Trigger trap based on the dungeon theme
                    triggerRandomTrap(player, location);
                }
            }
        }
        
        // Check for hidden floor traps (if player is moving on certain blocks)
        Material standingOn = location.getBlock().getRelative(BlockFace.DOWN).getType();
        if ((standingOn == Material.STONE_BRICKS || standingOn == Material.CRACKED_STONE_BRICKS 
                || standingOn == Material.MOSSY_STONE_BRICKS) && random.nextInt(100) < 5) {
            
            // 5% chance to trigger hidden trap
            if (isInDungeon(location) && !hasBeenTriggeredRecently(location)) {
                triggerHiddenTrap(player, location);
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Check for tripwire or button activation
        if (event.getAction() == Action.PHYSICAL || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
                Material type = block.getType();
                
                if (type == Material.TRIPWIRE || type == Material.STONE_BUTTON 
                        || type == Material.LEVER) {
                    
                    // Check if this is in a dungeon
                    if (isInDungeon(block.getLocation()) && !hasBeenTriggeredRecently(block.getLocation())) {
                        // Trigger trap
                        triggerRandomTrap(event.getPlayer(), block.getLocation());
                    }
                }
            }
        }
    }
    
    /**
     * Check if a material is a pressure plate
     */
    private boolean isPressurePlate(Material material) {
        return material == Material.STONE_PRESSURE_PLATE
                || material == Material.OAK_PRESSURE_PLATE
                || material == Material.SPRUCE_PRESSURE_PLATE
                || material == Material.BIRCH_PRESSURE_PLATE
                || material == Material.JUNGLE_PRESSURE_PLATE
                || material == Material.ACACIA_PRESSURE_PLATE
                || material == Material.DARK_OAK_PRESSURE_PLATE
                || material == Material.CRIMSON_PRESSURE_PLATE
                || material == Material.WARPED_PRESSURE_PLATE
                || material == Material.LIGHT_WEIGHTED_PRESSURE_PLATE
                || material == Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
    }
    
    /**
     * Check if a location is within a generated dungeon
     */
    private boolean isInDungeon(Location location) {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        
        // Get all dungeons
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        // Check each dungeon
        for (BiomeArea area : dungeons.keySet()) {
            if (area.getWorldName().equals(location.getWorld().getName())) {
                // Calculate distance from dungeon center
                double distance = Math.sqrt(
                        Math.pow(area.getCenterX() - location.getBlockX(), 2) +
                        Math.pow(area.getCenterZ() - location.getBlockZ(), 2)
                );
                
                // If within radius, consider it in the dungeon
                if (distance <= area.getRadius()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a trap at this location has been triggered recently
     */
    private boolean hasBeenTriggeredRecently(Location location) {
        // Round location to block center to avoid slight variations
        Location blockLoc = location.getBlock().getLocation().add(0.5, 0, 0.5);
        
        // Check if in cache and not expired
        if (triggeredTraps.containsKey(blockLoc)) {
            long triggerTime = triggeredTraps.get(blockLoc);
            if (System.currentTimeMillis() - triggerTime < TRAP_COOLDOWN) {
                return true;
            }
        }
        
        // Mark as triggered
        triggeredTraps.put(blockLoc, System.currentTimeMillis());
        return false;
    }
    
    /**
     * Trigger a random trap based on location and player
     */
    private void triggerRandomTrap(Player player, Location location) {
        // Get a random trap type
        TrapType[] trapTypes = TrapType.values();
        TrapType trapType = trapTypes[random.nextInt(trapTypes.length)];
        
        // Trigger the trap
        triggerTrap(player, location, trapType);
    }

    /**
     * Trigger a warden summon trap
     */
    private void triggerWardenSummonTrap(Player player, Location location) {
        // Get config values
        double summonChance = plugin.getConfig().getDouble("traps.types.WARDEN_SUMMON.summon_chance", 0.7);
        int warningSounds = plugin.getConfig().getInt("traps.types.WARDEN_SUMMON.warning_sounds", 3);
        int despawnTime = plugin.getConfig().getInt("traps.types.WARDEN_SUMMON.despawn_time", 120);

        // Play warning sounds and particles first
        for (int i = 0; i < warningSounds; i++) {
            int delay = i * 20; // 1 second between warnings
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Play sculk sensor activation sound
                location.getWorld().playSound(location, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.5f);

                // Play particles
                location.getWorld().spawnParticle(Particle.SCULK_CHARGE, location, 15, 1.0, 0.5, 1.0, 0);

                // Alert nearby players
                for (Player nearby : location.getWorld().getPlayers()) {
                    if (nearby.getLocation().distance(location) <= 15) {
                        nearby.sendMessage(ChatColor.DARK_PURPLE + "You hear an unsettling noise from the depths...");
                    }
                }
            }, delay);
        }

        // After all warnings, potentially summon the Warden
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Random chance to actually summon
            if (random.nextDouble() < summonChance) {
                // Spawn the Warden
                Warden warden = (Warden) location.getWorld().spawnEntity(location, EntityType.WARDEN);

                // Make the Warden angry at the player
                warden.setTarget(player);

                // Mark entity as a dungeon mob
                PersistentDataContainer container = warden.getPersistentDataContainer();
                container.set(new NamespacedKey(plugin, "dungeon_mob"), PersistentDataType.BYTE, (byte) 1);
                container.set(new NamespacedKey(plugin, "dungeon_boss"), PersistentDataType.BYTE, (byte) 1);

                // Send message to all nearby players
                for (Player nearby : location.getWorld().getPlayers()) {
                    if (nearby.getLocation().distance(location) <= 30) {
                        nearby.sendMessage(ChatColor.DARK_RED + "The Warden has been summoned!");
                    }
                }

                // Schedule despawn if desired
                if (despawnTime > 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (warden.isValid() && !warden.isDead()) {
                            warden.remove();
                        }
                    }, despawnTime * 20L);
                }
            }
        }, warningSounds * 20 + 40); // Extra 2 seconds after warnings

        // Notification
        player.sendMessage(ChatColor.DARK_PURPLE + "You've disturbed something ancient...");
    }

    /**
     * Trigger a sculk shrieker trap
     */
    private void triggerSculkShriekerTrap(Player player, Location location) {
        // Get config values
        int shriekCount = plugin.getConfig().getInt("traps.types.SCULK_SHRIEKER.shriek_count", 3);
        int radius = plugin.getConfig().getInt("traps.types.SCULK_SHRIEKER.radius", 5);
        int cooldown = plugin.getConfig().getInt("traps.types.SCULK_SHRIEKER.cooldown", 10);

        // Place a temporary sculk shrieker block
        Block originalBlock = location.getBlock();
        Material originalMaterial = originalBlock.getType();
        BlockData originalData = originalBlock.getBlockData().clone();

        // Save the block and replace with sculk shrieker
        originalBlock.setType(Material.SCULK_SHRIEKER);

        // Attempt to set the can_summon property if it's available (depends on Minecraft version)
        BlockData shriekerData = originalBlock.getBlockData();
        try {
            // This uses reflection to avoid compile errors if the property isn't available
            Method canSummonMethod = shriekerData.getClass().getMethod("setCanSummon", boolean.class);
            canSummonMethod.invoke(shriekerData, true);
            originalBlock.setBlockData(shriekerData);
        } catch (Exception e) {
            // Ignore if this property isn't available
        }

        // Schedule multiple shrieks
        for (int i = 0; i < shriekCount; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (originalBlock.getType() == Material.SCULK_SHRIEKER) {
                    // Play sculk shrieker sound
                    location.getWorld().playSound(location, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 2.0f, 1.0f);

                    // Display warning particles
                    location.getWorld().spawnParticle(Particle.SCULK_SOUL, location, 20, 1.0, 1.0, 1.0, 0.1);

                    // Get and warn all players in range
                    for (Player nearby : location.getWorld().getPlayers()) {
                        if (nearby.getLocation().distance(location) <= radius * 2) {
                            nearby.sendMessage(ChatColor.DARK_PURPLE + "A sculk shrieker cries out!");
                        }
                    }
                }
            }, i * cooldown * 20L); // Convert cooldown to ticks
        }

        // Restore the original block after all shrieks
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (originalBlock.getType() == Material.SCULK_SHRIEKER) {
                originalBlock.setType(originalMaterial);
                originalBlock.setBlockData(originalData);
            }
        }, (shriekCount * cooldown + 2) * 20L); // Add 2 seconds after last shriek

        // Apply darkness effect to player
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0)); // 10 seconds of darkness

        // Notification
        player.sendMessage(ChatColor.DARK_PURPLE + "You've activated a sculk shrieker!");
    }
    
    /**
     * Trigger a hidden trap (subset of trap types that make sense for hidden traps)
     */
    private void triggerHiddenTrap(Player player, Location location) {
        // Only certain traps make sense as hidden floor traps
        TrapType[] hiddenTrapTypes = {
            TrapType.PIT, TrapType.POISON_GAS, TrapType.FLAME_JET
        };
        
        TrapType trapType = hiddenTrapTypes[random.nextInt(hiddenTrapTypes.length)];
        triggerTrap(player, location, trapType);
    }
    
    /**
     * Trigger a specific trap type
     */
    private void triggerTrap(Player player, Location location, TrapType trapType) {
        switch (trapType) {
            case ARROW:
                triggerArrowTrap(player, location);
                break;
            case PIT:
                triggerPitTrap(player, location);
                break;
            case LAVA:
                triggerLavaTrap(player, location);
                break;
            case POISON_GAS:
                triggerPoisonGasTrap(player, location);
                break;
            case CAVE_IN:
                triggerCaveInTrap(player, location);
                break;
            case FLAME_JET:
                triggerFlameJetTrap(player, location);
                break;
            case TELEPORTER:
                triggerTeleporterTrap(player, location);
                break;
            case FREEZING:
                triggerFreezingTrap(player, location);
                break;
            case WARDEN_SUMMON:
                triggerWardenSummonTrap(player, location);
                break;
            case SCULK_SHRIEKER:
                triggerSculkShriekerTrap(player, location);
                break;
        }
    }
    
    /**
     * Trigger an arrow trap
     */
    private void triggerArrowTrap(Player player, Location location) {
        // Play sound effect
        location.getWorld().playSound(location, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        
        // Find direction facing player
        Vector direction = player.getLocation().subtract(location).toVector().normalize();
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.CRIT, location, 20, 0.5, 0.5, 0.5, 0.1);
        
        // Deal damage to player
        player.damage(4.0); // 2 hearts of damage
        
        // Knockback effect
        player.setVelocity(direction.multiply(0.5));
        
        // Notification
        player.sendMessage("§c§oYou triggered an arrow trap!");
    }
    
    /**
     * Trigger a pit trap
     */
    private void triggerPitTrap(Player player, Location location) {
        // Break blocks beneath the player
        Block block = location.getBlock().getRelative(BlockFace.DOWN);
        Material originalType = block.getType();
        
        // Play sound
        location.getWorld().playSound(location, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.5f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.BLOCK_CRACK, 
                location.add(0, -1, 0), 50, 0.5, 0.2, 0.5, 0.1, originalType.createBlockData());
        
        // Set blocks to air (create pit)
        for (int y = 0; y >= -3; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Block targetBlock = block.getRelative(x, y, z);
                    if (targetBlock.getType().isSolid() && !targetBlock.getType().toString().contains("BEDROCK")) {
                        targetBlock.setType(Material.AIR);
                    }
                }
            }
        }
        
        // Set bottom layer to something dangerous
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block targetBlock = block.getRelative(x, -4, z);
                if (random.nextBoolean()) {
                    targetBlock.setType(Material.COBWEB);
                } else {
                    targetBlock.setType(Material.POINTED_DRIPSTONE, true);
                }
            }
        }
        
        // Notification
        player.sendMessage("§c§oThe floor gives way beneath you!");
        
        // Restore after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if players are still in the pit
                boolean playersInPit = false;
                for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 4, 2)) {
                    if (entity instanceof Player) {
                        playersInPit = true;
                        break;
                    }
                }
                
                // Only restore if no players are in the pit
                if (!playersInPit) {
                    for (int y = -4; y <= 0; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                Block targetBlock = block.getRelative(x, y, z);
                                
                                if (y == 0) {
                                    targetBlock.setType(originalType);
                                } else if (y == -4) {
                                    targetBlock.setType(Material.STONE);
                                } else {
                                    targetBlock.setType(Material.COBBLESTONE);
                                }
                            }
                        }
                    }
                } else {
                    // Try again later
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (int y = -4; y <= 0; y++) {
                                for (int x = -1; x <= 1; x++) {
                                    for (int z = -1; z <= 1; z++) {
                                        block.getRelative(x, y, z).setType(Material.COBBLESTONE);
                                    }
                                }
                            }
                        }
                    }.runTaskLater(plugin, 1200L); // Try again in 1 minute
                }
            }
        }.runTaskLater(plugin, 300L); // Wait 15 seconds before restoring
    }
    
    /**
     * Trigger a lava trap
     */
    private void triggerLavaTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.LAVA, location, 20, 0.5, 0.5, 0.5, 0.1);
        
        // Set nearby blocks to lava
        Block block = location.getBlock();
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (block.getRelative(x, 0, z).getType() == Material.AIR) {
                    block.getRelative(x, 0, z).setType(Material.LAVA);
                }
            }
        }
        
        // Notification
        player.sendMessage("§c§oLava erupts from hidden vents in the floor!");
        
        // Clear lava after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (block.getRelative(x, 0, z).getType() == Material.LAVA) {
                            block.getRelative(x, 0, z).setType(Material.AIR);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 200L); // Clear after 10 seconds
    }
    
    /**
     * Trigger a poison gas trap
     */
    private void triggerPoisonGasTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.5f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.SMOKE_LARGE, location, 100, 2, 1, 2, 0.1);
        
        // Apply potion effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1)); // 10 seconds of poison
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0)); // 15 seconds of nausea
        
        // Affect nearby players too
        for (Entity entity : location.getWorld().getNearbyEntities(location, 3, 2, 3)) {
            if (entity instanceof Player && entity != player) {
                Player nearbyPlayer = (Player) entity;
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 1));
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 0));
                nearbyPlayer.sendMessage("§c§oYou breathe in poisonous gas!");
            }
        }
        
        // Notification
        player.sendMessage("§c§oPoisonous gas fills the air!");
    }
    
    /**
     * Trigger a cave-in trap
     */
    private void triggerCaveInTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, location.add(0, 2, 0), 5, 1, 1, 1, 0.1);
        
        // Drop blocks from ceiling
        Block block = location.getBlock();
        List<Block> originalBlocks = new ArrayList<>();
        Map<Block, Material> blockData = new HashMap<>();
        
        // Store original blocks and set to falling blocks
        for (int y = 1; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (random.nextDouble() < 0.7) { // 70% chance per block
                        Block targetBlock = block.getRelative(x, y, z);
                        
                        // Skip if not solid or bedrock
                        if (!targetBlock.getType().isSolid() || 
                                targetBlock.getType() == Material.BEDROCK) {
                            continue;
                        }
                        
                        // Store original
                        originalBlocks.add(targetBlock);
                        blockData.put(targetBlock, targetBlock.getType());
                        
                        // Set to falling block
                        targetBlock.setType(Material.GRAVEL);
                    }
                }
            }
        }
        
        // Deal damage to player
        player.damage(6.0); // 3 hearts of damage
        
        // Slow player
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2)); // 5 seconds of slowness
        
        // Notification
        player.sendMessage("§c§oThe ceiling collapses above you!");
        
        // Restore original blocks after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block b : originalBlocks) {
                    if (b.getType() == Material.GRAVEL) {
                        b.setType(blockData.getOrDefault(b, Material.STONE));
                    }
                }
            }
        }.runTaskLater(plugin, 600L); // 30 seconds
    }
    
    /**
     * Trigger a flame jet trap
     */
    private void triggerFlameJetTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        
        // Set player on fire
        player.setFireTicks(100); // 5 seconds of fire
        
        // Spawn particles repeatedly
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                location.getWorld().spawnParticle(Particle.FLAME, 
                        location.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                
                // Set nearby air blocks to fire temporarily
                if (ticks % 5 == 0) {
                    Block block = location.getBlock();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (block.getRelative(x, 0, z).getType() == Material.AIR) {
                                block.getRelative(x, 0, z).setType(Material.FIRE);
                            }
                        }
                    }
                }
                
                ticks++;
                if (ticks >= 20) {
                    // End flame jet and clear fire
                    Block block = location.getBlock();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (block.getRelative(x, 0, z).getType() == Material.FIRE) {
                                block.getRelative(x, 0, z).setType(Material.AIR);
                            }
                        }
                    }
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 1/4 second for 5 seconds
        
        // Notification
        player.sendMessage("§c§oJets of flame burst from the floor!");
    }
    
    /**
     * Trigger a teleporter trap
     */
    private void triggerTeleporterTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.PORTAL, location, 100, 0.5, 1, 0.5, 0.1);
        
        // Find a random room in the dungeon to teleport to
        DungeonManager dungeonManager = plugin.getDungeonManager();
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        // Get current dungeon
        BiomeArea currentDungeon = null;
        for (BiomeArea area : dungeons.keySet()) {
            if (area.getWorldName().equals(location.getWorld().getName())) {
                double distance = Math.sqrt(
                        Math.pow(area.getCenterX() - location.getBlockX(), 2) +
                        Math.pow(area.getCenterZ() - location.getBlockZ(), 2)
                );
                
                if (distance <= area.getRadius()) {
                    currentDungeon = area;
                    break;
                }
            }
        }
        
        if (currentDungeon != null) {
            // Calculate a random point in the dungeon
            int x = currentDungeon.getCenterX() + random.nextInt(40) - 20;
            int z = currentDungeon.getCenterZ() + random.nextInt(40) - 20;
            
            // Find a safe Y level
            int y = findSafeY(location.getWorld(), x, z);
            
            if (y > 0) {
                // Apply disorientation effects before teleporting
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1)); // 3 seconds of blindness
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 0)); // 10 seconds of nausea
                
                // Teleport after a short delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Create teleport location
                        Location teleportLoc = new Location(location.getWorld(), x + 0.5, y, z + 0.5);
                        
                        // Teleport player
                        player.teleport(teleportLoc);
                        
                        // Play sound and particles at destination
                        location.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                        location.getWorld().spawnParticle(Particle.PORTAL, teleportLoc, 100, 0.5, 1, 0.5, 0.1);
                    }
                }.runTaskLater(plugin, 20L); // 1 second delay
                
                // Notification
                player.sendMessage("§c§oYou triggered a teleportation trap!");
            }
        }
    }
    
    /**
     * Trigger a freezing trap
     */
    private void triggerFreezingTrap(Player player, Location location) {
        // Play sound
        location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
        
        // Spawn particles
        location.getWorld().spawnParticle(Particle.SNOW_SHOVEL, location, 100, 2, 1, 2, 0.1);
        location.getWorld().spawnParticle(Particle.SNOWFLAKE, location, 50, 2, 1, 2, 0.05);
        
        // Apply slowness and mining fatigue
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 3)); // 10 seconds of extreme slowness
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 300, 2)); // 15 seconds of mining fatigue
        
        // Set blocks to ice/snow
        Block block = location.getBlock();
        List<Block> changedBlocks = new ArrayList<>();
        
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Block targetBlock = block.getRelative(x, y, z);
                    
                    if (targetBlock.getType() == Material.AIR || targetBlock.getType() == Material.WATER) {
                        if (y == 0 && random.nextBoolean()) {
                            targetBlock.setType(Material.SNOW_BLOCK);
                        } else if (random.nextDouble() < 0.3) {
                            targetBlock.setType(Material.ICE);
                        }
                        changedBlocks.add(targetBlock);
                    }
                }
            }
        }
        
        // Notification
        player.sendMessage("§c§oA freezing chill surrounds you!");
        
        // Melt ice/snow after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block b : changedBlocks) {
                    if (b.getType() == Material.ICE || b.getType() == Material.SNOW_BLOCK) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }.runTaskLater(plugin, 400L); // 20 seconds
    }
    
    /**
     * Get trap type from config for a specific area
     */
    private List<TrapType> getTrapsForArea(BiomeArea area) {
        DungeonData dungeonData = plugin.getDungeonManager().getDungeon(area);
        List<TrapType> trapTypes = new ArrayList<>();
        
        if (dungeonData == null) {
            // Default traps if dungeon data not found
            trapTypes.add(TrapType.ARROW);
            trapTypes.add(TrapType.PIT);
            return trapTypes;
        }
        
        // Get theme name
        String themeName = dungeonData.getTheme().getName();
        
        // Get trap list from config
        List<String> trapNames = plugin.getConfig().getStringList("themes." + themeName + ".traps");
        
        if (trapNames.isEmpty()) {
            // Default traps if none configured
            trapTypes.add(TrapType.ARROW);
            trapTypes.add(TrapType.PIT);
            return trapTypes;
        }
        
        // Convert names to trap types
        for (String trapName : trapNames) {
            try {
                TrapType trapType = TrapType.valueOf(trapName);
                trapTypes.add(trapType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid trap type in config: " + trapName);
            }
        }
        
        return trapTypes;
    }
    
    /**
     * Check if a trap type is enabled in config
     */
    private boolean isTrapEnabled(TrapType trapType) {
        return plugin.getConfig().getBoolean("traps.types." + trapType.name() + ".enabled", true);
    }
    
    /**
     * Get damage multiplier from config
     */
    private double getDamageMultiplier() {
        String difficulty = plugin.getConfig().getString("traps.difficulty", "normal");
        
        switch (difficulty.toLowerCase()) {
            case "easy":
                return plugin.getConfig().getDouble("traps.difficulty.easy", 0.3);
            case "hard":
                return plugin.getConfig().getDouble("traps.difficulty.hard", 1.5);
            case "normal":
            default:
                return plugin.getConfig().getDouble("traps.difficulty.normal", 1.0);
        }
    }
    
    /**
     * Find a safe Y coordinate for teleportation
     */
    private int findSafeY(org.bukkit.World world, int x, int z) {
        // Start from height 50, then work upwards to find 2 blocks of air
        for (int y = 40; y < world.getMaxHeight() - 10; y++) {
            if (world.getBlockAt(x, y, z).getType().isSolid() && 
                world.getBlockAt(x, y + 1, z).getType() == Material.AIR && 
                world.getBlockAt(x, y + 2, z).getType() == Material.AIR) {
                return y + 1; // Return the first air block
            }
        }
        
        // If no safe location found, return -1
        return -1;
    }
    
    /**
     * Trigger a trap at a specific location without requiring a player trigger
     * Used for scheduled trap activation or triggered by redstone
     */
    public void activateRandomTrap(Location location) {
        // Check if this is in a dungeon
        if (isInDungeon(location) && !hasBeenTriggeredRecently(location)) {
            // Get entities in range that might be affected
            Collection<Entity> nearbyEntities = location.getWorld().getNearbyEntities(location, 3, 2, 3);
            
            // Only activate if players are nearby
            boolean playersNearby = false;
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Player) {
                    playersNearby = true;
                    break;
                }
            }
            
            if (playersNearby) {
                BiomeArea area = getDungeonAreaAtLocation(location);
                List<TrapType> availableTraps = getTrapsForArea(area);
                
                // If no traps configured, use defaults
                if (availableTraps.isEmpty()) {
                    availableTraps = Arrays.asList(TrapType.values());
                }
                
                // Filter for enabled traps
                availableTraps.removeIf(trapType -> !isTrapEnabled(trapType));
                
                // If no traps enabled, do nothing
                if (availableTraps.isEmpty()) {
                    return;
                }
                
                // Select random trap
                TrapType trapType = availableTraps.get(random.nextInt(availableTraps.size()));
                
                // Activate trap
                for (Entity entity : nearbyEntities) {
                    if (entity instanceof Player) {
                        triggerTrap((Player) entity, location, trapType);
                    }
                }
            }
        }
    }
    
    /**
     * Get the dungeon area at a specific location
     */
    private BiomeArea getDungeonAreaAtLocation(Location location) {
        DungeonManager dungeonManager = plugin.getDungeonManager();
        Map<BiomeArea, DungeonData> dungeons = dungeonManager.getAllDungeons();
        
        for (BiomeArea area : dungeons.keySet()) {
            if (area.getWorldName().equals(location.getWorld().getName())) {
                double distance = Math.sqrt(
                        Math.pow(area.getCenterX() - location.getBlockX(), 2) +
                        Math.pow(area.getCenterZ() - location.getBlockZ(), 2)
                );
                
                if (distance <= area.getRadius()) {
                    return area;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Register a custom trap at a location
     * 
     * @param location The location of the trap
     * @param trapType The type of trap
     * @return True if registration was successful
     */
    public boolean registerCustomTrap(Location location, TrapType trapType) {
        if (!isInDungeon(location)) {
            return false;
        }
        
        // Mark the trap location in persistent data
        Block block = location.getBlock();
        
        // Store trap info in config for persistence
        String key = location.getWorld().getName() + "." + 
                    location.getBlockX() + "." + 
                    location.getBlockY() + "." + 
                    location.getBlockZ();
        
        plugin.getConfig().set("custom_traps." + key, trapType.name());
        plugin.saveConfig();
        
        return true;
    }
    
    /**
     * Unregister a custom trap at a location
     */
    public boolean unregisterCustomTrap(Location location) {
        String key = location.getWorld().getName() + "." + 
                    location.getBlockX() + "." + 
                    location.getBlockY() + "." + 
                    location.getBlockZ();
        
        if (plugin.getConfig().isSet("custom_traps." + key)) {
            plugin.getConfig().set("custom_traps." + key, null);
            plugin.saveConfig();
            return true;
        }
        
        return false;
    }
}
