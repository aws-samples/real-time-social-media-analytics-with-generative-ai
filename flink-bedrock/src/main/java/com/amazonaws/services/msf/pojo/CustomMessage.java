package com.amazonaws.services.msf.pojo;

import java.util.Date;

public class CustomMessage {private String text;
    private String created_at;

    // Default constructor
    public CustomMessage() {
    }

    // Constructor with parameters
    public CustomMessage(String text, String created_at) {
        this.text = text;
        this.created_at = created_at;
    }

    // Getter for text
    public String getText() {
        return text;
    }

    // Setter for text
    public void setText(String text) {
        this.text = text;
    }

    // Getter for created_at
    public String getCreated_at() {
        return created_at;
    }

    // Setter for created_at
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "CustomMessage{" +
                "text='" + text + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}
