package com.backend.goaltracker.controller;

import com.backend.goaltracker.repository.GoalFirestoreRepository;
import com.backend.goaltracker.service.GeminiService;
import com.backend.goaltracker.util.PromptValidator;
import com.backend.goaltracker.util.GoalResponseValidator;
import entities.Goal;
import entities.GoalResponseDTO;
import entities.Subgoal;
import entities.SubgoalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin("*")
public class GoalController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private PromptValidator promptValidator;

    @Autowired
    private GoalResponseValidator responseValidator;

    @Autowired
    private GoalFirestoreRepository goalRepository;  // ✅ Inject Firebase repository

    @PostMapping("/generate")
    public ResponseEntity<?> generateGoal(@RequestBody Map<String, String> request) {
        String userPrompt = request.get("prompt");

        if (userPrompt == null || userPrompt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Prompt is required"));
        }

        try {
            System.out.println("=== Goal Generation Started ===");
            System.out.println("Original prompt: " + userPrompt);

            // ✅ STEP 1: Sanitize and validate the input prompt
            String sanitizedPrompt;
            try {
                sanitizedPrompt = promptValidator.sanitizePrompt(userPrompt);
                System.out.println("Sanitized prompt: " + sanitizedPrompt);
            } catch (IllegalArgumentException e) {
                System.err.println("Prompt validation failed: " + e.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }

            // ✅ STEP 2: Call Gemini API with sanitized prompt
            String jsonResponse = geminiService.generateGoal(sanitizedPrompt);

            // Parse Gemini API response JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            System.out.println("=== Parsing Gemini Response ===");

            // Extract text from Gemini API response
            JsonNode candidatesNode = root.path("candidates");
            if (candidatesNode.isMissingNode() || candidatesNode.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "No candidates in Gemini response"));
            }

            JsonNode contentNode = candidatesNode.get(0).path("content");
            if (contentNode.isMissingNode()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "No content in Gemini response"));
            }

            JsonNode partsNode = contentNode.path("parts");
            if (partsNode.isMissingNode() || partsNode.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "No parts in Gemini response"));
            }

            String textOutput = partsNode.get(0).path("text").asText();

            if (textOutput == null || textOutput.isEmpty()) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Empty text in Gemini response"));
            }

            System.out.println("Extracted text: " + textOutput);

            // Clean the response
            String cleanedJson = textOutput.trim();
            if (cleanedJson.startsWith("```json")) {
                cleanedJson = cleanedJson.substring(7);
            }
            if (cleanedJson.startsWith("```")) {
                cleanedJson = cleanedJson.substring(3);
            }
            if (cleanedJson.endsWith("```")) {
                cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
            }
            cleanedJson = cleanedJson.trim();

            System.out.println("Cleaned JSON: " + cleanedJson);

            // Parse the structured JSON into DTO
            GoalResponseDTO dto = mapper.readValue(cleanedJson, GoalResponseDTO.class);
            System.out.println("Parsed DTO - Title: " + dto.title + ", Subgoals: " + dto.subgoals.size());

            // ✅ STEP 3: Validate and fix the response DTO
            try {
                dto = responseValidator.validateAndFix(dto);
                System.out.println("Response validated and fixed");
            } catch (IllegalArgumentException e) {
                System.err.println("Response validation failed: " + e.getMessage());
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Invalid response from AI: " + e.getMessage()));
            }

            // ✅ STEP 4: Convert DTO into Goal entity
            Goal goal = convertToGoalEntity(dto);

            // ✅ STEP 5: Save to Firebase
            try {
                goal = goalRepository.saveGoal(goal);
                System.out.println("Goal saved to Firebase successfully");
            } catch (Exception e) {
                System.err.println("Failed to save to Firebase: " + e.getMessage());
                // Still return the goal even if Firebase save fails
                return ResponseEntity.ok()
                        .header("X-Firebase-Warning", "Goal generated but not saved to Firebase")
                        .body(goal);
            }

            System.out.println("=== Goal Generation Successful ===");
            return ResponseEntity.ok(goal);

        } catch (HttpClientErrorException e) {
            System.err.println("=== Gemini API HTTP Error ===");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Response: " + e.getResponseBodyAsString());

            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "error", "Gemini API Error",
                            "status", e.getStatusCode().toString(),
                            "details", e.getResponseBodyAsString()
                    ));
        } catch (Exception e) {
            System.err.println("=== Unexpected Error ===");
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage(),
                            "type", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * whenever user clicks check box in the client side, the subgoal gets marked as complete.
     */

    @PatchMapping("/{goalId}/subgoals/complete")
    public ResponseEntity<?> markSubgoalComplete(
            @PathVariable int goalId,
            @RequestBody Map<String, Object> request
    ) {
        String title = (String) request.get("title");
        boolean completed = (Boolean) request.getOrDefault("completed", true);

        if (title == null || title.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Subgoal title is required"));
        }

        try {
            boolean updated = goalRepository.markSubgoalComplete(goalId, title, completed);
            if (!updated) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Subgoal not found"));
            }

            Subgoal updatedSubgoal = goalRepository.getSubgoal(goalId, title);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subgoal", updatedSubgoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to update subgoal: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }


    /**
     * Get a goal by ID from Firebase
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getGoal(@PathVariable int id) {
        try {
            Goal goal = goalRepository.getGoalById(id);

            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(goal);

        } catch (Exception e) {
            System.err.println("Error fetching goal: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch goal", "message", e.getMessage()));
        }
    }

    /**
     * Delete a goal by ID from Firebase
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable int id) {
        try {
            goalRepository.deleteGoal(id);
            return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));

        } catch (Exception e) {
            System.err.println("Error deleting goal: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete goal", "message", e.getMessage()));
        }
    }

    /**
     * Save the main goal with the corresponding sub goals
     */
    @PostMapping("/{id}")
    public ResponseEntity<Boolean> saveChanges(@RequestBody Goal goal) {
        try {
            goalRepository.saveGoal(goal); // saveGoal throws exception if it fails
            return ResponseEntity.ok(true); // success
        } catch (Exception e) {
            System.err.println("Failed to save goal: " + e.getMessage());
            return ResponseEntity.ok(false); // failure
        }
    }



    private Goal convertToGoalEntity(GoalResponseDTO dto) {
        LocalDate deadline;
        try {
            deadline = LocalDate.parse(dto.deadline);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                deadline = LocalDate.parse(dto.deadline, formatter);
            } catch (Exception e2) {
                deadline = LocalDate.now().plusMonths(1);
            }
        }

        Goal goal = new Goal();
        goal.setId(Integer.parseInt(dto.id));
        goal.setTitle(dto.title);
        goal.setDescription(dto.description);
        goal.setDeadline(deadline);

        for (SubgoalDTO sgDto : dto.subgoals) {
            Subgoal subgoal = new Subgoal();
            subgoal.setGoalId(goal.getId());
            subgoal.setTitle(sgDto.title);
            subgoal.setDescription(sgDto.description);
            goal.addSubgoal(subgoal);
        }

        return goal;
    }
}