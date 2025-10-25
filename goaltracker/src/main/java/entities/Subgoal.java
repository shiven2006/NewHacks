package entities;

import entities.Goal;
import java.time.LocalDate;
import java.util.Date;

/**
 * All current methods:
 * getters + setters for all variables,
 */

public class Subgoal {
    private int goalId;  // id of the Goal this is a subgoal of
    private Goal originalGoal; // the goal it is a subgoal of
    private String title;
    private String description;
    private boolean isCompleted;
    // private LocalDate deadline;

    // Constructor
    /**
     * Default constructor.
     * Initializes a Subgoal with default values.
     */
    public Subgoal() {
        this.isCompleted = false;
        // this.deadline = null;
    }

    /**
     * Constructor for a simple subgoal without deadline.
     * @param title       short title of the subgoal
     * @param description description of the subgoal
     */
    public Subgoal(String title, String description) {
        this.description = description;
        this.title = title;
        this.isCompleted = false;
        // this.deadline = null;
    }

    /**
     * Full constructor for creating a subgoal with all fields.
     *
     * @param goalId      ID of the parent goal
     * @param title       title of the subgoal
     * @param description description of the subgoal
     */
    public Subgoal(int goalId, String title, String description) {
        this.goalId = goalId;
        this.title = title;
        this.description = description;
        this.isCompleted = false;
    }

    // Getters & setters

    public int getGoalId() {
        return goalId;
    }

    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    public Goal getOriginalGoal() {
        return originalGoal;
    }

    public void getOriginalGoal(Goal originalGoal) {
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

    public boolean getIsCompleted() {
        return isCompleted;
    }


    /**public LocalDate getDeadline() {
     return deadline;
     }

     public void setDeadline(LocalDate deadline) {
     this.deadline = deadline;
     }
     */

    /**
     * Marks this subgoal as completed.
     */
    public void markComplete() {
        this.isCompleted = true;
        this.originalGoal.removeSubgoal(this);
    }


}

