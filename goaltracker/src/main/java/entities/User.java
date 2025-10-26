package entities;

import java.util.ArrayList;
import java.util.List;

/**
 * All current methods:
 * getters + setters for all variables, add + remove goal (from goalList)
 * Should we override toString()?
 */

public class User {
    private int userId;
    private String name;
    private List<Goal> goalList;  // each user can have a list of goals (more than one goal)

    /**
     * Default constructor initializes an empty goal list.
     */
    public User() {
        this.goalList = new ArrayList<>();
    }

    /**
     * Constructor for creating a User with an id and name.
     *
     * @param userId   the unique identifier for the user
     * @param name the user's name
     */
    public User(int userId, String name) {
        this.userId = userId;
        this.name = name;
        this.goalList = new ArrayList<>();
    }

    public int getId() {
        return userId;
    }

    public void setId(int id) {
        this.userId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Goal> getGoalList() {
        return goalList;
    }

    public void setGoalList(List<Goal> goalList) {
        this.goalList = goalList;
    }

    /**
     * Adds a new goal to the user's goal list.
     *
     * @param goal the Goal object to add
     */
    public void addGoal(Goal goal) {
        if (goal != null) {
            goalList.add(goal);
        }
    }

    /**
     * Removes a goal from the user's goal list.
     *
     * @param goal the Goal object to remove
     */
    public void removeGoal(Goal goal) {
        if (goal != null) {
            goalList.remove(goal);
        }
    }

}
