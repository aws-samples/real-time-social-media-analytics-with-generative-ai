package com.amazonaws.services.msf.pojo;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class TweetList {
    private List<TweetPojo> data;
    private Meta meta;

    // Getter and setter methods

    public List<TweetPojo> getData() {
        return data;
    }

    public void setData(List<TweetPojo> data) {
        this.data = data;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "TweetList{" +
                "data=" + data +
                ", meta=" + meta +
                '}';
    }

    public static TweetList fromJson(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, TweetList.class);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON to TweetList: " + e.getMessage(), e);
        }
    }
}