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
 * Represents a phase of a boss fight
 */
public class BossPhase {
    private final List<BossAbility> abilities = new ArrayList<>();
    private int transitionThreshold = 75; // Percentage of health to transition
    private String transitionMessage = "The boss enters a new phase!";
    private boolean transitioned = false;
    
    public void addAbility(BossAbility ability) {
        abilities.add(ability);
    }
    
    public List<BossAbility> getAbilities() {
        return abilities;
    }
    
    public int getTransitionThreshold() {
        return transitionThreshold;
    }
    
    public void setTransitionThreshold(int transitionThreshold) {
        this.transitionThreshold = transitionThreshold;
    }
    
    public String getTransitionMessage() {
        return transitionMessage;
    }
    
    public void setTransitionMessage(String transitionMessage) {
        this.transitionMessage = transitionMessage;
    }
    
    public boolean isTransitioned() {
        return transitioned;
    }
    
    public void setTransitioned(boolean transitioned) {
        this.transitioned = transitioned;
    }
}
