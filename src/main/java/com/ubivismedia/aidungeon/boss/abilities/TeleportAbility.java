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
 * Teleports the boss to a random location near a target
 */
public class TeleportAbility extends BossAbility {
    private final double radius;
    
    public TeleportAbility(double radius) {
        this.radius = radius;
    }
    
    @Override
    public void execute(LivingEntity boss, List<Player> targets) {
        if (targets.isEmpty()) return;
        
        // Get random target
        Player target = targets.get((int) (Math.random() * targets.size()));
        Location targetLocation = target.getLocation();
        
        // Calculate random position around player
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * radius;
        double x = targetLocation.getX() + distance * Math.cos(angle);
        double z = targetLocation.getZ() + distance * Math.sin(angle);
        
        // Find safe Y
        Location teleportLoc = new Location(
                targetLocation.getWorld(),
                x,
                targetLocation.getY(),
                z,
                (float) (Math.random() * 360),
                0
        );
        
        // Play particles at old location
        boss.getWorld().spawnParticle(
                org.bukkit.Particle.PORTAL,
                boss.getLocation(),
                50,
                0.5,
                1,
                0.5,
                0.1
        );
        
        // Teleport
        boss.teleport(teleportLoc);
        
        // Play particles at new location
        boss.getWorld().spawnParticle(
                org.bukkit.Particle.PORTAL,
                teleportLoc,
                50,
                0.5,
                1,
                0.5,
                0.1
        );
        
        // Play sound
        boss.getWorld().playSound(
                teleportLoc,
                org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
                1.0f,
                0.5f
        );
    }
}
