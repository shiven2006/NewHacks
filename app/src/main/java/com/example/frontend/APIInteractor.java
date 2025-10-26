package com.example.frontend;

import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIInteractor {
    private String _APIurl = "http://10.0.2.2:8080/api/goals";

    public CompletableFuture<String> GenerateMainGoal(String prompt) {
        OkHttpClient client = new OkHttpClient();
        java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8"); // or .get(...) on OkHttp 4
        String jsonBody = "{ \"prompt\": \"" + prompt + "\" }";
        RequestBody body = RequestBody.create(JSON, jsonBody);

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8080/api/goals/generate")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                future.completeExceptionally(e);
            }
            @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try (okhttp3.ResponseBody rb = response.body()) {
                    future.complete(rb != null ? rb.string() : "");
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }
}
