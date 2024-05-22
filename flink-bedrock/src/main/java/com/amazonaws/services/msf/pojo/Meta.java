package com.amazonaws.services.msf.pojo;

public class Meta {
    private String newest_id;
    private String oldest_id;
    private int result_count;

    private String next_token;

    // Getter and setter methods

    public String getNewest_id() {
        return newest_id;
    }

    public void setNewest_id(String newest_id) {
        this.newest_id = newest_id;
    }

    public String getOldest_id() {
        return oldest_id;
    }

    public void setOldest_id(String oldest_id) {
        this.oldest_id = oldest_id;
    }

    public int getResult_count() {
        return result_count;
    }

    public void setResult_count(int result_count) {
        this.result_count = result_count;
    }


    public String getNext_token() {
        return next_token;
    }


    public void setNext_token(String next_token) {
        this.next_token = next_token;
    }

    @Override
    public String toString() {
        return "Meta{" +
                "newest_id='" + newest_id + '\'' +
                ", oldest_id='" + oldest_id + '\'' +
                ", result_count=" + result_count +
                ", next_token='" + next_token + '\'' +
                '}';
    }
}
