package com.example.voicenot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class TaskNotificationReceiver extends BroadcastReceiver {
    private TextToSpeech tts;

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskDescription = intent.getStringExtra("taskDescription");
        String taskType = intent.getStringExtra("taskType");
        boolean isSecondReminder = intent.getBooleanExtra("isSecondReminder", false); // NEW

        Log.d("TaskNotificationReceiver", "Received Task: " + taskDescription + ", Type: " + taskType + ", SecondReminder: " + isSecondReminder);

        // Initialize TTS
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);

                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "task_utterance");

                // ✅ Choose the correct voice message
                String ttsMessage;
                if (taskType != null && taskType.equals("Important & Urgent")) {
                    if (isSecondReminder) {
                        ttsMessage = "Reminder again: " + taskDescription;
                    } else {
                        ttsMessage = "You have a task: " + taskDescription;
                    }
                } else {
                    ttsMessage = "Reminder: " + taskDescription;
                }

                tts.speak(ttsMessage, TextToSpeech.QUEUE_FLUSH, params);

                // Shutdown TTS after speaking
                tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d("TaskNotificationReceiver", "TTS Started");
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d("TaskNotificationReceiver", "TTS Completed");
                        if (tts != null) {
                            tts.shutdown();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("TaskNotificationReceiver", "TTS Error");
                    }
                });
            } else {
                Log.e("TaskNotificationReceiver", "TTS Initialization Failed");
            }
        });
    }
}


/*
public class TaskNotificationReceiver extends BroadcastReceiver {
    private TextToSpeech tts;

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskDescription = intent.getStringExtra("taskDescription");
        Log.d("TaskNotificationReceiver", "Received Task: " + taskDescription);

        // Initialize TTS
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                tts.speak("Reminder: " + taskDescription, TextToSpeech.QUEUE_FLUSH, null, null);

                // Delay shutdown to allow TTS to complete
                tts.setOnUtteranceCompletedListener(utteranceId -> {
                    Log.d("TaskNotificationReceiver", "TTS Completed");
                    tts.shutdown();
                });
            } else {
                Log.e("TaskNotificationReceiver", "TTS Initialization Failed");
            }
        });
    }
}
*/

    /*public void onReceive(Context context, Intent intent) {
        String taskDescription = intent.getStringExtra("taskDescription");

        // Initialize TTS
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
                tts.speak("Reminder: " + taskDescription, TextToSpeech.QUEUE_FLUSH, null, null);
                // Shutdown TTS after speaking
                tts.shutdown();
            }
        });
    }
}
*/