package com.backend.goaltracker.controller;

import com.backend.goaltracker.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:3000")
public class GoalController {

    @Autowired
    private GeminiService geminiService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateGoal(@RequestBody Map<String, String> request) {
        String userPrompt = request.get("prompt");

        System.out.println("Received prompt: " + userPrompt);  // ← Add this

        if (userPrompt == null || userPrompt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Prompt is required"));
        }

        try {
            System.out.println("Calling Gemini service...");  // ← Add this
            String aiResponse = geminiService.generateGoal(userPrompt);
            System.out.println("Got response: " + aiResponse);  // ← Add this

            return ResponseEntity.ok(
                    Map.of(
                            "response", aiResponse,
                            "prompt", userPrompt
                    )
            );
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());  // ← Add this
            e.printStackTrace();  // ← Add this to see full error

            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}