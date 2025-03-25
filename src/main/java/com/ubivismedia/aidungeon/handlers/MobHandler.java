package com.ubivismedia.aidungeon.handlers;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.config.DungeonTheme;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.dungeons.DungeonManager;
import com.ubivismedia.aidungeon.storage.DungeonData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.rmi.server.Skeleton;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles mob spawning and behavior in dungeons
 */
public class MobHandler implements Listener {
    
    private final AIDungeonGenerator plugin;
    private final Random random = new Random();
    
    // Spawner types and their mob types
    private final Map<String, List<EntityType>> themeSpawnerTypes = new HashMap<>();
    
    // Unique dungeon mob tag
    private final NamespacedKey dungeonMobKey;
    private final NamespacedKey eliteMobKey;
    
    public MobHandler(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        this.dungeonMobKey = new NamespacedKey(plugin, "dungeon_mob");
        this.eliteMobKey = new NamespacedKey(plugin, "elite_mob");
        
        // Initialize theme-specific spawner types
        initializeThemeSpawnerTypes();
        
        // Run mob equip task to make sure mobs have proper equipment
        Bukkit.getScheduler().runTaskTimer(plugin, this::equipDungeonMobs, 200L, 600L);
    }
    
    /**
     * Initialize theme-specific spawner types
     */
    private void initializeThemeSpawnerTypes() {
        // Default mobs for all themes
        List<EntityType> defaultMobs = Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER);
        
        // PYRAMID theme
        themeSpawnerTypes.put("PYRAMID", Arrays.asList(
                EntityType.HUSK, EntityType.SKELETON, EntityType.CAVE_SPIDER, EntityType.SILVERFISH));
        
        // RUINS theme
        themeSpawnerTypes.put("RUINS", Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CREEPER));
        
        // MINESHAFT theme
        themeSpawnerTypes.put("MINESHAFT", Arrays.asList(
                EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.SILVERFISH, EntityType.BAT));
        
        // WITCH_HUT theme
        themeSpawnerTypes.put("WITCH_HUT", Arrays.asList(
                EntityType.WITCH, EntityType.SLIME, EntityType.CAVE_SPIDER, EntityType.ZOMBIE));
        
        // DWARVEN_HALLS theme
        themeSpawnerTypes.put("DWARVEN_HALLS", Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SILVERFISH, EntityType.PIGLIN));
        
        // ICE_CASTLE theme
        themeSpawnerTypes.put("ICE_CASTLE", Arrays.asList(
                EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SKELETON, EntityType.SNOWMAN));
        
        // UNDERWATER_RUINS theme
        themeSpawnerTypes.put("UNDERWATER_RUINS", Arrays.asList(
                EntityType.DROWNED, EntityType.GUARDIAN, EntityType.SQUID, EntityType.GLOW_SQUID));
        
        // Set default for any other themes
        for (DungeonTheme theme : plugin.getConfigManager().getAllThemes()) {
            if (!themeSpawnerTypes.containsKey(theme.getName())) {
                themeSpawnerTypes.put(theme.getName(), defaultMobs);
            }
        }
    }
    
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Check spawners in the chunk and update them
        for (BlockState state : event.getChunk().getTileEntities()) {
            if (state instanceof CreatureSpawner) {
                CreatureSpawner spawner = (CreatureSpawner) state;
                Location loc = spawner.getLocation();
                
                // Check if this spawner is in a dungeon
                BiomeArea dungeonArea = getDungeonAreaAtLocation(loc);
                if (dungeonArea != null) {
                    // Get dungeon data
                    DungeonData dungeonData = plugin.getDungeonManager().getDungeon(dungeonArea);
                    if (dungeonData != null) {
                        // Set spawner type based on theme
                        updateSpawnerForTheme(spawner, dungeonData.getTheme().getName());
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        // Get the spawner location
        Location spawnerLoc = event.getSpawner().getLocation();
        
        // Check if this spawner is in a dungeon
        BiomeArea dungeonArea = getDungeonAreaAtLocation(spawnerLoc);
        if (dungeonArea != null) {
            // Mark the spawned entity as a dungeon mob
            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity) {
                markAsDungeonMob((LivingEntity) entity);
                
                // Check for elite mob (10% chance)
                if (random.nextDouble() < 0.1) {
                    markAsEliteMob((LivingEntity) entity);
                }
            }
        }
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only process natural spawns and spawner spawns
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL && 
            event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        
        // Check if this spawn is in a dungeon
        Location loc = event.getLocation();
        BiomeArea dungeonArea = getDungeonAreaAtLocation(loc);
        
        if (dungeonArea != null) {
            LivingEntity entity = event.getEntity();
            
            // For natural spawns in dungeons, we might want to replace with theme-appropriate mobs
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                DungeonData dungeonData = plugin.getDungeonManager().getDungeon(dungeonArea);
                if (dungeonData != null) {
                    // 50% chance to replace with theme-specific mob
                    if (random.nextDouble() < 0.5) {
                        replaceWithThemeMob(event, dungeonData.getTheme().getName());
                    }
                }
            }
            
            // Mark as dungeon mob
            markAsDungeonMob(entity);
            
            // Small chance for elite mobs on natural spawns
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && random.nextDouble() < 0.05) {
                markAsEliteMob(entity);
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if this is a dungeon mob
        if (isDungeonMob(entity)) {
            // Increase drop rates for dungeon mobs
            increaseMobDrops(event);
            
            // Special drops for elite mobs
            if (isEliteMob(entity)) {
                addEliteMobDrops(event);
            }
        }
    }
    
    /**
     * Mark an entity as a dungeon mob
     */
    private void markAsDungeonMob(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(dungeonMobKey, PersistentDataType.BYTE, (byte) 1);
        
        // Schedule equipment to be added
        Bukkit.getScheduler().runTaskLater(plugin, () -> equipMob(entity, false), 1L);
    }
    
    /**
     * Mark an entity as an elite mob
     */
    private void markAsEliteMob(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(eliteMobKey, PersistentDataType.BYTE, (byte) 1);
        
        // Give it a name
        entity.setCustomName(getEliteName(entity));
        entity.setCustomNameVisible(true);
        
        // Increase health
        double baseHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(baseHealth * 2);
        entity.setHealth(baseHealth * 2);
        
        // Increase damage
        if (entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            double baseDamage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue();
            entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(baseDamage * 1.5);
        }
        
        // Add effects
        entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
        
        // 30% chance for regeneration
        if (random.nextDouble() < 0.3) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0));
        }
        
        // Schedule better equipment to be added
        Bukkit.getScheduler().runTaskLater(plugin, () -> equipMob(entity, true), 1L);
    }
    
    /**
     * Check if an entity is a dungeon mob
     */
    private boolean isDungeonMob(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(dungeonMobKey, PersistentDataType.BYTE);
    }
    
    /**
     * Check if an entity is an elite mob
     */
    private boolean isEliteMob(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(eliteMobKey, PersistentDataType.BYTE);
    }
    
    /**
     * Get a random elite name for a mob
     */
    private String getEliteName(LivingEntity entity) {
        String[] prefixes = {
            "§cElite ", "§cChampion ", "§cMighty ", "§cAncient ", "§cFearsome ", 
            "§cDread ", "§cDire ", "§cGrand ", "§cInfernal ", "§cDungeon "
        };
        
        String base = entity.getType().toString().toLowerCase();
        // Capitalize first letter
        base = base.substring(0, 1).toUpperCase() + base.substring(1);
        
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return prefix + base;
    }
    
    /**
     * Add equipment to a mob based on its type
     */
    private void equipMob(LivingEntity entity, boolean isElite) {
        // Skip if entity is dead or removed
        if (entity == null || entity.isDead() || !entity.isValid()) {
            return;
        }
        
        // Only equip certain mob types
        if (!(entity instanceof Zombie || entity instanceof Skeleton || 
              entity instanceof PigZombie || entity instanceof Wither || 
              entity instanceof WitherSkeleton || entity instanceof Stray ||
              entity instanceof Husk || entity instanceof Drowned)) {
            return;
        }
        
        // Chance for armor based on difficulty and elite status
        double armorChance = isElite ? 1.0 : 0.7;
        
        // Armor material
        Material[] armorMaterials;
        if (isElite) {
            armorMaterials = new Material[]{
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, 
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
            };
        } else {
            // Get random armor set
            Material[][] armorSets = {
                {Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS},
                {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS},
                {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
                {Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS}
            };
            
            armorMaterials = armorSets[random.nextInt(armorSets.length)];
        }
        
        // Add armor
        if (random.nextDouble() < armorChance) {
            // Helmet
            if (entity.getEquipment().getHelmet() == null && random.nextBoolean()) {
                entity.getEquipment().setHelmet(createEquipment(armorMaterials[0], isElite));
                entity.getEquipment().setHelmetDropChance(isElite ? 0.08f : 0.03f);
            }
            
            // Chestplate
            if (entity.getEquipment().getChestplate() == null && random.nextBoolean()) {
                entity.getEquipment().setChestplate(createEquipment(armorMaterials[1], isElite));
                entity.getEquipment().setChestplateDropChance(isElite ? 0.08f : 0.03f);
            }
            
            // Leggings
            if (entity.getEquipment().getLeggings() == null && random.nextBoolean()) {
                entity.getEquipment().setLeggings(createEquipment(armorMaterials[2], isElite));
                entity.getEquipment().setLeggingsDropChance(isElite ? 0.08f : 0.03f);
            }
            
            // Boots
            if (entity.getEquipment().getBoots() == null && random.nextBoolean()) {
                entity.getEquipment().setBoots(createEquipment(armorMaterials[3], isElite));
                entity.getEquipment().setBootsDropChance(isElite ? 0.08f : 0.03f);
            }
        }
        
        // Weapons
        if (entity.getEquipment().getItemInMainHand().getType() == Material.AIR) {
            Material weaponMaterial;
            
            if (entity instanceof Skeleton || entity instanceof Stray) {
                // Bow for skeletons
                weaponMaterial = Material.BOW;
            } else if (isElite) {
                // Diamond weapons for elite
                Material[] eliteWeapons = {
                    Material.DIAMOND_SWORD, Material.DIAMOND_AXE
                };
                weaponMaterial = eliteWeapons[random.nextInt(eliteWeapons.length)];
            } else {
                // Random weapon for others
                Material[] weapons = {
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE
                };
                weaponMaterial = weapons[random.nextInt(weapons.length)];
            }
            
            entity.getEquipment().setItemInMainHand(createEquipment(weaponMaterial, isElite));
            entity.getEquipment().setItemInMainHandDropChance(isElite ? 0.08f : 0.03f);
        }
    }
    
    /**
     * Create an equipment item with potential enchantments
     */
    private ItemStack createEquipment(Material material, boolean isElite) {
        ItemStack item = new ItemStack(material);
        
        // Add enchantments
        if (isElite || random.nextDouble() < 0.3) { // 30% chance for non-elite
            addRandomEnchantments(item, isElite);
        }
        
        // For elite items, add custom name
        if (isElite) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String[] prefixes = {"Ancient", "Mighty", "Dungeon", "Elite", "Fearsome", "Hallowed"};
                String[] suffixes = {"of Power", "of Strength", "of the Dungeon", "of Doom", "of Might", "of Glory"};
                
                String prefix = prefixes[random.nextInt(prefixes.length)];
                String suffix = suffixes[random.nextInt(suffixes.length)];
                
                // Get base name
                String baseName = material.toString().toLowerCase().replace('_', ' ');
                // Capitalize words
                String[] words = baseName.split(" ");
                StringBuilder nameBuilder = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        nameBuilder.append(word.substring(0, 1).toUpperCase())
                                 .append(word.substring(1))
                                 .append(" ");
                    }
                }
                
                meta.setDisplayName("§6" + prefix + " " + nameBuilder.toString().trim() + " " + suffix);
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }
    
    /**
     * Add random enchantments to an item
     */
    private void addRandomEnchantments(ItemStack item, boolean isElite) {
        // Skip if not enchantable
        if (!item.getType().name().contains("_")) {
            return;
        }
        
        // Determine number of enchantments
        int enchantCount = isElite ? 
                1 + random.nextInt(3) : // 1-3 for elite
                1;                      // 1 for non-elite
        
        // Get applicable enchantments
        List<Enchantment> applicableEnchants = new ArrayList<>();
        for (Enchantment enchant : Enchantment.values()) {
            if (enchant.canEnchantItem(item)) {
                applicableEnchants.add(enchant);
            }
        }
        
        // Skip if no applicable enchantments
        if (applicableEnchants.isEmpty()) {
            return;
        }
        
        // Add random enchantments
        for (int i = 0; i < enchantCount; i++) {
            if (applicableEnchants.isEmpty()) break;
            
            // Get random enchantment
            Enchantment enchant = applicableEnchants.get(random.nextInt(applicableEnchants.size()));
            applicableEnchants.remove(enchant); // Remove to avoid duplicates
            
            // Determine level
            int maxLevel = enchant.getMaxLevel();
            int level = isElite ? 
                    1 + random.nextInt(maxLevel) : // 1 to max for elite
                    1;                            // 1 for non-elite
            
            // Add enchantment
            item.addUnsafeEnchantment(enchant, level);
        }
    }
    
    /**
     * Update a spawner to spawn mobs appropriate for the dungeon theme
     */
    private void updateSpawnerForTheme(CreatureSpawner spawner, String themeName) {
        // Get mob types for this theme
        List<EntityType> mobTypes = themeSpawnerTypes.getOrDefault(themeName, 
                themeSpawnerTypes.get("RUINS")); // Fall back to RUINS if theme not found
        
        // Pick a random mob type
        EntityType mobType = mobTypes.get(random.nextInt(mobTypes.size()));
        
        // Update spawner
        spawner.setSpawnedType(mobType);
        spawner.update();
    }
    
    /**
     * Replace an entity with a theme-appropriate mob
     */
    private void replaceWithThemeMob(CreatureSpawnEvent event, String themeName) {
        // Get mob types for this theme
        List<EntityType> mobTypes = themeSpawnerTypes.getOrDefault(themeName, 
                themeSpawnerTypes.get("RUINS")); // Fall back to RUINS if theme not found
        
        // Pick a random mob type
        EntityType mobType = mobTypes.get(random.nextInt(mobTypes.size()));
        
        // Cancel the original spawn
        event.setCancelled(true);
        
        // Spawn the new mob
        Location loc = event.getLocation();
        //loc.getWorld().spawnEntity(loc, mobType, CreatureSpawnEvent.SpawnReason.CUSTOM);
        loc.getWorld().spawnEntity(loc, mobType);
    }
    
    /**
     * Increase drops for dungeon mobs
     */
    private void increaseMobDrops(EntityDeathEvent event) {
        // Get existing drops
        List<ItemStack> drops = event.getDrops();
        
        // Increase XP
        event.setDroppedExp(event.getDroppedExp() * 2);
        
        // 30% chance for additional common drops
        if (random.nextDouble() < 0.3) {
            // Add common drops based on mob type
            EntityType type = event.getEntityType();
            
            switch (type) {
                case ZOMBIE:
                case HUSK:
                case DROWNED:
                    drops.add(new ItemStack(Material.ROTTEN_FLESH, 1 + random.nextInt(3)));
                    if (random.nextDouble() < 0.1) drops.add(new ItemStack(Material.IRON_INGOT));
                    break;
                    
                case SKELETON:
                case STRAY:
                    drops.add(new ItemStack(Material.BONE, 1 + random.nextInt(3)));
                    if (random.nextDouble() < 0.1) drops.add(new ItemStack(Material.ARROW, 2 + random.nextInt(5)));
                    break;
                    
                case SPIDER:
                case CAVE_SPIDER:
                    drops.add(new ItemStack(Material.STRING, 1 + random.nextInt(3)));
                    if (random.nextDouble() < 0.1) drops.add(new ItemStack(Material.SPIDER_EYE));
                    break;
                    
                case CREEPER:
                    if (random.nextDouble() < 0.2) drops.add(new ItemStack(Material.GUNPOWDER, 1 + random.nextInt(2)));
                    break;
                    
                case WITCH:
                    if (random.nextDouble() < 0.2) drops.add(new ItemStack(Material.GLOWSTONE_DUST, 1 + random.nextInt(3)));
                    if (random.nextDouble() < 0.2) drops.add(new ItemStack(Material.REDSTONE, 1 + random.nextInt(3)));
                    if (random.nextDouble() < 0.1) drops.add(new ItemStack(Material.GLASS_BOTTLE));
                    break;
                    
                default:
                    // No special drops for other types
                    break;
            }
        }
    }
    
    /**
     * Add special drops for elite mobs
     */
    private void addEliteMobDrops(EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        
        // Guaranteed rare drop
        Material[] rareMaterials = {
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, 
            Material.EXPERIENCE_BOTTLE, Material.ENDER_PEARL
        };
        
        drops.add(new ItemStack(rareMaterials[random.nextInt(rareMaterials.length)], 1 + random.nextInt(2)));
        
        // Chance for enchanted book
        if (random.nextDouble() < 0.3) {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            addRandomEnchantments(book, true);
            drops.add(book);
        }
        
        // Increase XP further
        event.setDroppedExp(event.getDroppedExp() * 2); // 4x normal mob XP
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
     * Find and equip all dungeon mobs in loaded chunks
     */
    private void equipDungeonMobs() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            // Only process loaded entities to avoid performance issues
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isDungeonMob(entity)) {
                    boolean elite = isEliteMob(entity);
                    equipMob(entity, elite);
                }
            }
        }
    }
}
