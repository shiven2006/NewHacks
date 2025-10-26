package entities;

import java.util.List;

public class GoalResponseDTO {
    public String id;
    public String title;
    public String description;
    public String deadline;   // keep as string for now
    public List<SubgoalDTO> subgoals;
}