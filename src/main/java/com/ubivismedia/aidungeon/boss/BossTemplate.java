package com.ubivismedia.aidungeon.boss;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Template for a dungeon boss
 */
public class BossTemplate {
    private final String id;
    private final String name;
    private final EntityType entityType;
    private double maxHealth = 100.0;
    private double attackDamage = 8.0;
    private double movementSpeed = 0.25;
    private final List<BossPhase> phases = new ArrayList<>();
    private final Map<PotionEffectType, Integer> effects = new HashMap<>();
    
    // Equipment items
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    
    public BossTemplate(String id, String name, EntityType entityType) {
        this.id = id;
        this.name = name;
        this.entityType = entityType;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }
    
    public double getAttackDamage() {
        return attackDamage;
    }
    
    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }
    
    public double getMovementSpeed() {
        return movementSpeed;
    }
    
    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }
    
    public void addPhase(BossPhase phase) {
        phases.add(phase);
    }
    
    public List<BossPhase> getPhases() {
        return phases;
    }
    
    public void addEffect(PotionEffectType type, int level) {
        effects.put(type, level);
    }
    
    public Map<PotionEffectType, Integer> getEffects() {
        return effects;
    }
    
    public ItemStack getHelmet() {
        return helmet;
    }
    
    public void setHelmet(ItemStack helmet) {
        this.helmet = helmet;
    }
    
    public ItemStack getChestplate() {
        return chestplate;
    }
    
    public void setChestplate(ItemStack chestplate) {
        this.chestplate = chestplate;
    }
    
    public ItemStack getLeggings() {
        return leggings;
    }
    
    public void setLeggings(ItemStack leggings) {
        this.leggings = leggings;
    }
    
    public ItemStack getBoots() {
        return boots;
    }
    
    public void setBoots(ItemStack boots) {
        this.boots = boots;
    }
    
    public ItemStack getMainHand() {
        return mainHand;
    }
    
    public void setMainHand(ItemStack mainHand) {
        this.mainHand = mainHand;
    }
}