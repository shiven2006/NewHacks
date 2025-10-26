package com.backend.goaltracker.util;

import org.springframework.stereotype.Component;

@Component
public class PromptValidator {

    private static final int MIN_PROMPT_LENGTH = 5;
    private static final int MAX_PROMPT_LENGTH = 500;

    public String sanitizePrompt(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        // Remove extra whitespace and normalize
        prompt = prompt.trim().replaceAll("\\s+", " ");

        // Remove potentially problematic characters
        prompt = prompt.replaceAll("[\\r\\n\\t]", " ");

        // Remove special characters that might break JSON
        prompt = prompt.replaceAll("[\"\\\\]", "");

        // Validate length
        if (prompt.length() < MIN_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Prompt too short (minimum " + MIN_PROMPT_LENGTH + " characters)");
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            prompt = prompt.substring(0, MAX_PROMPT_LENGTH);
        }

        return prompt;
    }
}