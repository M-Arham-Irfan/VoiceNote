package com.example.voicenot;

public class SuggestionReceiver { // Renamed class
    private String activity;

    // Empty constructor required for Firestore
    public SuggestionReceiver() {
    }

    public SuggestionReceiver(String activity) {
        this.activity = activity;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}