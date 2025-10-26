package com.backend.goaltracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateGoal(String userPrompt) {
        if (userPrompt == null || userPrompt.isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        try {
            // ✅ Enhanced prompt with stricter formatting requirements
            String enhancedPrompt = buildEnhancedPrompt(userPrompt);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", enhancedPrompt)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,  // Lower temperature for more consistent output
                            "maxOutputTokens", 2000,
                            "topP", 0.8,
                            "topK", 40
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String urlWithKey = apiUrl + "?key=" + apiKey;

            System.out.println("=== Gemini API Request ===");
            System.out.println("URL: " + apiUrl);
            System.out.println("Prompt length: " + userPrompt.length());

            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, entity, String.class);

            System.out.println("=== Gemini API Response ===");
            System.out.println("Status: " + response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Gemini API returned status: " + response.getStatusCode());
            }

            if (response.getBody() == null || response.getBody().isEmpty()) {
                throw new RuntimeException("Gemini API returned empty response");
            }

            return response.getBody();

        } catch (Exception e) {
            System.err.println("=== Gemini API Error ===");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gemini API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build an enhanced prompt with strict formatting requirements
     */
    private String buildEnhancedPrompt(String userPrompt) {
        LocalDate suggestedDeadline = LocalDate.now().plusMonths(2);

        return "You are a goal planning assistant. Generate a structured goal with subgoals.\n\n"
                + "User's goal: " + userPrompt + "\n\n"
                + "CRITICAL REQUIREMENTS:\n"
                + "1. Return ONLY valid JSON - no markdown code blocks, no explanations, no extra text\n"
                + "2. Do NOT wrap in ```json or ``` tags\n"
                + "3. Use ISO date format (yyyy-MM-dd) for deadline\n"
                + "4. Include 3-5 specific, actionable subgoals\n"
                + "5. Make sure all fields are filled with meaningful content\n"
                + "6. Set a realistic deadline (suggested: " + suggestedDeadline + ")\n\n"
                + "OUTPUT THIS EXACT JSON STRUCTURE:\n"
                + "{\n"
                + "  \"id\": \"1\",\n"
                + "  \"title\": \"[Clear, concise main goal title]\",\n"
                + "  \"description\": \"[Detailed explanation of what will be achieved and why]\",\n"
                + "  \"deadline\": \"" + suggestedDeadline + "\",\n"
                + "  \"subgoals\": [\n"
                + "    {\n"
                + "      \"title\": \"[Specific subgoal 1 title]\",\n"
                + "      \"description\": \"[Concrete steps to achieve this subgoal]\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"title\": \"[Specific subgoal 2 title]\",\n"
                + "      \"description\": \"[Concrete steps to achieve this subgoal]\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"title\": \"[Specific subgoal 3 title]\",\n"
                + "      \"description\": \"[Concrete steps to achieve this subgoal]\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "IMPORTANT: Return ONLY the JSON object. No other text.";
    }
}