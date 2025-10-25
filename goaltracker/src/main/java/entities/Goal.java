package entities;

import entities.Subgoal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * All current methods:
 * getters + setters for all variables, add + remove subgoals (from subgoalsList), check for goal completion,
 * calculate goal progress
 * Should we override toString()?
 */

public class Goal {
    private int id;   // give each goal a unique ID -- do we actually need this??
    private String title;  // a title for the goal
    private String description;   // the goal itself (what the user inputs)
    private LocalDate createdAt;
    private List<Subgoal> subgoalsList;
    private LocalDate deadline;  // deadline for the goal
    private boolean isComplete;
    private int numTotalSubgoals;
    private int numCompletedSubgoals;

    /**
     * Default constructor initializes an empty goal with an empty subgoal list.
     */
    public Goal() {
        this.createdAt = LocalDate.now();
        this.subgoalsList = new ArrayList<>();
        this.isComplete = false;
    }

    /**
     * Constructor for creating a goal with title, description and deadline.
     *
     * @param title       the title of the goal
     * @param description a short description of the goal
     * @param deadline  the deadline for the goal (when you hope to complete it by)
     */
    public Goal(String title, String description, LocalDate deadline) {
        this.title = title;
        this.description = description;
        this.createdAt = LocalDate.now();
        this.subgoalsList = new ArrayList<>();
        this.isComplete = false;
        this.deadline = deadline;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public List<Subgoal> getSubgoals() {
        return subgoalsList;
    }

    public void setSubgoals(List<Subgoal> subgoals) {
        this.subgoalsList = subgoals;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public int getNumTotalSubgoals() {
        return numTotalSubgoals;
    }

    public void setNumTotalSubgoals(int numTotalSubgoals) {
        this.numTotalSubgoals = numTotalSubgoals;
    }

    public int getNumCompletedSubgoals() {
        return numCompletedSubgoals;
    }


    /**
     * Adds a new subgoal to this goal.
     *
     * @param subgoal the Subgoal to add
     */
    public void addSubgoal(Subgoal subgoal) {
        if (subgoal != null) {
            subgoalsList.add(subgoal);
            numTotalSubgoals++;
        }
    }

    /**
     * Removes a subgoal from this goal.
     * DO NOT USE THIS TO MARK SUBGOALS AS COMPLETE
     * @param subgoal the Subgoal to remove
     */
    public void removeSubgoal(Subgoal subgoal) {
        subgoalsList.remove(subgoal);
        numTotalSubgoals--;
    }

    /**
     * Checks if the goal is complete. Goal is complete iff all the subgoals are complete.
     * If all subgoals are complete, return true & update isComplete = true.
     * @return boolean
     */
    public boolean checkGoalCompletion() {
        for (Subgoal subgoal : subgoalsList) {
            if (!subgoal.getIsCompleted()) {
                return false;
            }
        }
        isComplete = true;
        return true;
    }

    /**
     * Calculates the progress of the goal. Progress = # completed subgoals / total # subgoals
     * @return float
     */
    public float calculateProgress() {
        return (float) numCompletedSubgoals / numTotalSubgoals;
    }


}
