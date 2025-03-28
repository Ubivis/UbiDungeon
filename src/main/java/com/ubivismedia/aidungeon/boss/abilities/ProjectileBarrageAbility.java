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
 * Fires multiple projectiles in a pattern
 */
public class ProjectileBarrageAbility extends BossAbility {
    private final String projectileType;
    private final int count;
    private final double speed;
    private final double spread;
    
    public ProjectileBarrageAbility(String projectileType, int count, double speed, double spread) {
        this.projectileType = projectileType;
        this.count = count;
        this.speed = speed;
        this.spread = spread;
    }
    
    @Override
    public void execute(LivingEntity boss, List<Player> targets) {
        if (targets.isEmpty()) return;
        
        // Get the closest target
        Player target = targets.get(0);
        double minDistance = target.getLocation().distance(boss.getLocation());
        
        for (Player player : targets) {
            double distance = player.getLocation().distance(boss.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                target = player;
            }
        }
        
        // Calculate direction to target
        org.bukkit.util.Vector direction = target.getLocation().toVector()
                .subtract(boss.getLocation().toVector())
                .normalize();
        
        // Launch projectiles
        for (int i = 0; i < count; i++) {
            // Calculate spread angle
            double angle = ((i * spread) / count) - (spread / 2);
            double radians = Math.toRadians(angle);
            
            // Rotate direction vector
            double x = direction.getX() * Math.cos(radians) - direction.getZ() * Math.sin(radians);
            double z = direction.getX() * Math.sin(radians) + direction.getZ() * Math.cos(radians);
            
            org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(x, direction.getY(), z).multiply(speed);
            
            // Launch projectile
            switch (projectileType.toUpperCase()) {
                case "ARROW":
                    boss.launchProjectile(org.bukkit.entity.Arrow.class, velocity);
                    break;
                case "FIREBALL":
                    boss.launchProjectile(org.bukkit.entity.Fireball.class, velocity);
                    break;
                case "SNOWBALL":
                    boss.launchProjectile(org.bukkit.entity.Snowball.class, velocity);
                    break;
                case "TRIDENT":
                    boss.launchProjectile(org.bukkit.entity.Trident.class, velocity);
                    break;
                default:
                    boss.launchProjectile(org.bukkit.entity.Arrow.class, velocity);
                    break;
            }
        }
        
        // Play sound
        boss.getWorld().playSound(
                boss.getLocation(),
                org.bukkit.Sound.ENTITY_BLAZE_SHOOT,
                1.0f,
                0.5f
        );
    }
}
