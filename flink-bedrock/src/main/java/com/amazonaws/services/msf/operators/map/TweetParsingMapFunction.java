package com.amazonaws.services.msf.operators.map;

import com.amazonaws.services.msf.pojo.TweetPojo;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * Map function for processing and cleaning the text field in a TweetPojo.
 */
public class TweetParsingMapFunction implements MapFunction<TweetPojo, TweetPojo> {

    /**
     * Processes and cleans the text field of a TweetPojo.
     *
     * @param tweet The input TweetPojo object.
     * @return TweetPojo with the processed text field.
     * @throws Exception Thrown if an error occurs during text processing.
     */
    @Override
    public TweetPojo map(TweetPojo tweet) throws Exception {
        // Process and clean the text field in the tweet
        String processedText = tweet.getText()
                .replaceAll("@\\w+", "")  // Remove @mentions
                .replaceAll("https?://\\S+\\s?", "")  // Remove URLs
                .replaceAll("\\n", " ");  // Replace newlines with spaces

        // Set the processed text back to the tweet
        tweet.setText(processedText);

        // Return the modified tweet
        return tweet;
    }
}
