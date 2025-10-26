package com.backend.goaltracker.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            // ✅ Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("goaltrack-95875-firebase-adminsdk-fbsvc-ccef1fb91e.json");

                if (resource.exists()) {
                    GoogleCredentials credentials = GoogleCredentials
                            .fromStream(resource.getInputStream());

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("✅ Firebase initialized successfully");
                } else {
                    System.out.println("⚠️ Firebase config not found - skipping initialization");
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");

            if (resource.exists()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase initialized successfully");
            } else {
                throw new RuntimeException("Firebase serviceAccountKey.json not found");
            }
        }

        return FirestoreClient.getFirestore();
    }

}