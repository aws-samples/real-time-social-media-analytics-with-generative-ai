package com.amazonaws.services.msf.operators.filter;

import com.amazonaws.services.msf.pojo.TweetPojo;
import org.apache.flink.api.common.functions.RichFilterFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;

/**
 * Filter function for deduplicating TweetPojo objects based on their tweet IDs.
 */
public class DeduplicationFilter extends RichFilterFunction<TweetPojo> {

    private transient MapState<String, Boolean> seenTweets;

    /**
     * Filters out duplicate tweets based on tweet IDs.
     *
     * @param tweetPojo The input TweetPojo object.
     * @return true if the tweet is not a duplicate, false otherwise.
     * @throws Exception Thrown if an error occurs during filtering.
     */
    @Override
    public boolean filter(TweetPojo tweetPojo) throws Exception {
        String tweetId = tweetPojo.getId();
        if (!seenTweets.contains(tweetId)) {
            seenTweets.put(tweetId, true);
            return true; // Not seen before, allow the item
        } else {
            return false; // Duplicate, filter it out
        }
    }

    /**
     * Initializes the MapState for tracking seen tweets.
     *
     * @param parameters Configuration parameters for the function.
     * @throws Exception Thrown if an error occurs during initialization.
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        // Initialize the state
        MapStateDescriptor<String, Boolean> descriptor = new MapStateDescriptor<>(
                "seenTweets",
                String.class,
                Boolean.class
        );
        seenTweets = getRuntimeContext().getMapState(descriptor);
    }
}
