package com.example.frontend;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import android.media.MediaPlayer;


public class MainPageActivity extends AppCompatActivity {


    private TextView tvBigGoal, tvSubgoal;
    private CheckBox cbSubgoal;
    private ImageView btnPrevGoal, btnNextGoal, imgPlant;
    private ProgressBar progressTasks;
    private ImageButton btnAddGoal, btnCollection;


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


        initGoals(); // TODO: API replace
        showCurrentGoal();


        btnPrevGoal.setOnClickListener(v -> {
            if (currentGoalIndex > 0) currentGoalIndex--;
            else currentGoalIndex = goalList.size() - 1;
            showCurrentGoal();
        });


        btnNextGoal.setOnClickListener(v -> {
            if (currentGoalIndex < goalList.size() - 1) currentGoalIndex++;
            else currentGoalIndex = 0;
            showCurrentGoal();
        });


        cbSubgoal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Goal g = goalList.get(currentGoalIndex);
            if (isChecked) {
                g.completed[g.currentSubgoalIndex] = true;


                // 更新植物成长阶段
                if (g.getProgress() >= 33 && g.plantStage == 0) g.plantStage = 1;
                else if (g.getProgress() >= 66 && g.plantStage == 1) g.plantStage = 2;
                else if (g.isAllDone()) {
                    g.plantStage = 3;
                    showCompletionAnimation(g);
                }


                Toast.makeText(this, "Subgoal finished ✅", Toast.LENGTH_SHORT).show();


                if (g.currentSubgoalIndex < g.subgoals.length - 1) g.currentSubgoalIndex++;
                else Toast.makeText(this, "🎉 All subgoals are done！", Toast.LENGTH_SHORT).show();


                showCurrentGoal();
            }
        });


        imgPlant.setOnClickListener(v -> {
            // 播放点击音效
            MediaPlayer mp = MediaPlayer.create(MainPageActivity.this, R.raw.pop);
            mp.start();

            // 播放完释放资源
            mp.setOnCompletionListener(MediaPlayer::release);

            // 再执行跳转
            Intent intent = new Intent(MainPageActivity.this, PlantDetailActivity.class);
            startActivity(intent);
        });
        btnAddGoal.setOnClickListener(v -> startActivity(new Intent(MainPageActivity.this, CreateGoalActivity.class)));
        btnCollection.setOnClickListener(v -> startActivity(new Intent(MainPageActivity.this, CollectionActivity.class)));
    }


    private void initGoals() {
        // TODO: API
        goalList.add(new Goal("BigGoal1", new String[]{"SubGoal11","SubGoal12","SubGoal13"}));
        goalList.add(new Goal("BigGoal2", new String[]{"SubGoal21","SubGoal22","SubGoal23"}));
        goalList.add(new Goal("BigGoal3", new String[]{"SubGoal31","SubGoal32","SubGoal33"}));
    }


    private void showCurrentGoal() {
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

        MediaPlayer mp = MediaPlayer.create(MainPageActivity.this, R.raw.celebrate);
        mp.start();
        mp.setOnCompletionListener(MediaPlayer::release);

        confetti.animate()
                .scaleX(1.5f).scaleY(1.5f).alpha(1f).setDuration(600)
                .withEndAction(() -> {
                    confetti.animate().alpha(0f).setDuration(800).withEndAction(() -> {
                        confetti.setVisibility(View.GONE);
                        animatePlantToCollection(g);
                    }).start();
                }).start();
    }


    private void animatePlantToCollection(Goal g) {
        // 每次完成 Goal 都加入 CollectionManager
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
                    Toast.makeText(this, g.title + " Added to collect🌿", Toast.LENGTH_SHORT).show();
                    goalList.remove(g);


                    if (goalList.isEmpty()) {
                        tvBigGoal.setText("No more tasks！");
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
}
