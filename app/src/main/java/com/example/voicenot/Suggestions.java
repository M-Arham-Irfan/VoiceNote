package com.example.voicenot;
import com.example.voicenot.SuggestionReceiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class Suggestions extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private FirebaseFirestore db;
    private TextView suggestionText;
    private Button likeButton, dislikeButton;
    private String userId = "user_12345"; // Replace with dynamic user ID
    private String openAiKey = "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"; // Add your OpenAI API key here
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestions);
        FirebaseApp.initializeApp(this);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        suggestionText = findViewById(R.id.suggestionText);
        likeButton = findViewById(R.id.likeButton);
        dislikeButton = findViewById(R.id.dislikeButton);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);

        // Fetch and display AI-powered suggestion
        fetchSuggestion();

        // Handle Like button click
        likeButton.setOnClickListener(v -> saveFeedback(true));

        // Handle Dislike button click
        dislikeButton.setOnClickListener(v -> saveFeedback(false));
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US); // Set language
        } else {
            Log.e("TTS", "Initialization failed");
        }
    }

    private void speakSuggestion(String suggestion) {
        if (textToSpeech != null) {
            textToSpeech.speak(suggestion, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void fetchSuggestion() {
        db.collection("suggestions").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String cachedSuggestion = documentSnapshot.getString("activity");
                        suggestionText.setText(cachedSuggestion);
                        speakSuggestion(cachedSuggestion); // Speak the suggestion
                    } else {
                        callChatGPT();
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching suggestion", e));
    }

    private void callChatGPT() {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("model", "gpt-3.5-turbo");
            json.put("messages", new JSONArray().put(new JSONObject()
                            .put("role", "system")
                            .put("content", "You provide personalized activity suggestions."))
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", "Suggest a personalized activity for me.")));

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + openAiKey)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("ChatGPT API", "API Call Failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String suggestion = jsonResponse.getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            runOnUiThread(() -> {
                                suggestionText.setText(suggestion);
                                speakSuggestion(suggestion); // Speak the suggestion
                            });

                            // Cache suggestion in Firebase
                            db.collection("suggestions").document(userId)
                                    .set(new
                                            SuggestionReceiver(suggestion))
                                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Suggestion Cached"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Added saveFeedback method to store user feedback in Firestore
    private void saveFeedback(boolean isLiked) {
        db.collection("feedback").document(userId)
                .set(new Feedback(isLiked))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(Suggestions.this, "Feedback saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error saving feedback", e));
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}