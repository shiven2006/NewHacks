package com.backend.goaltracker.controller;


import com.backend.goaltracker.service.FirebaseAuthService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> userData) {

        // Step 1: Validate Authorization header
        if (authHeader == null || authHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No authorization token provided"));
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid authorization header format"));
        }

        try {
            // Step 2: Extract token
            String idToken = authHeader.replace("Bearer ", "");

            // Step 3: Verify token with Firebase
            FirebaseToken decodedToken = firebaseAuthService.verifyToken(idToken);

            // Step 4: Extract user info
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // Step 5: Log registration (later: save to database)
            System.out.println("Registering: " + email + " with UID: " + uid);

            // Step 6: Return success response (200 OK)
            return ResponseEntity.ok(
                    Map.of(
                            "message", "User registered successfully",
                            "uid", uid,
                            "email", email
                    )
            );

        } catch (FirebaseAuthException e) {
            // Step 7: Handle authentication failure
            System.err.println("Token verification failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
        }
    }

}
