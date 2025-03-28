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
 * Represents an active boss instance
 */
public class DungeonBoss {
    private final BossTemplate template;
    private int currentPhaseIndex = 0;
    
    public DungeonBoss(BossTemplate template) {
        this.template = template;
    }
    
    public BossTemplate getTemplate() {
        return template;
    }
    
    public String getName() {
        return template.getName();
    }
    
    public BossPhase getCurrentPhase() {
        return template.getPhases().get(currentPhaseIndex);
    }
    
    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }
    
    public void setCurrentPhaseIndex(int currentPhaseIndex) {
        if (currentPhaseIndex >= 0 && currentPhaseIndex < template.getPhases().size()) {
            this.currentPhaseIndex = currentPhaseIndex;
        }
    }
    
    public List<BossPhase> getPhases() {
        return template.getPhases();
    }
}
