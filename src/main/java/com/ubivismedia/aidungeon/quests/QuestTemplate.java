package com.ubivismedia.aidungeon.quests;

import java.util.List;

/**
 * Represents a quest template from which player quests are created
 */
public class QuestTemplate {
    
    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final int requiredAmount;
    private final String targetEntity;
    private final String targetItem;
    private final List<String> rewardCommands;
    private final List<String> rewardMessages;
    private final List<String> rewardItems;
    
    /**
     * Create a new quest template
     */
    public QuestTemplate(String id, String name, String description, QuestType type, int requiredAmount,
                         String targetEntity, String targetItem, List<String> rewardCommands,
                         List<String> rewardMessages, List<String> rewardItems) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requiredAmount = requiredAmount;
        this.targetEntity = targetEntity;
        this.targetItem = targetItem;
        this.rewardCommands = rewardCommands;
        this.rewardMessages = rewardMessages;
        this.rewardItems = rewardItems;
    }
    
    /**
     * Get the template ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the quest name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the quest description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the quest type (KILL, COLLECT, EXPLORE)
     */
    public QuestType getType() {
        return type;
    }
    
    /**
     * Get the required amount to complete the quest
     */
    public int getRequiredAmount() {
        return requiredAmount;
    }
    
    /**
     * Get the target entity type (for KILL quests)
     */
    public String getTargetEntity() {
        return targetEntity;
    }
    
    /**
     * Get the target item type (for COLLECT quests)
     */
    public String getTargetItem() {
        return targetItem;
    }
    
    /**
     * Get the reward commands to execute when claiming
     */
    public List<String> getRewardCommands() {
        return rewardCommands;
    }
    
    /**
     * Get the messages to show when claiming rewards
     */
    public List<String> getRewardMessages() {
        return rewardMessages;
    }
    
    /**
     * Get the reward items to give when claiming
     */
    public List<String> getRewardItems() {
        return rewardItems;
    }
}
