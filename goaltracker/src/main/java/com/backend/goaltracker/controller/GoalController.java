package com.backend.goaltracker.controller;

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
            System.out.println("Full response tree: " + root.toPrettyString());

            // ✅ Extract text from Gemini API response
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

            // ✅ Clean the response (remove markdown code blocks if present)
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

            // ✅ Parse the structured JSON into DTO
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
     * Convert validated DTO to Goal entity
     * This method now receives a validated DTO with guaranteed fields
     */
    private Goal convertToGoalEntity(GoalResponseDTO dto) {
        // Parse deadline (already validated and in ISO format from validator)
        LocalDate deadline;
        try {
            deadline = LocalDate.parse(dto.deadline);
        } catch (Exception e) {
            // Fallback (should rarely happen due to validator)
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