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
            // Generate unique ID if not set
            if (goal.getId() == 0 || goal.getId() == 0) {
                DocumentReference docRef = firestore.collection(GOALS_COLLECTION).document();
                goal.setId(docRef.getId().hashCode()); // Use hashcode for numeric ID
            }

            // Convert Goal to Map for Firestore
            Map<String, Object> goalData = convertGoalToMap(goal);

            // Save to Firestore
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
            goal.setId(id);

            System.out.println("✅ Goal retrieved from Firestore: " + goal.getTitle());
            return goal;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error fetching goal from Firestore: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch goal from Firestore", e);
        }
    }


    /**
     * Get a specific subgoal by goalId and subgoalId.
     */
    public Subgoal getSubgoal(int goalId, String subgoalTitle) {
        Goal goal = getGoalById(goalId);
        if (goal == null || goal.getSubgoals() == null) {
            return null;
        }

        for (Subgoal sub : goal.getSubgoals()) {
            // Use hashCode as ID if that's how IDs are generated
            if (sub.getTitle().equals(subgoalTitle)) {
                return sub;
            }
        }
        return null;
    }

    /**
     * Mark a subgoal as completed.
     * Returns true if successfully updated, false otherwise.
     */
    public boolean markSubgoalComplete(int goalId, String subgoalTitle, boolean completed) {
        Goal goal = getGoalById(goalId);
        if (goal == null || goal.getSubgoals() == null) return false;

        for (Subgoal sub : goal.getSubgoals()) {
            if (sub.getTitle().equals(subgoalTitle)) {
                sub.setCompleted(completed);
                updateGoal(goal);  // Save updated goal to Firestore
                return true;
            }
        }
        return false;
    }


    /**
     * Get all goals from Firestore
     */
    public List<Goal> getAllGoals() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(GOALS_COLLECTION).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Goal> goals = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                Goal goal = convertMapToGoal(document.getData());
                // ✅ Use the stored ID instead of hashCode
                Object idObj = document.getData().get("id");
                if (idObj != null) {
                    goal.setId(((Number) idObj).intValue());
                } else {
                    goal.setId(document.getId().hashCode());
                }
                goals.add(goal);
            }

            System.out.println("✅ Retrieved " + goals.size() + " goals from Firestore");
            return goals;

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Error fetching all goals: " + e.getMessage());
            throw new RuntimeException("Failed to fetch goals from Firestore", e);
        }
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
        data.put("id", goal.getId());
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
                subgoalMap.put("completed", subgoal.isCompleted());  // ✅ Fixed method call
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

        // ✅ Handle ID safely
        Object idObj = data.get("id");
        if (idObj != null) {
            if (idObj instanceof Number) {
                goal.setId(((Number) idObj).intValue());
            } else if (idObj instanceof String) {
                try {
                    goal.setId(Integer.parseInt((String) idObj));
                } catch (NumberFormatException e) {
                    goal.setId(idObj.hashCode());
                }
            }
        }

        // ✅ Handle title safely
        Object titleObj = data.get("title");
        goal.setTitle(titleObj != null ? titleObj.toString() : "Untitled Goal");

        // ✅ Handle description safely
        Object descObj = data.get("description");
        goal.setDescription(descObj != null ? descObj.toString() : "No description");

        // ✅ Handle deadline safely
        Object deadlineObj = data.get("deadline");
        if (deadlineObj != null) {
            try {
                goal.setDeadline(java.time.LocalDate.parse(deadlineObj.toString()));
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse deadline: " + deadlineObj);
                goal.setDeadline(java.time.LocalDate.now().plusMonths(1));
            }
        }

        // ✅ Handle subgoals safely
        Object subgoalsObj = data.get("subgoals");
        if (subgoalsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> subgoalsList = (List<Map<String, Object>>) subgoalsObj;

            for (Map<String, Object> subgoalMap : subgoalsList) {
                try {
                    Subgoal subgoal = new Subgoal();

                    // ✅ Handle goalId
                    Object goalIdObj = subgoalMap.get("goalId");
                    if (goalIdObj instanceof Number) {
                        subgoal.setGoalId(((Number) goalIdObj).intValue());
                    }

                    // ✅ Handle title
                    Object subTitleObj = subgoalMap.get("title");
                    subgoal.setTitle(subTitleObj != null ? subTitleObj.toString() : "Untitled Subgoal");

                    // ✅ Handle description
                    Object subDescObj = subgoalMap.get("description");
                    subgoal.setDescription(subDescObj != null ? subDescObj.toString() : "");

                    // ✅ Handle completed status
                    Object completedObj = subgoalMap.get("completed");
                    if (completedObj instanceof Boolean) {
                        subgoal.setCompleted((Boolean) completedObj);
                    } else {
                        subgoal.setCompleted(false);
                    }

                    goal.addSubgoal(subgoal);

                } catch (Exception e) {
                    System.err.println("⚠️ Failed to parse subgoal: " + e.getMessage());
                    // Continue with next subgoal
                }
            }
        }

        return goal;
    }


}