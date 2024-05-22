package com.amazonaws.services.msf.operators.process;

import com.amazonaws.services.msf.pojo.TweetList;
import com.amazonaws.services.msf.pojo.TweetPojo;
import com.amazonaws.services.msf.pojo.Rules;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Process function for handling Rules and triggering Twitter API searches based on specified rules.
 */
public class XProcessFunction extends ProcessFunction<Rules, TweetPojo> {

    private transient ValueState<String> queryState;
    private transient ValueState<Long> timerState;
    private transient ValueState<Long> timerRuleState;
    private transient ValueState<String> nextTokenState;
    private transient ValueState<String> apiKeyState;
    private final int maxResults = 15; // Set your desired maximum results here

    /**
     * Action to be performed when a timer fires, triggering a Twitter API search.
     *
     * @param timestamp The timestamp when the timer fired.
     * @param ctx       The context for accessing timer services and emitting results.
     * @param out       The collector for emitting TweetPojo objects.
     * @throws Exception Thrown if an error occurs during timer processing.
     */
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<TweetPojo> out) throws Exception {
        Long currentTimer = timerState.value();
        String query = queryState.value();
        String nextToken = nextTokenState.value();
        String apiKey = apiKeyState.value();

        ctx.timerService().deleteProcessingTimeTimer(currentTimer);

        // Call the Twitter API and collect tweets
        if (query != null && apiKey != null) {
            Tuple2<List<TweetPojo>, String> result = searchTweets(apiKey, query, nextToken, maxResults);
            List<TweetPojo> tweets = result.f0;
            String newNextToken = result.f1;

            for (TweetPojo tweet : tweets) {
                out.collect(tweet);
            }

            // Update the next token state for the next iteration
            nextTokenState.update(newNextToken);
        }

        // Set a new timer based on the duration stored in the state
        long state = timerRuleState.value();
        long newTimer = ctx.timerService().currentProcessingTime() + state * 1000;
        ctx.timerService().registerProcessingTimeTimer(newTimer);
        timerState.update(newTimer);
    }

    /**
     * Initialization method to set up state and descriptors.
     *
     * @param parameters Configuration parameters for the function.
     * @throws Exception Thrown if an error occurs during initialization.
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<Long> timerStateDescriptor =
                new ValueStateDescriptor<>("timerState", Long.class);

        ValueStateDescriptor<Long> timerRuleStateDescriptor =
                new ValueStateDescriptor<>("timerRuleState", Long.class);

        ValueStateDescriptor<String> queryStateDescriptor =
                new ValueStateDescriptor<>("queryState", String.class);

        ValueStateDescriptor<String> nextTokenDescriptor =
                new ValueStateDescriptor<>("nextTokenDescriptor", String.class);

        ValueStateDescriptor<String> apiKeyDescriptor =
                new ValueStateDescriptor<>("apiKeyDescriptor", String.class);

        apiKeyState = getRuntimeContext().getState(apiKeyDescriptor);
        queryState = getRuntimeContext().getState(queryStateDescriptor);
        timerState = getRuntimeContext().getState(timerStateDescriptor);
        timerRuleState = getRuntimeContext().getState(timerRuleStateDescriptor);
        nextTokenState = getRuntimeContext().getState(nextTokenDescriptor);
    }

    /**
     * Process method to handle incoming Rules and trigger actions accordingly.
     *
     * @param rules     The input Rules object.
     * @param context   The context for accessing timer services and emitting results.
     * @param collector The collector for emitting TweetPojo objects.
     * @throws Exception Thrown if an error occurs during Rules processing.
     */
    @Override
    public void processElement(Rules rules, ProcessFunction<Rules, TweetPojo>.Context context, Collector<TweetPojo> collector) throws Exception {
        Long currentTimer = timerState.value();

        if (rules.getRule().equals("apiKey")) {
            String apiKey = rules.getKey();
            apiKeyState.update(apiKey);
        }

        if (rules.getRule().equals("timer")) {
            long timer = Long.parseLong(rules.getKey());
            if (timer >= 15) {
                timerRuleState.update(timer);
                long newTimer = context.timerService().currentProcessingTime() + timer * 1000;
                context.timerService().registerProcessingTimeTimer(newTimer);
                timerState.update(newTimer);
            } else if (currentTimer != null) {
                context.timerService().deleteProcessingTimeTimer(currentTimer);
            }
        }

        if ("query".equals(rules.getRule())) {
            // Extract the values from the record and update the state
            String newQuery = rules.getKey();
            if (!newQuery.isEmpty()) {
                queryState.update(newQuery);
            }
        }
    }

    /**
     * Method to perform the Twitter API search and return the JSON response.
     *
     * @param apiKey         The Twitter API key for authentication.
     * @param searchKeywords The keywords to search for on Twitter.
     * @param nextToken      The next token for pagination.
     * @param maxResults     The maximum number of results to retrieve.
     * @return Tuple2 containing the list of TweetPojo objects and the next token for pagination.
     * @throws Exception Thrown if an error occurs during the Twitter API search.
     */
    public static Tuple2<List<TweetPojo>, String> searchTweets(String apiKey, String searchKeywords, String nextToken, int maxResults) throws Exception {
        String apiUrl = "https://api.twitter.com/2/tweets/search/recent";

        // Encode the search string for the API request
        String encodedSearchString = URLEncoder.encode(searchKeywords + " lang:en" + " -is:retweet ", "UTF-8");
        String endpoint = apiUrl + "?query=" + encodedSearchString;

        // Append nextToken to the request if available
        if (nextToken != null && !nextToken.isEmpty()) {
            endpoint += "&next_token=" + nextToken;
        }

        // Specify the maximum results and tweet fields in the request
        endpoint += "&max_results=" + maxResults + "&tweet.fields=created_at,public_metrics,lang,author_id&=";

        // Create the URL and open a connection
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;

            // Read the response from the API
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            // Parse the JSON response into a list of TweetPojo
            TweetList tweetList = TweetList.fromJson(response.toString());
            String newNextToken = tweetList.getMeta().getNext_token();
            List<TweetPojo> tweets = new ArrayList<>();

            if (tweetList.getData() != null) {
                tweets = tweetList.getData();
            }

            return Tuple2.of(tweets, newNextToken);
        } catch (Exception e) {
            // Throw an exception if the API request fails
            throw new RuntimeException("Failed to fetch and parse data from Twitter API: " + e.getMessage(), e);
        } finally {
            connection.disconnect(); // Close the connection
        }
    }
}
