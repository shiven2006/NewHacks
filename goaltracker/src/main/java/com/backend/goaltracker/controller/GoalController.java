package com.backend.goaltracker.controller;

import com.backend.goaltracker.repository.GoalFirestoreRepository;
import com.backend.goaltracker.service.GeminiService;
import com.backend.goaltracker.util.PromptValidator;
import com.backend.goaltracker.util.GoalResponseValidator;
import entities.Goal;
import entities.GoalResponseDTO;
import entities.Subgoal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private GoalFirestoreRepository goalRepository;

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
                System.out.println("Goal saved to Firebase successfully with ID: " + goal.getId());
            } catch (Exception e) {
                System.err.println("Failed to save to Firebase: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to save goal to database", "message", e.getMessage()));
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
     * ✅ NEW: Get all goals from Firebase
     * This allows the frontend to display a list of all goals
     */
    @GetMapping("/")
    public ResponseEntity<?> getAllGoals() {
        try {
            List<Goal> goals = goalRepository.getAllGoals();
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            System.err.println("Error fetching all goals: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch goals", "message", e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get a goal by TITLE (more user-friendly)
     * URL example: /api/goals/by-title/Learn%20Web%20Development
     */
    @GetMapping("/by-title/{title}")
    public ResponseEntity<?> getGoalByTitle(@PathVariable String title) {
        try {
            // Decode URL-encoded title
            String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);

            Goal goal = goalRepository.getGoalByTitle(decodedTitle);

            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(goal);

        } catch (Exception e) {
            System.err.println("Error fetching goal by title: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch goal", "message", e.getMessage()));
        }
    }

    /**
     * ✅ Update an existing goal using TITLE
     */
    @PutMapping("/by-title/{title}")
    public ResponseEntity<?> updateGoalByTitle(
            @PathVariable String title,
            @RequestBody Goal goal
    ) {
        try {
            String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);

            // ✅ Find existing goal by title
            Goal existingGoal = goalRepository.getGoalByTitle(decodedTitle);
            if (existingGoal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Keep the same ID
            goal.setId(existingGoal.getId());

            // ✅ Ensure all subgoals have the correct goalId
            if (goal.getSubgoals() != null) {
                for (Subgoal subgoal : goal.getSubgoals()) {
                    subgoal.setGoalId(existingGoal.getId());
                }
            }

            // ✅ Save to Firebase
            Goal savedGoal = goalRepository.saveGoal(goal);
            System.out.println("Goal '" + title + "' updated in Firebase");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "goal", savedGoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to update goal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Update an existing goal using ID (keep this for compatibility)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGoal(
            @PathVariable int id,
            @RequestBody Goal goal
    ) {
        try {
            // ✅ Validate that the path ID matches the goal ID
            if (goal.getId() == null || goal.getId() != id) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Goal ID in path must match goal ID in body"
                        ));
            }

            // ✅ Check if goal exists
            Goal existingGoal = goalRepository.getGoalById(id);
            if (existingGoal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Ensure all subgoals have the correct goalId
            if (goal.getSubgoals() != null) {
                for (Subgoal subgoal : goal.getSubgoals()) {
                    subgoal.setGoalId(id);
                }
            }

            // ✅ Save to Firebase
            Goal savedGoal = goalRepository.saveGoal(goal);
            System.out.println("Goal " + id + " updated in Firebase");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "goal", savedGoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to update goal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Delete a goal by ID from Firebase
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable int id) {
        try {
            // ✅ First check if goal exists
            Goal existingGoal = goalRepository.getGoalById(id);
            if (existingGoal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Delete from Firebase
            goalRepository.deleteGoal(id);
            System.out.println("Goal " + id + " deleted from Firebase");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Goal deleted successfully"
            ));

        } catch (Exception e) {
            System.err.println("Error deleting goal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to delete goal",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ NEW: Delete a goal by TITLE
     */
    @DeleteMapping("/by-title/{title}")
    public ResponseEntity<?> deleteGoalByTitle(@PathVariable String title) {
        try {
            String decodedTitle = URLDecoder.decode(title, StandardCharsets.UTF_8);

            Goal existingGoal = goalRepository.getGoalByTitle(decodedTitle);
            if (existingGoal == null) {
                return ResponseEntity.notFound().build();
            }

            goalRepository.deleteGoal(existingGoal.getId());
            System.out.println("Goal '" + title + "' deleted from Firebase");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Goal deleted successfully"
            ));

        } catch (Exception e) {
            System.err.println("Error deleting goal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to delete goal",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Mark a subgoal as complete using TITLE-based lookup
     */
    @PatchMapping("/by-title/{goalTitle}/subgoals/complete")
    public ResponseEntity<?> markSubgoalCompleteByTitle(
            @PathVariable String goalTitle,
            @RequestBody Map<String, Object> request
    ) {
        String subgoalTitle = (String) request.get("title");
        Boolean completed = (Boolean) request.getOrDefault("completed", true);

        if (subgoalTitle == null || subgoalTitle.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Subgoal title is required"));
        }

        try {
            String decodedGoalTitle = URLDecoder.decode(goalTitle, StandardCharsets.UTF_8);

            // ✅ Get the full goal from Firebase by title
            Goal goal = goalRepository.getGoalByTitle(decodedGoalTitle);

            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Find and update the subgoal
            boolean found = false;
            for (Subgoal subgoal : goal.getSubgoals()) {
                if (subgoal.getTitle().equals(subgoalTitle)) {
                    subgoal.setCompleted(completed);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Subgoal not found"
                ));
            }

            // ✅ Save the entire goal back to Firebase
            goalRepository.saveGoal(goal);

            System.out.println("Subgoal '" + subgoalTitle + "' in goal '" + goalTitle +
                    "' marked as " + (completed ? "complete" : "incomplete"));

            // ✅ Return the updated subgoal
            Subgoal updatedSubgoal = goal.getSubgoals().stream()
                    .filter(sg -> sg.getTitle().equals(subgoalTitle))
                    .findFirst()
                    .orElse(null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subgoal", updatedSubgoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to update subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ Mark a subgoal as complete using ID (keep for compatibility)
     */
    @PatchMapping("/{goalId}/subgoals/complete")
    public ResponseEntity<?> markSubgoalComplete(
            @PathVariable int goalId,
            @RequestBody Map<String, Object> request
    ) {
        String title = (String) request.get("title");
        Boolean completed = (Boolean) request.getOrDefault("completed", true);

        if (title == null || title.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Subgoal title is required"));
        }

        try {
            // ✅ Get the full goal from Firebase
            Goal goal = goalRepository.getGoalById(goalId);

            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Find and update the subgoal
            boolean found = false;
            for (Subgoal subgoal : goal.getSubgoals()) {
                if (subgoal.getTitle().equals(title)) {
                    subgoal.setCompleted(completed);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Subgoal not found"
                ));
            }

            // ✅ Save the entire goal back to Firebase
            goalRepository.saveGoal(goal);

            System.out.println("Subgoal '" + title + "' marked as " +
                    (completed ? "complete" : "incomplete") +
                    " and saved to Firebase");

            // ✅ Return the updated subgoal
            Subgoal updatedSubgoal = goal.getSubgoals().stream()
                    .filter(sg -> sg.getTitle().equals(title))
                    .findFirst()
                    .orElse(null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subgoal", updatedSubgoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to update subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * ✅ Add a new subgoal to an existing goal (by title)
     */
    @PostMapping("/by-title/{goalTitle}/subgoals")
    public ResponseEntity<?> addSubgoalByTitle(
            @PathVariable String goalTitle,
            @RequestBody Subgoal subgoal
    ) {
        try {
            String decodedTitle = URLDecoder.decode(goalTitle, StandardCharsets.UTF_8);

            // ✅ Get existing goal
            Goal goal = goalRepository.getGoalByTitle(decodedTitle);
            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Set the goalId and add to goal
            subgoal.setGoalId(goal.getId());
            subgoal.setCompleted(false);
            goal.addSubgoal(subgoal);

            // ✅ Save to Firebase
            goalRepository.saveGoal(goal);
            System.out.println("New subgoal added to goal '" + goalTitle + "'");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subgoal", subgoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to add subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Add a new subgoal to an existing goal (by ID - keep for compatibility)
     */
    @PostMapping("/{goalId}/subgoals")
    public ResponseEntity<?> addSubgoal(
            @PathVariable int goalId,
            @RequestBody Subgoal subgoal
    ) {
        try {
            // ✅ Get existing goal
            Goal goal = goalRepository.getGoalById(goalId);
            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Set the goalId and add to goal
            subgoal.setGoalId(goalId);
            subgoal.setCompleted(false);
            goal.addSubgoal(subgoal);

            // ✅ Save to Firebase
            goalRepository.saveGoal(goal);
            System.out.println("New subgoal added to goal " + goalId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "subgoal", subgoal
            ));

        } catch (Exception e) {
            System.err.println("Failed to add subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Delete a subgoal from a goal (by goal title)
     */
    @DeleteMapping("/by-title/{goalTitle}/subgoals/{subgoalTitle}")
    public ResponseEntity<?> deleteSubgoalByTitle(
            @PathVariable String goalTitle,
            @PathVariable String subgoalTitle
    ) {
        try {
            String decodedGoalTitle = URLDecoder.decode(goalTitle, StandardCharsets.UTF_8);
            String decodedSubgoalTitle = URLDecoder.decode(subgoalTitle, StandardCharsets.UTF_8);

            // ✅ Get existing goal
            Goal goal = goalRepository.getGoalByTitle(decodedGoalTitle);
            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Find and remove subgoal
            boolean removed = goal.getSubgoals().removeIf(
                    sg -> sg.getTitle().equals(decodedSubgoalTitle)
            );

            if (!removed) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Subgoal not found"
                ));
            }

            // ✅ Save to Firebase
            goalRepository.saveGoal(goal);
            System.out.println("Subgoal '" + subgoalTitle + "' deleted from goal '" + goalTitle + "'");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subgoal deleted successfully"
            ));

        } catch (Exception e) {
            System.err.println("Failed to delete subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ Delete a subgoal from a goal (by ID - keep for compatibility)
     */
    @DeleteMapping("/{goalId}/subgoals/{subgoalTitle}")
    public ResponseEntity<?> deleteSubgoal(
            @PathVariable int goalId,
            @PathVariable String subgoalTitle
    ) {
        try {
            String decodedTitle = URLDecoder.decode(subgoalTitle, StandardCharsets.UTF_8);

            // ✅ Get existing goal
            Goal goal = goalRepository.getGoalById(goalId);
            if (goal == null) {
                return ResponseEntity.notFound().build();
            }

            // ✅ Find and remove subgoal
            boolean removed = goal.getSubgoals().removeIf(
                    sg -> sg.getTitle().equals(decodedTitle)
            );

            if (!removed) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Subgoal not found"
                ));
            }

            // ✅ Save to Firebase
            goalRepository.saveGoal(goal);
            System.out.println("Subgoal '" + subgoalTitle + "' deleted from goal " + goalId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Subgoal deleted successfully"
            ));

        } catch (Exception e) {
            System.err.println("Failed to delete subgoal: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * ✅ FIXED: Convert DTO to Goal entity
     * DO NOT set ID here - let the repository generate it
     */
    private Goal convertToGoalEntity(GoalResponseDTO dto) {
        Goal goal = new Goal();

        // ✅ DON'T set ID - let repository handle it
        // The ID from Gemini ("1") is just a placeholder

        goal.setTitle(dto.title);
        goal.setDescription(dto.description);

        // Parse deadline with fallback
        LocalDate deadline;
        try {
            deadline = LocalDate.parse(dto.deadline);
        } catch (Exception e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                deadline = LocalDate.parse(dto.deadline, formatter);
            } catch (Exception e2) {
                System.err.println("Failed to parse deadline: " + dto.deadline);
                deadline = LocalDate.now().plusMonths(2);
            }
        }
        goal.setDeadline(deadline);

        // Add subgoals (without goalId yet - repository will set it)
        if (dto.subgoals != null) {
            for (GoalResponseDTO.SubgoalDTO sgDto : dto.subgoals) {
                Subgoal subgoal = new Subgoal();
                subgoal.setTitle(sgDto.title);
                subgoal.setDescription(sgDto.description);
                subgoal.setCompleted(false);
                goal.addSubgoal(subgoal);
            }
        }

        return goal;
    }
}