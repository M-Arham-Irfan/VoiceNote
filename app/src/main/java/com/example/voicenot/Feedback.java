package com.example.voicenot;

public class Feedback {
    private boolean liked;

    // Empty constructor for Firestore
    public Feedback() {
    }

    public Feedback(boolean liked) {
        this.liked = liked;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }
}