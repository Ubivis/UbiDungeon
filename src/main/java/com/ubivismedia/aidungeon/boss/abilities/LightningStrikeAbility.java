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
 * Strikes lightning at random positions around targets
 */
public class LightningStrikeAbility extends BossAbility {
    private final int count;
    private final double radius;
    
    public LightningStrikeAbility(int count, double radius) {
        this.count = count;
        this.radius = radius;
    }
    
    @Override
    public void execute(LivingEntity boss, List<Player> targets) {
        if (targets.isEmpty()) return;
        
        // Get random players to target
        List<Player> shuffledTargets = new ArrayList<>(targets);
        java.util.Collections.shuffle(shuffledTargets);
        
        for (int i = 0; i < Math.min(count, targets.size()); i++) {
            Player target = shuffledTargets.get(i % shuffledTargets.size());
            Location location = target.getLocation();
            
            // Calculate random position around player
            double angle = Math.random() * 2 * Math.PI;
            double distance = Math.random() * radius;
            double x = location.getX() + distance * Math.cos(angle);
            double z = location.getZ() + distance * Math.sin(angle);
            
            // Strike lightning
            Location strikeLoc = new Location(location.getWorld(), x, location.getY(), z);
            location.getWorld().strikeLightning(strikeLoc);
            
            // Send warning
            target.sendMessage("§c§oLightning strikes around you!");
        }
    }
}
