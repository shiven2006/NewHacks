package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.*;

public class MainPageActivity extends AppCompatActivity {

    private static final String TAG = "MainPageActivity";

    private TextView tvBigGoal, tvSubgoal;
    private CheckBox cbSubgoal;
    private ImageView btnPrevGoal, btnNextGoal, imgPlant;
    private ProgressBar progressTasks;
    private ImageButton btnAddGoal, btnCollection;

    private OkHttpClient client;
    private static final String BASE_URL = "http://10.0.2.2:8080/api/goals";

    // Frontend Goal management
    static class Goal {
        int id;
        String title;
        String[] subgoals;
        boolean[] completed;
        int currentSubgoalIndex;
        int plantStage;

        Goal(int id, String title, String[] subgoals) {
            this.id = id;
            this.title = title;
            this.subgoals = subgoals;
            this.completed = new boolean[subgoals.length];
            this.currentSubgoalIndex = 0;
            this.plantStage = 0;
        }

        int getProgress() {
            int done = 0;
            for (boolean b : completed) if (b) done++;
            return (done * 100) / subgoals.length;
        }

        boolean isAllDone() {
            for (boolean b : completed) if (!b) return false;
            return true;
        }
    }

    private List<Goal> goalList = new ArrayList<>();
    private int currentGoalIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);

        // Initialize views
        tvBigGoal = findViewById(R.id.tvBigGoal);
        btnPrevGoal = findViewById(R.id.btnPrevGoal);
        btnNextGoal = findViewById(R.id.btnNextGoal);
        tvSubgoal = findViewById(R.id.tvSubgoal);
        cbSubgoal = findViewById(R.id.checkboxSubgoal);
        imgPlant = findViewById(R.id.imgPlant);
        progressTasks = findViewById(R.id.progressTasks);
        btnAddGoal = findViewById(R.id.btnAddGoal);
        btnCollection = findViewById(R.id.btnCollection);

        // Initialize HTTP client
        client = new OkHttpClient();

        // ✅ Load all goals from backend on startup
        loadAllGoalsFromBackend();

        // Navigation buttons
        btnPrevGoal.setOnClickListener(v -> {
            if (!goalList.isEmpty()) {
                currentGoalIndex = (currentGoalIndex - 1 + goalList.size()) % goalList.size();
                showCurrentGoal();
            }
        });

        btnNextGoal.setOnClickListener(v -> {
            if (!goalList.isEmpty()) {
                currentGoalIndex = (currentGoalIndex + 1) % goalList.size();
                showCurrentGoal();
            }
        });

        // Checkbox for completing subgoals
        cbSubgoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (goalList.isEmpty()) return;
            Goal g = goalList.get(currentGoalIndex);

            if (isChecked) {
                g.completed[g.currentSubgoalIndex] = true;

                // ✅ Update backend: mark subgoal as complete
                markSubgoalComplete(g.id, g.subgoals[g.currentSubgoalIndex], true);

                // Update plant stage based on progress
                int progress = g.getProgress();
                if (progress >= 100) g.plantStage = 3;
                else if (progress >= 66) g.plantStage = 2;
                else if (progress >= 33) g.plantStage = 1;
                else g.plantStage = 0;

                Toast.makeText(this, "Subgoal finished ✅", Toast.LENGTH_SHORT).show();

                // Move to next subgoal or show completion
                if (g.currentSubgoalIndex < g.subgoals.length - 1) {
                    g.currentSubgoalIndex++;
                } else {
                    if (g.isAllDone()) {
                        showCompletionAnimation(g);
                    } else {
                        Toast.makeText(this, "🎉 All subgoals are done!", Toast.LENGTH_SHORT).show();
                    }
                }

                showCurrentGoal();
            }
        });

        imgPlant.setOnClickListener(v -> {
            if (!goalList.isEmpty()) {
                Goal currentGoal = goalList.get(currentGoalIndex);

                Intent intent = new Intent(MainPageActivity.this, PlantDetailActivity.class);
                intent.putExtra("GOAL_ID", currentGoal.id);
                intent.putExtra("GOAL_TITLE", currentGoal.title);
                startActivity(intent);
            } else {
                Toast.makeText(MainPageActivity.this, "No goal to display", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddGoal.setOnClickListener(v ->
                startActivity(new Intent(MainPageActivity.this, CreateGoalActivity.class))
        );

        btnCollection.setOnClickListener(v ->
                startActivity(new Intent(MainPageActivity.this, CollectionActivity.class))
        );
    }

    // ================== BACKEND API CALLS ==================

    /**
     * ✅ GET /api/goals/ - Load all goals from backend
     */
    private void loadAllGoalsFromBackend() {
        Request request = new Request.Builder()
                .url(BASE_URL + "/")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load goals", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainPageActivity.this,
                            "Failed to load goals: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showCurrentGoal();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        Log.d(TAG, "Response JSON: " + jsonData);

                        // ✅ Parse JSON using built-in JSONArray
                        JSONArray jsonArray = new JSONArray(jsonData);
                        List<ApiGoal> apiGoals = parseGoalsFromJson(jsonArray);

                        runOnUiThread(() -> {
                            updateGoalListFromApi(apiGoals);
                            showCurrentGoal();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing goals", e);
                        runOnUiThread(() -> {
                            Toast.makeText(MainPageActivity.this,
                                    "Error parsing goals: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Server returned error: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(MainPageActivity.this,
                                "Failed to load goals from server (HTTP " + response.code() + ")",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * ✅ Parse JSON array into ApiGoal objects with better error handling
     */
    private List<ApiGoal> parseGoalsFromJson(JSONArray jsonArray) throws Exception {
        List<ApiGoal> goals = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject goalJson = jsonArray.getJSONObject(i);

                ApiGoal apiGoal = new ApiGoal();

                // ✅ Handle ID - could be int or string
                if (goalJson.has("id")) {
                    Object idObj = goalJson.get("id");
                    if (idObj instanceof Integer) {
                        apiGoal.id = (Integer) idObj;
                    } else if (idObj instanceof String) {
                        apiGoal.id = Integer.parseInt((String) idObj);
                    } else {
                        apiGoal.id = goalJson.getInt("id");
                    }
                }

                // ✅ Handle title
                apiGoal.title = goalJson.optString("title", "Untitled Goal");
                apiGoal.description = goalJson.optString("description", "");
                apiGoal.deadline = goalJson.optString("deadline", "");

                // ✅ Parse subgoals array
                apiGoal.subgoals = new ArrayList<>();

                if (goalJson.has("subgoals") && !goalJson.isNull("subgoals")) {
                    JSONArray subgoalsJson = goalJson.getJSONArray("subgoals");

                    for (int j = 0; j < subgoalsJson.length(); j++) {
                        try {
                            JSONObject subgoalJson = subgoalsJson.getJSONObject(j);

                            ApiSubgoal apiSubgoal = new ApiSubgoal();

                            // ✅ Handle goalId
                            if (subgoalJson.has("goalId")) {
                                Object goalIdObj = subgoalJson.get("goalId");
                                if (goalIdObj instanceof Integer) {
                                    apiSubgoal.goalId = (Integer) goalIdObj;
                                } else if (goalIdObj instanceof String) {
                                    apiSubgoal.goalId = Integer.parseInt((String) goalIdObj);
                                } else {
                                    apiSubgoal.goalId = subgoalJson.getInt("goalId");
                                }
                            }

                            apiSubgoal.title = subgoalJson.optString("title", "Untitled Subgoal");
                            apiSubgoal.description = subgoalJson.optString("description", "");

                            // ✅ Handle completed - could be boolean, int, or string
                            if (subgoalJson.has("completed")) {
                                Object completedObj = subgoalJson.get("completed");
                                if (completedObj instanceof Boolean) {
                                    apiSubgoal.completed = (Boolean) completedObj;
                                } else if (completedObj instanceof Integer) {
                                    apiSubgoal.completed = ((Integer) completedObj) != 0;
                                } else if (completedObj instanceof String) {
                                    apiSubgoal.completed = Boolean.parseBoolean((String) completedObj);
                                } else {
                                    apiSubgoal.completed = subgoalJson.getBoolean("completed");
                                }
                            } else {
                                apiSubgoal.completed = false;
                            }

                            apiGoal.subgoals.add(apiSubgoal);

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing subgoal " + j, e);
                            // Continue with next subgoal
                        }
                    }
                }

                goals.add(apiGoal);
                Log.d(TAG, "Parsed goal: " + apiGoal.title + " with " + apiGoal.subgoals.size() + " subgoals");

            } catch (Exception e) {
                Log.e(TAG, "Error parsing goal " + i, e);
                // Continue with next goal
            }
        }

        return goals;
    }

    /**
     * ✅ PATCH /api/goals/{goalId}/subgoals/complete - Mark subgoal as complete
     */
    private void markSubgoalComplete(int goalId, String subgoalTitle, boolean completed) {
        try {
            // ✅ Create JSON request body using JSONObject
            JSONObject requestJson = new JSONObject();
            requestJson.put("title", subgoalTitle);
            requestJson.put("completed", completed);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/" + goalId + "/subgoals/complete")
                    .patch(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Failed to update subgoal", e);
                    runOnUiThread(() ->
                            Toast.makeText(MainPageActivity.this,
                                    "Failed to update subgoal",
                                    Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Subgoal updated in backend");
                    } else {
                        Log.e(TAG, "Failed to update subgoal: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating request", e);
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ DELETE /api/goals/{id} - Delete goal from backend
     */
    private void deleteGoalFromBackend(int goalId, Goal goal) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/" + goalId)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to delete goal", e);
                runOnUiThread(() ->
                        Toast.makeText(MainPageActivity.this,
                                "Failed to delete goal from server",
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Goal deleted from backend");
                    runOnUiThread(() -> {
                        // Remove from local list
                        goalList.remove(goal);
                        if (goalList.isEmpty()) {
                            tvBigGoal.setText("No more tasks!");
                            tvSubgoal.setText("");
                            cbSubgoal.setEnabled(false);
                        } else {
                            if (currentGoalIndex >= goalList.size()) {
                                currentGoalIndex = 0;
                            }
                            showCurrentGoal();
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to delete goal: " + response.code());
                }
            }
        });
    }

    // ================== UI UPDATES ==================

    /**
     * Convert API goals to frontend Goal objects
     */
    private void updateGoalListFromApi(List<ApiGoal> apiGoals) {
        goalList.clear();

        for (ApiGoal apiGoal : apiGoals) {
            // Skip goals with no subgoals
            if (apiGoal.subgoals == null || apiGoal.subgoals.isEmpty()) {
                Log.w(TAG, "Skipping goal with no subgoals: " + apiGoal.title);
                continue;
            }

            // Extract subgoal titles and completion states
            String[] subTitles = new String[apiGoal.subgoals.size()];
            boolean[] completedStates = new boolean[apiGoal.subgoals.size()];

            for (int i = 0; i < apiGoal.subgoals.size(); i++) {
                subTitles[i] = apiGoal.subgoals.get(i).title;
                completedStates[i] = apiGoal.subgoals.get(i).completed;
            }

            Goal goal = new Goal(apiGoal.id, apiGoal.title, subTitles);
            goal.completed = completedStates;

            // Find current subgoal index (first incomplete one)
            goal.currentSubgoalIndex = 0;
            for (int i = 0; i < completedStates.length; i++) {
                if (!completedStates[i]) {
                    goal.currentSubgoalIndex = i;
                    break;
                }
            }

            // Calculate plant stage based on progress
            int progress = goal.getProgress();
            if (progress >= 100) goal.plantStage = 3;
            else if (progress >= 66) goal.plantStage = 2;
            else if (progress >= 33) goal.plantStage = 1;
            else goal.plantStage = 0;

            goalList.add(goal);
        }

        currentGoalIndex = 0;
        Log.d(TAG, "✅ Loaded " + goalList.size() + " goals from backend");
    }

    /**
     * Display current goal on screen
     */
    private void showCurrentGoal() {
        if (goalList.isEmpty()) {
            tvBigGoal.setText("No tasks!");
            tvSubgoal.setText("");
            cbSubgoal.setEnabled(false);
            progressTasks.setProgress(0);
            imgPlant.setImageResource(R.drawable.ic_plant_stage1);
            return;
        }

        Goal g = goalList.get(currentGoalIndex);
        tvBigGoal.setText(g.title);
        tvSubgoal.setText(g.subgoals[g.currentSubgoalIndex]);
        cbSubgoal.setChecked(g.completed[g.currentSubgoalIndex]);
        cbSubgoal.setEnabled(!g.completed[g.currentSubgoalIndex]);
        progressTasks.setProgress(g.getProgress());

        // Update plant image based on stage
        switch (g.plantStage) {
            case 0: imgPlant.setImageResource(R.drawable.ic_plant_stage1); break;
            case 1: imgPlant.setImageResource(R.drawable.ic_plant_stage2); break;
            case 2: imgPlant.setImageResource(R.drawable.ic_plant_stage3); break;
            default: imgPlant.setImageResource(R.drawable.ic_plant_stage4); break;
        }
    }

    /**
     * Show celebration animation when goal is complete
     */
    private void showCompletionAnimation(Goal g) {
        ImageView confetti = findViewById(R.id.imgCelebrate);
        confetti.setVisibility(View.VISIBLE);
        confetti.setScaleX(0f);
        confetti.setScaleY(0f);
        confetti.setAlpha(0f);

        confetti.animate()
                .scaleX(1.5f).scaleY(1.5f).alpha(1f).setDuration(600)
                .withEndAction(() -> {
                    confetti.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                        confetti.setVisibility(View.GONE);
                        animatePlantToCollection(g);
                    }).start();
                }).start();
    }

    /**
     * Animate plant moving to collection and delete from backend
     */
    private void animatePlantToCollection(Goal g) {
        int plantDrawable = getPlantDrawableForGoal(g);
        CollectionManager.getInstance().addPlant(plantDrawable);

        int[] plantLoc = new int[2];
        int[] starLoc = new int[2];
        imgPlant.getLocationOnScreen(plantLoc);
        btnCollection.getLocationOnScreen(starLoc);

        float deltaX = starLoc[0] - plantLoc[0];
        float deltaY = starLoc[1] - plantLoc[1];

        imgPlant.animate()
                .translationX(deltaX).translationY(deltaY)
                .scaleX(0.3f).scaleY(0.3f).setDuration(800)
                .withEndAction(() -> {
                    Toast.makeText(this, g.title + " Added to collection 🌿", Toast.LENGTH_SHORT).show();

                    // ✅ Delete from backend
                    deleteGoalFromBackend(g.id, g);

                    // Reset plant animation
                    imgPlant.setTranslationX(0);
                    imgPlant.setTranslationY(0);
                    imgPlant.setScaleX(1f);
                    imgPlant.setScaleY(1f);
                }).start();
    }

    private int getPlantDrawableForGoal(Goal g) {
        switch (g.plantStage) {
            case 1: return R.drawable.ic_plant_stage2;
            case 2: return R.drawable.ic_plant_stage3;
            case 3: return R.drawable.ic_plant_stage4;
            default: return R.drawable.ic_plant_stage1;
        }
    }

    // ================== API MODELS ==================

    /**
     * Backend Goal model (matches server response)
     */
    public static class ApiGoal {
        int id;
        String title;
        String description;
        String deadline;
        List<ApiSubgoal> subgoals;
    }

    /**
     * Backend Subgoal model (matches server response)
     */
    public static class ApiSubgoal {
        int goalId;
        String title;
        String description;
        boolean completed;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Reload goals when returning to this activity
        loadAllGoalsFromBackend();
    }
}