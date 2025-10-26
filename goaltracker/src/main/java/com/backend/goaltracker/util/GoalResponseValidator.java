package com.backend.goaltracker.util;

import entities.GoalResponseDTO;
import entities.SubgoalDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class GoalResponseValidator {

    public GoalResponseDTO validateAndFix(GoalResponseDTO dto) {
        // Ensure ID is set
        if (dto.id == null || dto.id.isEmpty()) {
            dto.id = "1";
        }

        // Ensure title exists
        if (dto.title == null || dto.title.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal title cannot be empty");
        }
        dto.title = dto.title.trim();

        // Ensure description exists
        if (dto.description == null || dto.description.trim().isEmpty()) {
            dto.description = "No description provided";
        }
        dto.description = dto.description.trim();

        // Validate and fix deadline
        dto.deadline = validateAndFixDeadline(dto.deadline);

        // Validate subgoals
        if (dto.subgoals == null || dto.subgoals.isEmpty()) {
            throw new IllegalArgumentException("At least one subgoal is required");
        }

        // Validate each subgoal
        for (GoalResponseDTO.SubgoalDTO subgoal : dto.subgoals) {
            if (subgoal.title == null || subgoal.title.trim().isEmpty()) {
                throw new IllegalArgumentException("Subgoal title cannot be empty");
            }
            subgoal.title = subgoal.title.trim();

            if (subgoal.description == null || subgoal.description.trim().isEmpty()) {
                subgoal.description = "No description provided";
            }
            subgoal.description = subgoal.description.trim();
        }

        return dto;
    }

    private String validateAndFixDeadline(String deadline) {
        if (deadline == null || deadline.isEmpty()) {
            // Default to 30 days from now
            return LocalDate.now().plusDays(30).toString();
        }

        // Try multiple date formats
        String[] dateFormats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy/MM/dd", "dd-MM-yyyy"};

        for (String format : dateFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDate parsedDate = LocalDate.parse(deadline, formatter);
                // Return in ISO format (yyyy-MM-dd)
                return parsedDate.toString();
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        // If all formats fail, default to 30 days from now
        System.out.println("Warning: Could not parse deadline '" + deadline + "', using default");
        return LocalDate.now().plusDays(30).toString();
    }
}