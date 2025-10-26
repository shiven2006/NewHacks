package entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for parsing Gemini API response
 */
public class GoalResponseDTO {

    @JsonProperty("id")
    public String id;

    @JsonProperty("title")
    public String title;

    @JsonProperty("description")
    public String description;

    @JsonProperty("deadline")
    public String deadline;

    @JsonProperty("subgoals")
    public List<SubgoalDTO> subgoals;

    public static class SubgoalDTO {
        @JsonProperty("title")
        public String title;

        @JsonProperty("description")
        public String description;

        public SubgoalDTO() {}

        public SubgoalDTO(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public GoalResponseDTO() {}

    public GoalResponseDTO(String id, String title, String description,
                           String deadline, List<SubgoalDTO> subgoals) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.subgoals = subgoals;
    }

    @Override
    public String toString() {
        return "GoalResponseDTO{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", deadline='" + deadline + '\'' +
                ", subgoals=" + (subgoals != null ? subgoals.size() : 0) +
                '}';
    }
}