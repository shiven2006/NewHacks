package com.example.frontend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainGoalModel {
    private int id;   // give each goal a unique ID -- do we actually need this??
    private String title;  // a title for the goal
    private String description;   // the goal itself (what the user inputs)
    private Date createdAt;
    private List<SubgoalModel> subgoalsList;
    private Date deadline;  // deadline for the goal
    private boolean isComplete;
    private int numTotalSubgoals;
    private int numCompletedSubgoals;

    public MainGoalModel(int id, String title, String description, Date createdAt, List<SubgoalModel> subgoalsList, Date deadline, boolean isComplete, int numTotalSubgoals, int numCompletedSubgoals) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.subgoalsList = subgoalsList;
        this.deadline = deadline;
        this.isComplete = isComplete;
        this.numTotalSubgoals = numTotalSubgoals;
        this.numCompletedSubgoals = numCompletedSubgoals;
    }

    public MainGoalModel() {

    }

    public static MainGoalModel ParseJSON(String jsonString) throws JSONException, ParseException {
        MainGoalModel model = new MainGoalModel();
        JSONObject object = new JSONObject(jsonString);
        model.id = Integer.parseInt(object.getString("id"));
        model.title = object.getString("title");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        model.deadline = sdf.parse(object.getString("deadline"));
//        model.numTotalSubgoals = Integer.parseInt(object.getString("numTotalSubgoals"));
//        model.numCompletedSubgoals = 0;
        JSONArray array = object.getJSONArray("subgoals");
        model.subgoalsList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject subgoalJSONObject = array.getJSONObject(i);
            int goalId = Integer.parseInt(subgoalJSONObject.getString("goalId"));
            String subgoalTitle = subgoalJSONObject.getString("title");
            String subgoalDescription = subgoalJSONObject.getString("description");
            Boolean subgoalIsCompleted = Boolean.parseBoolean(subgoalJSONObject.getString("isCompleted"));
            model.subgoalsList.add(new SubgoalModel(goalId, subgoalTitle, subgoalDescription, subgoalIsCompleted));
        }
        model.isComplete = false;
        return model;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public List<SubgoalModel> getSubgoalsList() {
        return subgoalsList;
    }

    public Date getDeadline() {
        return deadline;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public int getNumTotalSubgoals() {
        return numTotalSubgoals;
    }

    public int getNumCompletedSubgoals() {
        return numCompletedSubgoals;
    }
}
