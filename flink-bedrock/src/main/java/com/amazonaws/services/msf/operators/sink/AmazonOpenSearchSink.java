//package com.amazonaws.services.msf.operators.sink;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.flink.api.common.functions.RuntimeContext;
//import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
//import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
//import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
//import org.apache.http.HttpHost;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.client.CredentialsProvider;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
//import org.elasticsearch.action.index.IndexRequest;
//import org.elasticsearch.client.Requests;
//import org.elasticsearch.client.RestClientBuilder;
//import org.elasticsearch.common.xcontent.XContentType;
//import org.json.JSONObject;
//
///**
// * Sink class for writing data to Amazon OpenSearch using ElasticsearchSink.
// */
//public class AmazonOpenSearchSink {
//
//    // Elasticsearch bulk flush configurations
//    private static final int FLUSH_MAX_ACTIONS = 10_000;
//    private static final long FLUSH_INTERVAL_MILLIS = 1_000;
//    private static final int FLUSH_MAX_SIZE_MB = 1;
//
//    /**
//     * Builds an ElasticsearchSink for writing JSON data to Amazon OpenSearch.
//     *
//     * @param elasticsearchEndpoint The Elasticsearch endpoint.
//     * @param osUser                The OpenSearch username.
//     * @param osPassword            The OpenSearch password.
//     * @param indexName             The name of the index.
//     * @return ElasticsearchSink for writing JSON data to Amazon OpenSearch.
//     */
//    public static ElasticsearchSink<JSONObject> buildOpenSearchSink(String elasticsearchEndpoint, String osUser, String osPassword, String indexName) {
//        ElasticsearchSink.Builder<JSONObject> esSinkBuilder = getJsonObjectBuilder(elasticsearchEndpoint, indexName);
//
//        // Set Elasticsearch bulk flush configurations
//        esSinkBuilder.setBulkFlushMaxActions(FLUSH_MAX_ACTIONS);
//        esSinkBuilder.setBulkFlushInterval(FLUSH_INTERVAL_MILLIS);
//        esSinkBuilder.setBulkFlushMaxSizeMb(FLUSH_MAX_SIZE_MB);
//        esSinkBuilder.setBulkFlushBackoff(true);
//
//        // Set REST client factory with HTTP client configuration callback for username and password
//        esSinkBuilder.setRestClientFactory(
//                restClientBuilder -> restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
//                    @Override
//                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
//                        // Set OpenSearch username and password for authentication
//                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(osUser, osPassword));
//                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
//                    }
//                }));
//
//        return esSinkBuilder.build();
//    }
//
//    /**
//     * Creates a builder for ElasticsearchSink with the specified Elasticsearch endpoint and index name.
//     *
//     * @param elasticsearchEndpoint The Elasticsearch endpoint.
//     * @param indexName             The name of the index.
//     * @return ElasticsearchSink.Builder for JSON data.
//     */
//    private static ElasticsearchSink.Builder<JSONObject> getJsonObjectBuilder(String elasticsearchEndpoint, String indexName) {
//        HttpHost httpHost = new HttpHost(elasticsearchEndpoint, 443, "https");
//        final List<HttpHost> httpHosts = Arrays.asList(httpHost);
//
//        return new ElasticsearchSink.Builder<>(
//                httpHosts,
//                new ElasticsearchSinkFunction<JSONObject>() {
//                    // Creates an IndexRequest for Elasticsearch based on the JSON element
//                    public IndexRequest createIndexRequest(JSONObject element) {
//                        Map<String, Object> json = new HashMap<>();
//                        json.put("embeddings", element.getJSONArray("embedding"));
//                        json.put("@timestamp", element.get("@timestamp"));
//
//                        // Add additional fields based on the indexName
//                        if (indexName.contains("custom")) {
//                            json.put("text", element.get("text"));
//                        } else {
//                            json.put("tweet", element.get("tweet"));
//                            json.put("likes", element.get("likes"));
//                            json.put("retweet_count", element.get("retweet_count"));
//                            json.put("impression_count", element.get("impression_count"));
//                        }
//
//                        // Create and return an IndexRequest
//                        return Requests.indexRequest()
//                                .index(indexName)
//                                .id(element.get("_id").toString())
//                                .source(json, XContentType.JSON);
//                    }
//
//                    // Processes the JSON element and adds it to the RequestIndexer
//                    @Override
//                    public void process(JSONObject element, RuntimeContext ctx, RequestIndexer indexer) {
//                        indexer.add(createIndexRequest(element));
//                    }
//                }
//        );
//    }
//}
