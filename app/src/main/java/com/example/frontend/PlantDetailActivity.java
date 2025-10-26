package com.example.frontend;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PlantDetailActivity extends AppCompatActivity {

    private TextView tvGoalTitle, tvGoalDescription;
    private LinearLayout subgoalContainer;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail); // 确保你的 xml 文件名正确

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 获取控件引用
        tvGoalTitle = findViewById(R.id.tvGoalTitle);
        tvGoalDescription = findViewById(R.id.tvGoalDescription);
        subgoalContainer = findViewById(R.id.subgoalContainer);

        // 设置 Goal 信息
        String goalTitle = "Learn Spanish";
        String goalDescription = "Master conversational Spanish in 6 months.";
        tvGoalTitle.setText(goalTitle);
        tvGoalDescription.setText(goalDescription);

        // 示例 Subgoal 数据，可以用 List 或数组来存储
        Subgoal[] subgoals = new Subgoal[] {
                new Subgoal("Learn basic vocabulary", "Start with 500 essential Spanish words.", false),
                new Subgoal("Practice speaking", "Have at least 3 conversations per week.", false),
                new Subgoal("Grammar exercises", "Complete 20 grammar exercises.", true)
        };

        // 动态添加 Subgoal
        for (Subgoal subgoal : subgoals) {
            addSubgoalView(subgoal);
        }
    }

    // 动态创建 Subgoal 布局
    private void addSubgoalView(Subgoal subgoal) {
        // 创建 LinearLayout
        LinearLayout subgoalLayout = new LinearLayout(this);
        subgoalLayout.setOrientation(LinearLayout.VERTICAL);
        subgoalLayout.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        subgoalLayout.setLayoutParams(layoutParams);


        // Subgoal Title
        TextView title = new TextView(this);
        title.setText(subgoal.title);
        title.setTextSize(18);
        title.setTextColor(getResources().getColor(R.color.black));
        title.setPadding(0, 0, 0, 4);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        // Subgoal Description
        TextView description = new TextView(this);
        description.setText(subgoal.description);
        description.setTextSize(15);
        description.setTextColor(Color.DKGRAY);

        // Subgoal Status
        TextView status = new TextView(this);
        status.setText("Status: " + (subgoal.isCompleted ? "Completed" : "Not Completed"));
        status.setTextSize(14);
        status.setTextColor(getResources().getColor(R.color.black));
        status.setTypeface(null, android.graphics.Typeface.ITALIC);
        status.setPadding(0, 6, 0, 0);

        // 添加到布局
        subgoalLayout.addView(title);
        subgoalLayout.addView(description);
        subgoalLayout.addView(status);

        // 添加到父布局
        subgoalContainer.addView(subgoalLayout);
    }

    // Subgoal 数据类
    static class Subgoal {
        String title;
        String description;
        boolean isCompleted;

        Subgoal(String title, String description, boolean isCompleted) {
            this.title = title;
            this.description = description;
            this.isCompleted = isCompleted;
        }
    }
}
