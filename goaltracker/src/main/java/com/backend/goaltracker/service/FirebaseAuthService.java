package com.backend.goaltracker.service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;


@Service
public class FirebaseAuthService {
    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        return decodedToken;
    }


    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = verifyToken(idToken);
        return decodedToken.getUid();
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String idToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = verifyToken(idToken);
        return decodedToken.getEmail();
    }



}