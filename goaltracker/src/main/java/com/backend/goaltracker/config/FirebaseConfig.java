package com.backend.goaltracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // ✅ Check if Firebase is already initialized (prevents test errors)
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("goaltrack-95875-firebase-adminsdk-fbsvc-ccef1fb91e.json");

                // ✅ Only initialize if the file exists (skip in tests)
                if (resource.exists()) {
                    GoogleCredentials credentials = GoogleCredentials
                            .fromStream(resource.getInputStream());

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase initialized successfully");
                } else {
                    System.out.println("⚠️ Firebase config not found - skipping initialization (test mode?)");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to initialize Firebase: " + e.getMessage());
            // Don't throw exception - allow app to start without Firebase
        }
    }
}