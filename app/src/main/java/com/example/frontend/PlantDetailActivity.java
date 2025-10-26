package com.example.frontend;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlantDetailActivity extends AppCompatActivity {

    private TextView tvGoalTitle, tvGoalDescription;
    private LinearLayout subgoalContainer;
    private ImageButton btnBack;
    private OkHttpClient client;

    private static final String BASE_URL = "http://10.0.2.2:8080/api/goals";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        client = new OkHttpClient();

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        tvGoalTitle = findViewById(R.id.tvGoalTitle);
        tvGoalDescription = findViewById(R.id.tvGoalDescription);
        subgoalContainer = findViewById(R.id.subgoalContainer);

        btnBack.setOnClickListener(v -> finish());

        // Get goal title passed from MainPageActivity
        String goalTitle = getIntent().getStringExtra("GOAL_TITLE");
        if (goalTitle != null && !goalTitle.isEmpty()) {
            fetchGoalByTitle(goalTitle);
        } else {
            Toast.makeText(this, "Goal title missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Fetch goal data by title from backend
     */
    private void fetchGoalByTitle(String title) {
        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
            String url = BASE_URL + "/by-title/" + encodedTitle;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(
                                PlantDetailActivity.this,
                                "Failed to load goal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            Toast.makeText(
                                    PlantDetailActivity.this,
                                    "Failed to fetch goal from server",
                                    Toast.LENGTH_SHORT).show();
                        });
                        response.close();
                        return;
                    }

                    String jsonData = response.body().string();
                    response.close();

                    try {
                        JSONObject goalJson = new JSONObject(jsonData);

                        String goalTitle = goalJson.getString("title");
                        String goalDescription = goalJson.optString("description", "");

                        JSONArray subgoalsJson = goalJson.getJSONArray("subgoals");

                        runOnUiThread(() -> {
                            tvGoalTitle.setText(goalTitle);
                            tvGoalDescription.setText(goalDescription);

                            subgoalContainer.removeAllViews(); // clear any old views

                            try {
                                for (int i = 0; i < subgoalsJson.length(); i++) {
                                    JSONObject sub = subgoalsJson.getJSONObject(i);
                                    String subTitle = sub.getString("title");
                                    String subDescription = sub.optString("description", "");
                                    boolean completed = sub.optBoolean("completed", false);

                                    addSubgoalView(subTitle, subDescription, completed);
                                }
                            } catch (Exception e) {
                                Toast.makeText(
                                        PlantDetailActivity.this,
                                        "Failed to parse subgoals: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> Toast.makeText(
                                PlantDetailActivity.this,
                                "Failed to parse goal JSON: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                        );
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error encoding title: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Dynamically create subgoal views
     */
    private void addSubgoalView(String title, String description, boolean completed) {
        LinearLayout subgoalLayout = new LinearLayout(this);
        subgoalLayout.setOrientation(LinearLayout.VERTICAL);
        subgoalLayout.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 16);
        subgoalLayout.setLayoutParams(layoutParams);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.black));

        // Description
        TextView tvDescription = new TextView(this);
        tvDescription.setText(description);
        tvDescription.setTextSize(15);
        tvDescription.setTextColor(ContextCompat.getColor(this, R.color.dark_gray));

        // Status
        TextView tvStatus = new TextView(this);
        tvStatus.setText("Status: " + (completed ? "Completed" : "Not Completed"));
        tvStatus.setTextSize(14);
        tvStatus.setTypeface(null, android.graphics.Typeface.ITALIC);
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.black));
        tvStatus.setPadding(0, 6, 0, 0);

        subgoalLayout.addView(tvTitle);
        subgoalLayout.addView(tvDescription);
        subgoalLayout.addView(tvStatus);

        subgoalContainer.addView(subgoalLayout);
    }
}