package com.example.frontend;

public class SubgoalModel {
    private int mainGoalId;

    private String title;
    private String description;
    private boolean isCompleted;

    public SubgoalModel(int mainGoalId, String title, String description, boolean isCompleted) {
        this.mainGoalId = mainGoalId;
        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}
