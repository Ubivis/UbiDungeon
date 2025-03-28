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
 * Creates an area effect that applies a potion effect
 */
public class AreaEffectAbility extends BossAbility {
    private final PotionEffectType effectType;
    private final int duration;
    private final int amplifier;
    private final double radius;
    
    public AreaEffectAbility(PotionEffectType effectType, int duration, int amplifier, double radius) {
        this.effectType = effectType;
        this.duration = duration;
        this.amplifier = amplifier;
        this.radius = radius;
    }
    
    @Override
    public void execute(LivingEntity boss, List<Player> targets) {
        Location location = boss.getLocation();
        
        // Create area effect cloud
        org.bukkit.entity.AreaEffectCloud cloud = (org.bukkit.entity.AreaEffectCloud) location.getWorld().spawnEntity(
                location,
                EntityType.AREA_EFFECT_CLOUD
        );
        
        // Set properties
        cloud.setRadius((float) radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(0); // Don't shrink
        cloud.setColor(org.bukkit.Color.fromRGB(139, 0, 0));
        cloud.addCustomEffect(
                new org.bukkit.potion.PotionEffect(effectType, duration, amplifier),
                true
        );
    }
}
