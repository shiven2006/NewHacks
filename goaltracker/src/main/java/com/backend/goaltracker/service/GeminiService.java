package com.backend.goaltracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Generates a structured SMART goal with subgoals using the Gemini API.
     * @param userPrompt The user's raw goal description.
     * @return A formatted JSON string representing the goal and subgoals.
     */
    public String generateGoal(String userPrompt) {
        System.out.println("=== GEMINI API CALL DEBUG ===");
        System.out.println("API Key present: " + (apiKey != null && !apiKey.isEmpty()));
        System.out.println("API URL: " + apiUrl);
        System.out.println("User Prompt: " + userPrompt);

        // ✅ Improved system prompt — ensures structured JSON output
        String improvedPrompt = """
You are an expert AI goal-setting assistant that creates structured, actionable goals.

Your task:
Given a user’s goal description, return a JSON object representing a "Goal" with realistic subgoals.

Follow these strict instructions:

1. Convert the goal description into a SMART goal (Specific, Measurable, Achievable, Relevant, Time-bound).
2. Assign a reasonable **deadline** for the main goal in timestamp format (dd/mm/yyyy).
3. Create 3–7 **subgoals** that break down the main goal into clear, trackable steps.
4. Each subgoal must be part of a checked list (✓ before each title for clarity) and include:
   - The same `id` as the main goal
   - A short `title` summarizing the step
   - A short, actionable `description`
5. Return **only** the final structured JSON (no explanations, no markdown, no extra text).

The JSON structure must exactly follow this format:

{
  "id": "<GOAL_ID>",
  "title": "<Main goal title>",
  "description": "<SMART goal description>",
  "deadline": "<timestamp(dd/mm/yyyy)>",
  "subgoals": [
    {
      "id": "<GOAL_ID>",
      "title": "<Subgoal title>",
      "description": "<Short actionable description>"
    },...
  ]
}

Goal description: "%s"
""".formatted(userPrompt);


        // ✅ Use improvedPrompt instead of raw userPrompt
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", improvedPrompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            System.out.println("Calling Gemini API...");

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();

            if (body == null) throw new RuntimeException("Empty response from Gemini");

            String text = extractTextFromResponse(body);
            System.out.println("=== Gemini Response ===");
            System.out.println(text);

            return text;

        } catch (HttpClientErrorException e) {
            System.err.println("Gemini API Error: " + e.getStatusCode());
            System.err.println("Error details: " + e.getResponseBodyAsString());
            throw new RuntimeException("Gemini API error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage());
        }
    }

    /**
     * Safely extracts the generated text content from the Gemini response.
     */
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) return "No candidates found";

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return "No content found";

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return "No parts found";

            String text = (String) parts.getFirst().get("text");
            return text != null ? text.trim() : "No text found";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }
}
