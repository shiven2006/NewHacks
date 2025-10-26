package entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Subgoal {
    private int goalId;

    @JsonIgnore  // ✅ Prevents circular reference in JSON
    private Goal originalGoal;

    private String title;
    private String description;
    private boolean isCompleted;  // ✅ Changed from public to private

    // Constructors
    /**
     * Default constructor.
     */
    public Subgoal() {
        this.isCompleted = false;
    }

    /**
     * Constructor for a simple subgoal without deadline.
     */
    public Subgoal(String title, String description) {
        this.title = title;
        this.description = description;
        this.isCompleted = false;
    }

    /**
     * Full constructor.
     */
    public Subgoal(int goalId, String title, String description) {
        this.goalId = goalId;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
    }

    // Getters & Setters
    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public Goal getOriginalGoal() {
        return originalGoal;
    }

    public void setOriginalGoal(Goal originalGoal) {
        this.originalGoal = originalGoal;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ✅ Fixed: Proper getter name for boolean
    public boolean isCompleted() {
        return isCompleted;
    }

    // ✅ Fixed: Better setter that doesn't modify originalGoal
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        // Only remove from original goal if it exists and is being marked complete
        if (completed && this.originalGoal != null) {
            this.originalGoal.removeSubgoal(this);
        }
    }

    /**
     * Marks this subgoal as completed.
     */
    public void markCompleted() {
        setCompleted(true);
    }

    @Override
    public String toString() {
        return "Subgoal{" +
                "goalId=" + goalId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", isCompleted=" + isCompleted +
                '}';
    }

    public boolean getIsCompleted() {
        return isCompleted;
    }
}