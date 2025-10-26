package com.example.frontend;

public class SubgoalModel {
    String name;
    String description;

    public SubgoalModel(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
