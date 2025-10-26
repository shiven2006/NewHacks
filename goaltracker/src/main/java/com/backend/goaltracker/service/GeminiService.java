package com.backend.goaltracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
            // Enhanced prompt for structured JSON output
            String enhancedPrompt = "You are a goal planning assistant. Generate a structured goal with subgoals in JSON format.\n\n"
                    + "User request: " + userPrompt + "\n\n"
                    + "Return ONLY valid JSON in this exact format (no markdown, no extra text):\n"
                    + "{\n"
                    + "  \"id\": \"1\",\n"
                    + "  \"title\": \"Main goal title\",\n"
                    + "  \"description\": \"Detailed description\",\n"
                    + "  \"deadline\": \"31/12/2024\",\n"
                    + "  \"subgoals\": [\n"
                    + "    {\n"
                    + "      \"title\": \"Subgoal 1\",\n"
                    + "      \"description\": \"Details\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}";

            // ✅ Correct Gemini API request format
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", enhancedPrompt)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "maxOutputTokens", 2000,
                            "topP", 0.8,
                            "topK", 40
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // ✅ Append API key to URL
            String urlWithKey = apiUrl + "?key=" + apiKey;

            System.out.println("=== Gemini API Request ===");
            System.out.println("URL: " + apiUrl);
            System.out.println("Prompt: " + userPrompt);
            System.out.println("Request body: " + requestBody);

            // ✅ Send to Gemini API endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(urlWithKey, entity, String.class);

            System.out.println("=== Gemini API Response ===");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            // ✅ Check if response is successful
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
}