package com.ubivismedia.aidungeon.boss.abilities;

import com.ubivismedia.aidungeon.boss.BossAbility;

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
 * Summons minions around the boss
 */
public class MinionSummonAbility extends BossAbility {
    private final EntityType entityType;
    private final int count;
    private final double radius;
    
    public MinionSummonAbility(EntityType entityType, int count, double radius) {
        this.entityType = entityType;
        this.count = count;
        this.radius = radius;
    }
    
    @Override
    public void execute(LivingEntity boss, List<Player> targets) {
        Location location = boss.getLocation();
        
        for (int i = 0; i < count; i++) {
            // Calculate random position around boss
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = location.getX() + distance * Math.cos(angle);
            double z = location.getZ() + distance * Math.sin(angle);
            
            // Find safe Y
            Location spawnLoc = new Location(location.getWorld(), x, location.getY(), z);
            
            // Spawn minion
            LivingEntity minion = (LivingEntity) location.getWorld().spawnEntity(spawnLoc, entityType);
            
            // Mark as minion
            minion.setCustomName(boss.getCustomName() + "'s Minion");
            minion.setCustomNameVisible(true);
        }
        
        // Play effect
        location.getWorld().strikeLightningEffect(location);
    }
}