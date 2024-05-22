package com.amazonaws.services.msf.pojo;

import java.util.List;

public class TweetPojo {
    private String lang;
    private String id;
    private String text;
    private List<String> edit_history_tweet_ids;
    private String created_at;
    private String author_id;

    private PublicMetrics public_metrics; // Added field for organic_metrics

    // Getter and setter methods

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getEdit_history_tweet_ids() {
        return edit_history_tweet_ids;
    }

    public void setEdit_history_tweet_ids(List<String> edit_history_tweet_ids) {
        this.edit_history_tweet_ids = edit_history_tweet_ids;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getAuthor_id() {
        return author_id;
    }

    public void setAuthor_id(String author_id) {
        this.author_id = author_id;
    }

    public PublicMetrics getPublic_metrics() {
        return public_metrics;
    }

    public void setPublic_metrics(PublicMetrics public_metrics) {
        this.public_metrics = public_metrics;
    }
    @Override
    public String toString() {
        return "Tweet{" +
                "lang='" + lang + '\'' +
                ", id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", edit_history_tweet_ids=" + edit_history_tweet_ids +
                ", created_at='" + created_at + '\'' +
                ", author_id='" + author_id + '\'' +
                ", public_metrics=" + public_metrics +
                '}';
    }
}
