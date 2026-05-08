package com.notes.notesproxmlviews;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

public class Note {
    @DocumentId
    private String id;
    private String title;
    private String content;
    private String imageUrl;
    private String localImagePath;
    private Timestamp reminderTime;
    private boolean focusModeActive;
    private int focusDuration;

    @ServerTimestamp
    private Timestamp timestamp;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Construtor vazio necessário para Firestore
    public Note() {
        this.focusDuration = 25;
        this.focusModeActive = false;
    }

    // Construtor com parâmetros
    public Note(String title, String content) {
        this.title = title;
        this.content = content;
        this.focusDuration = 25;
        this.focusModeActive = false;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLocalImagePath() {
        return localImagePath;
    }

    public void setLocalImagePath(String localImagePath) {
        this.localImagePath = localImagePath;
    }

    public Timestamp getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(Timestamp reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isFocusModeActive() {
        return focusModeActive;
    }

    public void setFocusModeActive(boolean focusModeActive) {
        this.focusModeActive = focusModeActive;
    }

    public int getFocusDuration() {
        return focusDuration;
    }

    public void setFocusDuration(int focusDuration) {
        this.focusDuration = focusDuration;
    }

    public Timestamp getTimestamp() {
        return timestamp != null ? timestamp : createdAt;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}