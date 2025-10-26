package entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Goal {
    private Integer id;
    private String title;
    private String description;
    private LocalDate deadline;
    private List<Subgoal> subgoals;

    public Goal() {
        this.subgoals = new ArrayList<>();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public List<Subgoal> getSubgoals() {
        return subgoals;
    }

    public void setSubgoals(List<Subgoal> subgoals) {
        this.subgoals = subgoals;
    }

    public void addSubgoal(Subgoal subgoal) {
        if (this.subgoals == null) {
            this.subgoals = new ArrayList<>();
        }
        this.subgoals.add(subgoal);
    }

    public void removeSubgoal(Subgoal subgoal) {
        if (this.subgoals != null) {
            this.subgoals.remove(subgoal);
        }
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", deadline=" + deadline +
                ", subgoals=" + (subgoals != null ? subgoals.size() : 0) +
                '}';
    }
}