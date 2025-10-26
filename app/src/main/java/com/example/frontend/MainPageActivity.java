package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.android.volley.VolleyError;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainPageActivity extends AppCompatActivity {

    private TextView tvBigGoal, tvSubgoal;
    private CheckBox cbSubgoal;
    private ImageView btnPrevGoal, btnNextGoal, imgPlant;
    private ProgressBar progressTasks;
    private ImageButton btnAddGoal, btnCollection;

    // 前端管理 Goal
    static class Goal {
        String title;
        String[] subgoals;
        boolean[] completed;
        int currentSubgoalIndex;
        int plantStage;

        Goal(String title, String[] subgoals) {
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

        tvBigGoal = findViewById(R.id.tvBigGoal);
        btnPrevGoal = findViewById(R.id.btnPrevGoal);
        btnNextGoal = findViewById(R.id.btnNextGoal);
        tvSubgoal = findViewById(R.id.tvSubgoal);
        cbSubgoal = findViewById(R.id.checkboxSubgoal);
        imgPlant = findViewById(R.id.imgPlant);
        progressTasks = findViewById(R.id.progressTasks);
        btnAddGoal = findViewById(R.id.btnAddGoal);
        btnCollection = findViewById(R.id.btnCollection);

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

        cbSubgoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (goalList.isEmpty()) return;
            Goal g = goalList.get(currentGoalIndex);
            if (isChecked) {
                g.completed[g.currentSubgoalIndex] = true;

                int progress = g.getProgress();
                if (progress >= 100) g.plantStage = 3;
                else if (progress >= 66) g.plantStage = 2;
                else if (progress >= 33) g.plantStage = 1;
                else g.plantStage = 0;

                if (g.isAllDone()) showCompletionAnimation(g);

                Toast.makeText(this, "Subgoal finished ✅", Toast.LENGTH_SHORT).show();

                if (g.currentSubgoalIndex < g.subgoals.length - 1) g.currentSubgoalIndex++;
                else Toast.makeText(this, "🎉 All subgoals are done!", Toast.LENGTH_SHORT).show();

                showCurrentGoal();
            }
        });

        imgPlant.setOnClickListener(v -> startActivity(new Intent(MainPageActivity.this, PlantDetailActivity.class)));
        btnAddGoal.setOnClickListener(v -> startActivity(new Intent(MainPageActivity.this, CreateGoalActivity.class)));
        btnCollection.setOnClickListener(v -> startActivity(new Intent(MainPageActivity.this, CollectionActivity.class)));
    }



    // ---------------- API 调用 ----------------
//    private void fetchGoalsFromApi() {
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("http://10.0.2.2:8080/") // 模拟器访问本地服务
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        ApiService apiService = retrofit.create(ApiService.class);
//        Call<List<ApiGoal>> call = apiService.getUserGoals(1); // userId = 1
//        call.enqueue(new Callback<List<ApiGoal>>() {
//            @Override
//            public void onResponse(Call<List<ApiGoal>> call, Response<List<ApiGoal>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    updateGoalList(response.body());
//                } else {
//                    Toast.makeText(MainPageActivity.this, "API returned empty or error", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//
//            @Override
//            public void onFailure(Call<List<ApiGoal>> call, Throwable t) {
//                Toast.makeText(MainPageActivity.this, "Failed to fetch goals: " + t.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
//    }

    private void updateGoalList(List<ApiGoal> apiGoals) {
        goalList.clear();
        for (ApiGoal apiGoal : apiGoals) {
            String[] subTitles = new String[apiGoal.subgoalsList.size()];
            for (int i = 0; i < apiGoal.subgoalsList.size(); i++) {
                subTitles[i] = apiGoal.subgoalsList.get(i).title;
            }
            goalList.add(new Goal(apiGoal.title, subTitles));
        }
        currentGoalIndex = 0;
        showCurrentGoal();
    }

    // ---------------- 显示与动画 ----------------
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
        progressTasks.setProgress(g.getProgress());

        switch (g.plantStage) {
            case 0: imgPlant.setImageResource(R.drawable.ic_plant_stage1); break;
            case 1: imgPlant.setImageResource(R.drawable.ic_plant_stage2); break;
            case 2: imgPlant.setImageResource(R.drawable.ic_plant_stage3); break;
            default: imgPlant.setImageResource(R.drawable.ic_plant_stage4); break;
        }
    }

    private void showCompletionAnimation(Goal g) {
        ImageView confetti = findViewById(R.id.imgCelebrate);
        confetti.setVisibility(View.VISIBLE);
        confetti.setScaleX(0f); confetti.setScaleY(0f); confetti.setAlpha(0f);

        confetti.animate()
                .scaleX(1.5f).scaleY(1.5f).alpha(1f).setDuration(600)
                .withEndAction(() -> confetti.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                    confetti.setVisibility(View.GONE);
                    animatePlantToCollection(g);
                }).start())
                .start();
    }

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
                    Toast.makeText(this, g.title + " Added to collect 🌿", Toast.LENGTH_SHORT).show();
                    goalList.remove(g);
                    if (goalList.isEmpty()) {
                        tvBigGoal.setText("No more tasks!");
                        tvSubgoal.setText("");
                        cbSubgoal.setEnabled(false);
                    } else {
                        if (currentGoalIndex >= goalList.size()) currentGoalIndex = 0;
                        showCurrentGoal();
                    }
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

    // ---------------- Retrofit API ----------------
    public interface ApiService {
//        @retrofit2.http.GET("api/users/{userId}/goals")
//        Call<List<ApiGoal>> getUserGoals(@retrofit2.http.Path("userId") int userId);
    }

    public static class ApiGoal {
        String title;
        List<ApiSubgoal> subgoalsList;
    }

    public static class ApiSubgoal {
        String title;
    }
}
