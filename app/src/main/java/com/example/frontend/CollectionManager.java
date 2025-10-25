package com.example.frontend;

import java.util.ArrayList;
import java.util.List;

public class CollectionManager {
    private static CollectionManager instance;
    private final List<Integer> collectedPlants;

    private CollectionManager() {
        collectedPlants = new ArrayList<>();
    }

    public static CollectionManager getInstance() {
        if (instance == null) {
            instance = new CollectionManager();
        }
        return instance;
    }

    public void addPlant(int drawableRes) {
        collectedPlants.add(drawableRes);
    }

    public List<Integer> getPlants() {
        return collectedPlants;
    }
}
