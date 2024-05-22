package com.amazonaws.services.msf.operators.map;

import com.amazonaws.services.msf.pojo.TweetPojo;
import org.apache.flink.api.common.functions.MapFunction;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Map function for converting a TweetPojo object to a JSONObject.
 */
public class TweetPojoToJSONObject implements MapFunction<TweetPojo, JSONObject> {

    /**
     * Converts a TweetPojo object to a JSONObject.
     *
     * @param tweet The input TweetPojo object.
     * @return JSONObject representing the mapped fields of the TweetPojo.
     * @throws ParseException Thrown if there is an issue with parsing or formatting dates.
     */
    @Override
    public JSONObject map(TweetPojo tweet) throws ParseException {
        // Create a new JSONObject to store mapped fields from TweetPojo
        JSONObject result = new JSONObject();
        // Map "tweet" field from TweetPojo to "tweet" field in JSONObject
        result.put("tweet", tweet.getText());
        // Map "created_at" field from TweetPojo to "created_at" field in JSONObject
        result.put("created_at", tweet.getCreated_at());
        // Map "likes" field from TweetPojo to "likes" field in JSONObject
        result.put("likes", tweet.getPublic_metrics().getLike_count());
        // Map "retweet_count" field from TweetPojo to "retweet_count" field in JSONObject
        result.put("retweet_count", tweet.getPublic_metrics().getRetweet_count());
        // Map "impression_count" field from TweetPojo to "impression_count" field in JSONObject
        result.put("impression_count", tweet.getPublic_metrics().getImpression_count());
        // Map "_id" field from TweetPojo to "_id" field in JSONObject
        result.put("_id", tweet.getId());

        // Return the final JSONObject
        return result;
    }
}
