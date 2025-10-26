package com.backend.goaltracker.util;

import org.springframework.stereotype.Component;

@Component
public class PromptValidator {

    private static final int MIN_PROMPT_LENGTH = 3;  // More permissive
    private static final int MAX_PROMPT_LENGTH = 500;

    public String sanitizePrompt(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        // Store original for comparison
        String original = prompt;

        // Remove extra whitespace and normalize
        prompt = prompt.trim().replaceAll("\\s+", " ");

        // Remove line breaks and tabs
        prompt = prompt.replaceAll("[\\r\\n\\t]", " ");

        // Only remove problematic characters for JSON, keep most special chars
        // Remove only unescaped quotes and backslashes that could break JSON
        prompt = prompt.replaceAll("\\\\(?![\"\\\\])", ""); // Remove stray backslashes

        // Validate length
        if (prompt.length() < MIN_PROMPT_LENGTH) {
            throw new IllegalArgumentException("Prompt too short (minimum " + MIN_PROMPT_LENGTH + " characters)");
        }

        if (prompt.length() > MAX_PROMPT_LENGTH) {
            System.out.println("Warning: Prompt truncated from " + prompt.length() + " to " + MAX_PROMPT_LENGTH + " characters");
            prompt = prompt.substring(0, MAX_PROMPT_LENGTH);
        }

        // Log if significant changes were made
        if (!original.equals(prompt)) {
            System.out.println("Prompt was sanitized");
        }

        return prompt;
    }

    /**
     * Validate without throwing exceptions (for checking only)
     */
    public boolean isValid(String prompt) {
        try {
            sanitizePrompt(prompt);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}