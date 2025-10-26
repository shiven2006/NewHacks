package com.example.frontend;

import java.util.ArrayList;

public class MainGoalModel {
    String name;
    String description;
    ArrayList<SubgoalModel> subgoals;

    public MainGoalModel(String name, String description, ArrayList<SubgoalModel> subgoals) {
        this.name = name;
        this.description = description;
        this.subgoals = subgoals;
    }
    public String getName(){
        return name;
    }
    public String getDescription(){
        return description;
    }
    public ArrayList<SubgoalModel> getSubgoals() {
        return subgoals;
    }
}
