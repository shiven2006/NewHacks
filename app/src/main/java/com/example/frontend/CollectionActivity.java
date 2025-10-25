package com.example.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class CollectionActivity extends AppCompatActivity {

    private GridLayout gridPlants;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        gridPlants = findViewById(R.id.gridPlants);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        populatePlants();
    }

    private void populatePlants() {
        gridPlants.removeAllViews();
        List<Integer> plantDrawableList = CollectionManager.getInstance().getPlants();

        for (int i = 0; i < plantDrawableList.size(); i++) {
            int drawableRes = plantDrawableList.get(i);
            ImageView plantView = new ImageView(this);
            plantView.setImageResource(drawableRes);
            plantView.setLayoutParams(new GridLayout.LayoutParams());
            plantView.setPadding(16,16,16,16);
            plantView.setBackgroundResource(R.drawable.rounded_box);
            plantView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            int finalI = i;
            plantView.setOnClickListener(v -> openPlantDetail(finalI));
            gridPlants.addView(plantView);
        }
    }

    private void openPlantDetail(int index) {
        Intent intent = new Intent(this, PlantDetailActivity.class);
        intent.putExtra("plantIndex", index);
        startActivity(intent);
    }
}

