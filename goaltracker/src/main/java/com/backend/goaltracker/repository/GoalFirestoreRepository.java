package com.backend.goaltracker.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import entities.Goal;
import entities.Subgoal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Repository
public class GoalFirestoreRepository {

    @Autowired
    private Firestore firestore;

    private static final String GOALS_COLLECTION = "goals";

    /**
     * Save a goal to Firestore
     */
    public Goal saveGoal(Goal goal) {
        try {
            // ✅ Generate unique ID if not set - FIX: Check for null separately
            if (goal.getId() == null) {
                // Use Firestore auto-generated ID
                DocumentReference docRef = firestore.collection(GOALS_COLLECTION).document();
                String firestoreId = docRef.getId();

                // Convert to a stable integer ID (use absolute value to avoid negatives)
                int numericId = Math.abs(firestoreId.hashCode());

                // Ensure it's not zero
                if (numericId == 0) {
                    numericId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                }

                goal.setId(numericId);
                System.out.println("Generated new goal ID: " + numericId);
            } else if (goal.getId() == 0) {
                // If someone explicitly set it to 0, generate a new one
                int numericId = Math.abs(UUID.randomUUID().hashCode());
                if (numericId == 0) {
                    numericId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                }
                goal.setId(numericId);
                System.out.println("Replaced zero ID with: " + numericId);
            }

            // ✅ Set goalId for all subgoals AFTER we have a valid goal ID
            if (goal.getSubgoals() != null) {
                for (Subgoal subgoal : goal.getSubgoals()) {
                    subgoal.setGoalId(goal.getId());
                }
            }

            // Convert Goal to Map for Firestore
            Map<String, Object> goalData = convertGoalToMap(goal);

            // Save to Firestore using the numeric ID as document ID
            String docId = String.valueOf(goal.getId());
            DocumentReference docRef = firestore.collection(GOALS_COLLECTION).document(docId);

            ApiFuture<WriteResult> result = docRef.set(goalData);

            // Wait for completion
            WriteResult writeResult = result.get();

            System.out.println("✅ Goal saved to Firestore at: " + writeResult.getUpdateTime());
            System.out.println("Document ID: " + docId);

            return goal;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error saving goal to Firestore: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save goal to Firestore", e);
        }
    }

    /**
     * Get a goal by ID from Firestore
     */
    public Goal getGoalById(int id) {
        try {
            String docId = String.valueOf(id);
            DocumentReference docRef = firestore.collection(GOALS_COLLECTION).document(docId);

            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                System.out.println("⚠️ Goal not found with ID: " + id);
                return null;
            }

            // Convert Firestore document to Goal object
            Goal goal = convertMapToGoal(document.getData());
            if (goal != null) {
                goal.setId(id);
            }

            System.out.println("✅ Goal retrieved from Firestore: " + (goal != null ? goal.getTitle() : "null"));
            return goal;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error fetching goal from Firestore: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch goal from Firestore", e);
        }
    }

    /**
     * Get a specific subgoal by goalId and subgoalTitle.
     */
    public Subgoal getSubgoal(int goalId, String subgoalTitle) {
        Goal goal = getGoalById(goalId);
        if (goal == null || goal.getSubgoals() == null) {
            return null;
        }

        for (Subgoal sub : goal.getSubgoals()) {
            if (sub.getTitle().equals(subgoalTitle)) {
                return sub;
            }
        }
        return null;
    }

    /**
     * Get all goals from Firebase
     */
    public List<Goal> getAllGoals() throws Exception {
        List<Goal> goals = new ArrayList<>();

        ApiFuture<QuerySnapshot> future = firestore.collection(GOALS_COLLECTION).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            try {
                Goal goal = convertMapToGoal(document.getData());
                if (goal != null) {
                    goals.add(goal);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse goal from document: " + document.getId());
                e.printStackTrace();
            }
        }

        System.out.println("Retrieved " + goals.size() + " goals from Firebase");
        return goals;
    }

    /**
     * Get a goal by its title
     */
    public Goal getGoalByTitle(String title) throws Exception {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        // Query Firestore for goals with matching title
        ApiFuture<QuerySnapshot> future = firestore.collection(GOALS_COLLECTION)
                .whereEqualTo("title", title)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            System.out.println("No goal found with title: " + title);
            return null;
        }

        if (documents.size() > 1) {
            System.out.println("Warning: Multiple goals found with title: " + title + ". Returning first one.");
        }

        Goal goal = convertMapToGoal(documents.get(0).getData());
        System.out.println("Found goal with title: " + title);
        return goal;
    }

    /**
     * Mark a subgoal as completed.
     * Returns true if successfully updated, false otherwise.
     */
    public boolean markSubgoalComplete(int goalId, String subgoalTitle, boolean completed) {
        Goal goal = getGoalById(goalId);
        if (goal == null || goal.getSubgoals() == null) {
            return false;
        }

        boolean found = false;
        for (Subgoal sub : goal.getSubgoals()) {
            if (sub.getTitle().equals(subgoalTitle)) {
                sub.setCompleted(completed);
                found = true;
                break;
            }
        }

        if (found) {
            updateGoal(goal);
            return true;
        }

        return false;
    }

    /**
     * Delete a goal by ID from Firestore
     */
    public void deleteGoal(int id) {
        try {
            String docId = String.valueOf(id);
            ApiFuture<WriteResult> writeResult = firestore.collection(GOALS_COLLECTION)
                    .document(docId)
                    .delete();

            writeResult.get();
            System.out.println("✅ Goal deleted from Firestore with ID: " + id);

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error deleting goal from Firestore: " + e.getMessage());
            throw new RuntimeException("Failed to delete goal from Firestore", e);
        }
    }

    /**
     * Update a goal in Firestore
     */
    public Goal updateGoal(Goal goal) {
        try {
            if (goal.getId() == null) {
                throw new IllegalArgumentException("Cannot update goal without ID");
            }

            Map<String, Object> goalData = convertGoalToMap(goal);
            String docId = String.valueOf(goal.getId());

            ApiFuture<WriteResult> result = firestore.collection(GOALS_COLLECTION)
                    .document(docId)
                    .set(goalData, SetOptions.merge());

            result.get();
            System.out.println("✅ Goal updated in Firestore");

            return goal;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error updating goal: " + e.getMessage());
            throw new RuntimeException("Failed to update goal in Firestore", e);
        }
    }

    /**
     * Convert Goal entity to Firestore Map
     */
    private Map<String, Object> convertGoalToMap(Goal goal) {
        Map<String, Object> data = new HashMap<>();

        // ✅ Add null check for ID
        if (goal.getId() != null) {
            data.put("id", goal.getId());
        } else {
            throw new IllegalStateException("Goal ID cannot be null when saving to Firestore");
        }

        data.put("title", goal.getTitle());
        data.put("description", goal.getDescription());
        data.put("deadline", goal.getDeadline() != null ? goal.getDeadline().toString() : null);
        data.put("createdAt", com.google.cloud.Timestamp.now());

        // Convert subgoals to list of maps
        List<Map<String, Object>> subgoalsList = new ArrayList<>();
        if (goal.getSubgoals() != null) {
            for (Subgoal subgoal : goal.getSubgoals()) {
                Map<String, Object> subgoalMap = new HashMap<>();
                subgoalMap.put("goalId", subgoal.getGoalId());
                subgoalMap.put("title", subgoal.getTitle());
                subgoalMap.put("description", subgoal.getDescription());
                subgoalMap.put("completed", subgoal.isCompleted());
                subgoalsList.add(subgoalMap);
            }
        }
        data.put("subgoals", subgoalsList);

        return data;
    }

    /**
     * Convert Firestore Map to Goal entity
     */
    private Goal convertMapToGoal(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Goal goal = new Goal();

        // Handle ID safely
        Object idObj = data.get("id");
        if (idObj != null) {
            if (idObj instanceof Number) {
                goal.setId(((Number) idObj).intValue());
            } else if (idObj instanceof String) {
                try {
                    goal.setId(Integer.parseInt((String) idObj));
                } catch (NumberFormatException e) {
                    goal.setId(Math.abs(idObj.hashCode()));
                }
            }
        }

        // Handle title safely
        Object titleObj = data.get("title");
        goal.setTitle(titleObj != null ? titleObj.toString() : "Untitled Goal");

        // Handle description safely
        Object descObj = data.get("description");
        goal.setDescription(descObj != null ? descObj.toString() : "No description");

        // Handle deadline safely
        Object deadlineObj = data.get("deadline");
        if (deadlineObj != null) {
            try {
                goal.setDeadline(java.time.LocalDate.parse(deadlineObj.toString()));
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse deadline: " + deadlineObj);
                goal.setDeadline(java.time.LocalDate.now().plusMonths(1));
            }
        }

        // Handle subgoals safely
        Object subgoalsObj = data.get("subgoals");
        if (subgoalsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subgoalsList = (List<Map<String, Object>>) subgoalsObj;

            for (Map<String, Object> subgoalMap : subgoalsList) {
                try {
                    Subgoal subgoal = new Subgoal();

                    // Handle goalId
                    Object goalIdObj = subgoalMap.get("goalId");
                    if (goalIdObj instanceof Number) {
                        subgoal.setGoalId(((Number) goalIdObj).intValue());
                    }

                    // Handle title
                    Object subTitleObj = subgoalMap.get("title");
                    subgoal.setTitle(subTitleObj != null ? subTitleObj.toString() : "Untitled Subgoal");

                    // Handle description
                    Object subDescObj = subgoalMap.get("description");
                    subgoal.setDescription(subDescObj != null ? subDescObj.toString() : "");

                    // Handle completed status
                    Object completedObj = subgoalMap.get("completed");
                    if (completedObj instanceof Boolean) {
                        subgoal.setCompleted((Boolean) completedObj);
                    } else {
                        subgoal.setCompleted(false);
                    }

                    goal.addSubgoal(subgoal);

                } catch (Exception e) {
                    System.err.println("⚠️ Failed to parse subgoal: " + e.getMessage());
                }
            }
        }

        return goal;
    }
}