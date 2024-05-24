package com.amazonaws.services.msf.operators.asyncIO;

import com.amazonaws.services.msf.DataStreamJob;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.AsyncFunction;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * AsyncFunction implementation for invoking an embedding model using BedrockRuntimeAsyncClient
 * with specific enhancements for processing tweet-related data.
 */
public class BedRockEmbeddingModelAsyncTweetFunction extends RichAsyncFunction<JSONObject, JSONObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedRockEmbeddingModelAsyncTweetFunction.class);

    private transient BedrockRuntimeAsyncClient bedrockClient;

    private String region;

    public BedRockEmbeddingModelAsyncTweetFunction(String region) {
        this.region = region;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        bedrockClient = BedrockRuntimeAsyncClient.builder()
                .region(Region.of(region))  // Use the specified AWS region
                .build();
    }

    @Override
    public void close() throws Exception {
        bedrockClient.close();
    }


    /**
     * Asynchronously invoke the embedding model and complete the ResultFuture with the result.
     *
     * @param jsonObject   The input JSON object representing a tweet.
     * @param resultFuture The ResultFuture to be completed with the result.
     * @throws Exception    If an error occurs during the asynchronous invocation.
     */
    @Override
    public void asyncInvoke(JSONObject jsonObject, ResultFuture<JSONObject> resultFuture) throws Exception {
        // Simulate an asynchronous call using CompletableFuture
        CompletableFuture.supplyAsync(new Supplier<JSONObject>() {
            @Override
            public JSONObject get() {
                try {

                    // Extract tweet content from input JSON object
                    String stringBody = jsonObject.getString("tweet");
                    ArrayList<String> stringList = new ArrayList<>();

                    stringList.add(stringBody);
                    // Prepare input JSON for the model invocation
                    JSONObject jsonBody = new JSONObject()
                            .put("inputText", stringBody);

                    SdkBytes body = SdkBytes.fromUtf8String(jsonBody.toString());

                    // Prepare model invocation request
                    InvokeModelRequest request = InvokeModelRequest.builder()
                            .modelId("amazon.titan-embed-text-v1")
                            .contentType("application/json")
                            .accept("*/*")
                            .body(body)
                            .build();

                    // Invoke the model asynchronously and get the CompletableFuture for the response
                    CompletableFuture<InvokeModelResponse> futureResponse = bedrockClient.invokeModel(request);

                    // Extract and process the response when it is available
                    JSONObject response = new JSONObject(
                            futureResponse.join().body().asString(StandardCharsets.UTF_8)
                    );

                    // Add additional fields related to tweet data to the response
                    response.put("tweet", jsonObject.get("tweet"));
                    response.put("@timestamp", jsonObject.get("created_at"));
                    response.put("likes", jsonObject.get("likes"));
                    response.put("retweet_count", jsonObject.get("retweet_count"));
                    response.put("impression_count", jsonObject.get("impression_count"));
                    response.put("_id", jsonObject.get("_id"));

                    return response;
                } catch (Exception e) {
                    LOGGER.error("Error during asynchronous invocation", e);
                    return null;
                }
            }
        }).thenAccept((JSONObject result) -> {
            // Complete the ResultFuture with the final result
            resultFuture.complete(Collections.singleton(result));
        });
    }
}
