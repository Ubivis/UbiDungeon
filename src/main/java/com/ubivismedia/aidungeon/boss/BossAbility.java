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
 * Base class for boss abilities
 */
public abstract class BossAbility {
    private int cooldownTicks;
    private int remainingCooldown = 0;
    
    public abstract void execute(LivingEntity boss, List<Player> targets);
    
    public void setCooldown(int cooldownSeconds) {
        this.cooldownTicks = cooldownSeconds * 20; // Convert to ticks
        this.remainingCooldown = 0;
    }
    
    public void tickCooldown() {
        if (remainingCooldown > 0) {
            remainingCooldown--;
        }
    }
    
    public boolean isReady() {
        return remainingCooldown <= 0;
    }
    
    public void resetCooldown() {
        remainingCooldown = cooldownTicks;
    }
}
