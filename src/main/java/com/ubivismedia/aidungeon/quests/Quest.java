package com.ubivismedia.aidungeon.quests;

/**
 * Represents a quest assigned to a player
 */
public class Quest {
    
    private final String id;
    private final QuestTemplate template;
    private final String dungeonId;
    private int progress;
    private boolean completed;
    private boolean rewardClaimed;
    
    /**
     * Create a new quest instance
     */
    public Quest(String id, QuestTemplate template, String dungeonId, int progress, boolean completed, boolean rewardClaimed) {
        this.id = id;
        this.template = template;
        this.dungeonId = dungeonId;
        this.progress = progress;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
    }
    
    /**
     * Get the unique ID of this quest
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the template this quest is based on
     */
    public QuestTemplate getTemplate() {
        return template;
    }
    
    /**
     * Get the dungeon ID this quest is for
     */
    public String getDungeonId() {
        return dungeonId;
    }
    
    /**
     * Get the current progress (number of objective items collected/killed/etc)
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * Set the current progress
     */
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    /**
     * Check if the quest is completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Mark the quest as completed
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Check if the reward has been claimed
     */
    public boolean isRewardClaimed() {
        return rewardClaimed;
    }
    
    /**
     * Mark the reward as claimed
     */
    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
    
    /**
     * Get the percentage of completion (0-100)
     */
    public int getCompletionPercentage() {
        int required = template.getRequiredAmount();
        if (required <= 0) {
            return 100;
        }
        
        return Math.min(100, (progress * 100) / required);
    }
}
