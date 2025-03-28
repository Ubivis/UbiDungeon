package com.ubivismedia.aidungeon.boss;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import com.ubivismedia.aidungeon.dungeons.BiomeArea;
import com.ubivismedia.aidungeon.boss.abilities.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles advanced boss battle mechanics for AI Dungeon Generator
 */
public class BossManager implements Listener {

    private final AIDungeonGenerator plugin;
    private final Map<UUID, DungeonBoss> activeBosses = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> bossParticipants = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Boss metadata keys
    private static final String BOSS_UUID_KEY = "dungeon_boss_uuid";
    private static final String BOSS_TYPE_KEY = "dungeon_boss_type";
    private static final String BOSS_PHASE_KEY = "dungeon_boss_phase";

    /**
     * Initialize the boss manager
     */
    public BossManager(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start boss ability task
        new BukkitRunnable() {
            @Override
            public void run() {
                processActiveBosses();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    /**
     * Process active boss abilities
     */
    private void processActiveBosses() {
        // Create a copy to avoid concurrent modification
        Set<UUID> bossIds = new HashSet<>(activeBosses.keySet());
        
        for (UUID bossId : bossIds) {
            Entity entity = getBossEntity(bossId);
            
            // Skip if boss is no longer valid or alive
            if (entity == null || !entity.isValid() || !(entity instanceof LivingEntity) || ((LivingEntity) entity).isDead()) {
                cleanupBoss(bossId);
                continue;
            }
            
            LivingEntity boss = (LivingEntity) entity;
            DungeonBoss bossData = activeBosses.get(bossId);
            
            // Process abilities
            processBossAbilities(boss, bossData);
            
            // Update boss bar
            updateBossBar(bossId, boss);
        }
    }

    /**
     * Get a boss entity by UUID
     */
    private Entity getBossEntity(UUID bossId) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(bossId)) {
                    return entity;
                }
            }
        }
        return null;
    }

    /**
     * Process boss abilities based on cooldowns
     */
    private void processBossAbilities(LivingEntity boss, DungeonBoss bossData) {
        BossPhase currentPhase = bossData.getCurrentPhase();
        
        for (BossAbility ability : currentPhase.getAbilities()) {
            if (ability.isReady()) {
                // Use ability
                ability.execute(boss, getActivePlayers(boss.getUniqueId()));
                
                // Reset cooldown
                ability.resetCooldown();
            }
        }
    }

    /**
     * Get active players in the boss fight
     */
    private List<Player> getActivePlayers(UUID bossId) {
        List<Player> players = new ArrayList<>();
        List<UUID> participants = bossParticipants.getOrDefault(bossId, new ArrayList<>());
        
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        
        return players;
    }

    /**
     * Update the boss bar for all participants
     */
    private void updateBossBar(UUID bossId, LivingEntity boss) {
        BossBar bar = bossBars.get(bossId);
        if (bar == null) return;
        
        // Update health percentage
        double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = boss.getHealth();
        double percentage = health / maxHealth;
        
        bar.setProgress(Math.max(0, Math.min(1, percentage)));
        
        // Update title with phase info
        DungeonBoss bossData = activeBosses.get(bossId);
        bar.setTitle(bossData.getName() + " - Phase " + (bossData.getCurrentPhaseIndex() + 1));
        
        // Set color based on health
        if (percentage < 0.25) {
            bar.setColor(BarColor.RED);
        } else if (percentage < 0.5) {
            bar.setColor(BarColor.YELLOW);
        } else {
            bar.setColor(BarColor.GREEN);
        }
    }

    /**
     * Clean up boss data when a boss is defeated or removed
     */
    private void cleanupBoss(UUID bossId) {
        // Remove from active bosses
        activeBosses.remove(bossId);
        
        // Remove boss bar
        BossBar bar = bossBars.remove(bossId);
        if (bar != null) {
            bar.removeAll();
        }
        
        // Clean up participants
        bossParticipants.remove(bossId);
    }

    /**
     * Spawn a dungeon boss
     */
    public LivingEntity spawnBoss(String bossType, Location location, BiomeArea dungeonArea) {
        // Load boss configuration
        BossTemplate template = loadBossTemplate(bossType);
        if (template == null) {
            plugin.getLogger().warning("Failed to load boss template: " + bossType);
            return null;
        }
        
        // Spawn the entity
        LivingEntity boss = (LivingEntity) location.getWorld().spawnEntity(location, template.getEntityType());
        
        // Initialize the boss
        initializeBoss(boss, template, dungeonArea);
        
        return boss;
    }

    /**
     * Initialize a boss entity with advanced properties
     */
    private void initializeBoss(LivingEntity boss, BossTemplate template, BiomeArea dungeonArea) {
        // Set custom name
        boss.setCustomName(template.getName());
        boss.setCustomNameVisible(true);
        
        // Set attributes
        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(template.getMaxHealth());
        boss.setHealth(template.getMaxHealth());
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(template.getAttackDamage());
        boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(template.getMovementSpeed());
        
        // Add metadata
        PersistentDataContainer container = boss.getPersistentDataContainer();
        container.set(plugin.getNamespacedKey(BOSS_TYPE_KEY), PersistentDataType.STRING, template.getId());
        container.set(plugin.getNamespacedKey(BOSS_UUID_KEY), PersistentDataType.STRING, boss.getUniqueId().toString());
        container.set(plugin.getNamespacedKey(BOSS_PHASE_KEY), PersistentDataType.INTEGER, 0);
        
        // Mark as dungeon boss
        container.set(plugin.getNamespacedKey("dungeon_boss"), PersistentDataType.BYTE, (byte) 1);
        container.set(plugin.getNamespacedKey("dungeon_mob"), PersistentDataType.BYTE, (byte) 1);
        
        // Store boss data
        DungeonBoss bossData = new DungeonBoss(template);
        activeBosses.put(boss.getUniqueId(), bossData);
        
        // Create boss bar
        BossBar bossBar = Bukkit.createBossBar(
                template.getName() + " - Phase 1",
                BarColor.GREEN,
                BarStyle.SEGMENTED_10
        );
        bossBars.put(boss.getUniqueId(), bossBar);
        
        // Initialize participants list
        bossParticipants.put(boss.getUniqueId(), new ArrayList<>());
        
        // Apply equipment if applicable
        if (boss instanceof Zombie || boss instanceof Skeleton || boss instanceof PigZombie) {
            applyBossEquipment(boss, template);
        }
        
        // Apply initial effects
        for (PotionEffectType effectType : template.getEffects().keySet()) {
            int level = template.getEffects().get(effectType);
            boss.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, level));
        }
    }

    /**
     * Apply equipment to a boss entity
     */
    private void applyBossEquipment(LivingEntity boss, BossTemplate template) {
        // Apply equipment if configured
        if (template.getHelmet() != null) {
            boss.getEquipment().setHelmet(template.getHelmet());
        }
        if (template.getChestplate() != null) {
            boss.getEquipment().setChestplate(template.getChestplate());
        }
        if (template.getLeggings() != null) {
            boss.getEquipment().setLeggings(template.getLeggings());
        }
        if (template.getBoots() != null) {
            boss.getEquipment().setBoots(template.getBoots());
        }
        if (template.getMainHand() != null) {
            boss.getEquipment().setItemInMainHand(template.getMainHand());
        }
        
        // Set drop chances to 0 (drops will be handled on death)
        boss.getEquipment().setHelmetDropChance(0);
        boss.getEquipment().setChestplateDropChance(0);
        boss.getEquipment().setLeggingsDropChance(0);
        boss.getEquipment().setBootsDropChance(0);
        boss.getEquipment().setItemInMainHandDropChance(0);
    }

    /**
     * Load a boss template from configuration
     */
    private BossTemplate loadBossTemplate(String id) {
        try {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("bosses." + id);
            if (section == null) {
                plugin.getLogger().warning("Boss template not found: " + id);
                return null;
            }
            
            String name = section.getString("name", "Unknown Boss");
            EntityType entityType = EntityType.valueOf(section.getString("entity_type", "ZOMBIE"));
            double maxHealth = section.getDouble("max_health", 100.0);
            double attackDamage = section.getDouble("attack_damage", 8.0);
            double movementSpeed = section.getDouble("movement_speed", 0.25);
            
            // Create boss template
            BossTemplate template = new BossTemplate(id, name, entityType);
            template.setMaxHealth(maxHealth);
            template.setAttackDamage(attackDamage);
            template.setMovementSpeed(movementSpeed);
            
            // Load phases
            ConfigurationSection phasesSection = section.getConfigurationSection("phases");
            if (phasesSection != null) {
                for (String phaseKey : phasesSection.getKeys(false)) {
                    ConfigurationSection phaseSection = phasesSection.getConfigurationSection(phaseKey);
                    if (phaseSection == null) continue;
                    
                    // Create phase
                    BossPhase phase = new BossPhase();
                    phase.setTransitionThreshold(phaseSection.getInt("transition_threshold", 75));
                    phase.setTransitionMessage(phaseSection.getString("transition_message", "The boss enters a new phase!"));
                    
                    // Load abilities
                    ConfigurationSection abilitiesSection = phaseSection.getConfigurationSection("abilities");
                    if (abilitiesSection != null) {
                        for (String abilityKey : abilitiesSection.getKeys(false)) {
                            ConfigurationSection abilitySection = abilitiesSection.getConfigurationSection(abilityKey);
                            if (abilitySection == null) continue;
                            
                            // Load ability configuration
                            String abilityType = abilitySection.getString("type");
                            int cooldown = abilitySection.getInt("cooldown", 20);
                            
                            // Create ability based on type
                            BossAbility ability = createAbility(abilityType, abilitySection);
                            if (ability != null) {
                                ability.setCooldown(cooldown);
                                phase.addAbility(ability);
                            }
                        }
                    }
                    
                    // Add phase to template
                    template.addPhase(phase);
                }
            }
            
            // If no phases defined, create a default phase
            if (template.getPhases().isEmpty()) {
                BossPhase defaultPhase = new BossPhase();
                template.addPhase(defaultPhase);
            }
            
            // Load potion effects
            ConfigurationSection effectsSection = section.getConfigurationSection("effects");
            if (effectsSection != null) {
                for (String effectKey : effectsSection.getKeys(false)) {
                    try {
                        PotionEffectType effectType = PotionEffectType.getByName(effectKey.toUpperCase());
                        if (effectType != null) {
                            int level = effectsSection.getInt(effectKey, 0);
                            template.addEffect(effectType, level);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid effect type: " + effectKey);
                    }
                }
            }
            
            return template;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading boss template: " + id, e);
            return null;
        }
    }

    /**
     * Create a boss ability based on type
     */
    private BossAbility createAbility(String abilityType, ConfigurationSection config) {
        try {
            switch (abilityType.toUpperCase()) {
                case "MINION_SUMMON":
                    return new MinionSummonAbility(
                            EntityType.valueOf(config.getString("entity_type", "ZOMBIE")),
                            config.getInt("count", 3),
                            config.getDouble("radius", 5.0)
                    );
                case "AREA_EFFECT":
                    return new AreaEffectAbility(
                            PotionEffectType.getByName(config.getString("effect", "HARM")),
                            config.getInt("duration", 100),
                            config.getInt("amplifier", 1),
                            config.getDouble("radius", 5.0)
                    );
                case "LIGHTNING_STRIKE":
                    return new LightningStrikeAbility(
                            config.getInt("count", 3),
                            config.getDouble("radius", 8.0)
                    );
                case "TELEPORT":
                    return new TeleportAbility(
                            config.getDouble("radius", 10.0)
                    );
                case "PROJECTILE_BARRAGE":
                    return new ProjectileBarrageAbility(
                            config.getString("projectile", "ARROW"),
                            config.getInt("count", 5),
                            config.getDouble("speed", 1.5),
                            config.getDouble("spread", 45.0)
                    );
                default:
                    plugin.getLogger().warning("Unknown ability type: " + abilityType);
                    return null;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error creating ability: " + abilityType, e);
            return null;
        }
    }

    /**
     * Handle entity damage event for boss entities
     */
    @EventHandler
    public void onBossDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity && isDungeonBoss(event.getEntity())) {
            LivingEntity boss = (LivingEntity) event.getEntity();
            UUID bossId = boss.getUniqueId();
            
            // Check for phase transitions
            checkPhaseTransition(boss);
            
            // If damaged by a player, add to participants
            if (event instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                Player player = getPlayerDamager(damager);
                
                if (player != null) {
                    addBossParticipant(bossId, player);
                }
            }
        }
    }

    /**
     * Get player from damage source
     */
    private Player getPlayerDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
            return (Player) ((Projectile) damager).getShooter();
        }
        return null;
    }

    /**
     * Add a player to the boss fight participants
     */
    private void addBossParticipant(UUID bossId, Player player) {
        List<UUID> participants = bossParticipants.computeIfAbsent(bossId, k -> new ArrayList<>());
        
        if (!participants.contains(player.getUniqueId())) {
            participants.add(player.getUniqueId());
            
            // Add to boss bar
            BossBar bar = bossBars.get(bossId);
            if (bar != null) {
                bar.addPlayer(player);
            }
        }
    }

    /**
     * Check if an entity is a dungeon boss
     */
    public boolean isDungeonBoss(Entity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(plugin.getNamespacedKey("dungeon_boss"), PersistentDataType.BYTE);
    }

    /**
     * Check for boss phase transitions
     */
    private void checkPhaseTransition(LivingEntity boss) {
        UUID bossId = boss.getUniqueId();
        DungeonBoss bossData = activeBosses.get(bossId);
        if (bossData == null) return;
        
        // Get current health percentage
        double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double healthPercentage = (boss.getHealth() / maxHealth) * 100;
        
        // Check if we should transition to the next phase
        BossPhase currentPhase = bossData.getCurrentPhase();
        int currentPhaseIndex = bossData.getCurrentPhaseIndex();
        
        if (currentPhaseIndex < bossData.getPhases().size() - 1) {
            int threshold = currentPhase.getTransitionThreshold();
            
            if (healthPercentage <= threshold && !currentPhase.isTransitioned()) {
                // Mark as transitioned
                currentPhase.setTransitioned(true);
                
                // Move to next phase
                bossData.setCurrentPhaseIndex(currentPhaseIndex + 1);
                
                // Update phase in metadata
                boss.getPersistentDataContainer().set(
                        plugin.getNamespacedKey(BOSS_PHASE_KEY),
                        PersistentDataType.INTEGER,
                        currentPhaseIndex + 1
                );
                
                // Execute phase transition
                executePhaseTransition(boss, bossData.getCurrentPhase());
            }
        }
    }

    /**
     * Execute phase transition effects
     */
    private void executePhaseTransition(LivingEntity boss, BossPhase newPhase) {
        // Announce transition
        String message = newPhase.getTransitionMessage();
        for (Player player : getActivePlayers(boss.getUniqueId())) {
            player.sendMessage("§c§l" + message);
        }
        
        // Play effects
        Location location = boss.getLocation();
        World world = location.getWorld();
        world.strikeLightningEffect(location);
        world.playSound(location, org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        
        // Add temporary invulnerability
        boss.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (boss.isValid() && !boss.isDead()) {
                    boss.setInvulnerable(false);
                }
            }
        }.runTaskLater(plugin, 60L); // 3-second invulnerability
    }

    /**
     * Handle boss death event
     */
    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (isDungeonBoss(event.getEntity())) {
            LivingEntity boss = event.getEntity();
            UUID bossId = boss.getUniqueId();
            
            // Get boss data
            PersistentDataContainer container = boss.getPersistentDataContainer();
            String bossType = container.get(plugin.getNamespacedKey(BOSS_TYPE_KEY), PersistentDataType.STRING);
            
            // Increase drops if boss had a type
            if (bossType != null) {
                enhanceBossDrops(event, bossType);
            }
            
            // Notify participants
            notifyBossDeath(bossId, bossType);
            
            // Cleanup
            cleanupBoss(bossId);
        }
    }

    /**
     * Add custom drops when a boss is defeated
     */
    private void enhanceBossDrops(EntityDeathEvent event, String bossType) {
        // Clear default drops if configured
        if (plugin.getConfig().getBoolean("bosses." + bossType + ".clear_default_drops", true)) {
            event.getDrops().clear();
        }
        
        // Get configured drops
        ConfigurationSection dropsSection = plugin.getConfig().getConfigurationSection("bosses." + bossType + ".drops");
        if (dropsSection == null) return;
        
        // Add guaranteed drops
        ConfigurationSection guaranteedSection = dropsSection.getConfigurationSection("guaranteed");
        if (guaranteedSection != null) {
            for (String item : guaranteedSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(item);
                    int amount = guaranteedSection.getInt(item, 1);
                    event.getDrops().add(new ItemStack(material, amount));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid material in boss drops: " + item);
                }
            }
        }
        
        // Add chance-based drops
        ConfigurationSection chanceSection = dropsSection.getConfigurationSection("chance");
        if (chanceSection != null) {
            for (String item : chanceSection.getKeys(false)) {
                try {
                    ConfigurationSection itemSection = chanceSection.getConfigurationSection(item);
                    if (itemSection == null) continue;
                    
                    double chance = itemSection.getDouble("chance", 0.5);
                    int amount = itemSection.getInt("amount", 1);
                    
                    if (random.nextDouble() < chance) {
                        Material material = Material.valueOf(item);
                        event.getDrops().add(new ItemStack(material, amount));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid material in boss drops: " + item);
                }
            }
        }
        
        // Increase XP
        int xpBonus = plugin.getConfig().getInt("bosses." + bossType + ".xp_bonus", 500);
        event.setDroppedExp(event.getDroppedExp() + xpBonus);
    }

    /**
     * Notify participants about boss defeat
     */
    private void notifyBossDeath(UUID bossId, String bossType) {
        List<UUID> participants = bossParticipants.getOrDefault(bossId, new ArrayList<>());
        
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage("§6§lYou have defeated the " + 
                        plugin.getConfig().getString("bosses." + bossType + ".name", "Dungeon Boss") + "!");
                
                // Play victory sound
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                // Add to player stats if available
                // TODO: Implement stats tracking
            }
        }
    }
}