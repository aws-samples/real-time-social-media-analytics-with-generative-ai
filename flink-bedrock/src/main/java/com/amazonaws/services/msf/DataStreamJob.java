/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.services.msf;

import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import com.amazonaws.services.msf.operators.asyncIO.BedRockEmbeddingModelAsyncCustomMessage;
import com.amazonaws.services.msf.operators.asyncIO.BedRockEmbeddingModelAsyncTweetFunction;
import com.amazonaws.services.msf.operators.filter.DeduplicationFilter;
import com.amazonaws.services.msf.operators.map.*;
import com.amazonaws.services.msf.operators.process.XProcessFunction;
import com.amazonaws.services.msf.pojo.Rules;
import com.amazonaws.services.msf.pojo.TweetPojo;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.opensearch.sink.OpensearchSink;
import org.apache.flink.connector.opensearch.sink.OpensearchSinkBuilder;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.AWSConfigConstants;
import org.apache.http.HttpHost;
import org.json.JSONObject;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.Requests;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants.STREAM_INITIAL_POSITION;

/**
 * Main class for the Twitter GenAi application that processes and analyzes tweets using Flink and OpenSearch.
 */
public class DataStreamJob {

	// Default configuration values
	private static final String DEFAULT_OS_DOMAIN = "";
	private static final String DEFAULT_OS_USER = "";
	private static final String DEFAULT_OS_PASSWORD = "";
	private static final String DEFAULT_OS_TWITTER_INDEX = "";
	private static final String DEFAULT_OS_TWITTER_CUSTOM_INDEX = "";
	private static final String DEFAULT_SOURCE_STREAM = "";
	private static final String DEFAULT_RULES_STREAM = "";
	private static final String DEFAULT_AWS_REGION = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(DataStreamJob.class);

	/**
	 * Checks if the execution environment is local.
	 *
	 * @param env The execution environment.
	 * @return True if the environment is local; false otherwise.
	 */
	private static boolean isLocal(StreamExecutionEnvironment env) {
		return env instanceof LocalStreamEnvironment;
	}

	/**
	 * Loads application parameters from the command line or runtime properties.
	 *
	 * @param args Command line arguments.
	 * @param env  The execution environment.
	 * @return Application parameters.
	 * @throws IOException If an I/O error occurs.
	 */
	private static ParameterTool loadApplicationParameters(String[] args, StreamExecutionEnvironment env) throws IOException {
		if (env instanceof LocalStreamEnvironment) {
			return ParameterTool.fromArgs(args);
		} else {
			Map<String, Properties> applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties();
			Properties flinkProperties = applicationProperties.get("FlinkApplicationProperties");
			if (flinkProperties == null) {
				throw new RuntimeException("Unable to load FlinkApplicationProperties properties from runtime properties");
			}
			Map<String, String> map = new HashMap<>(flinkProperties.size());
			flinkProperties.forEach((k, v) -> map.put((String) k, (String) v));
			return ParameterTool.fromMap(map);
		}
	}

	/**
	 * The main entry point for the application.
	 *
	 * @param args Command line arguments.
	 * @throws Exception If an exception occurs during execution.
	 */
	public static void main(String[] args) throws Exception {

		// Get the execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// Load application parameters
		final ParameterTool applicationProperties = loadApplicationParameters(args, env);

		// OpenSearch configuration values
		String osPassword = applicationProperties.get("os.password", DEFAULT_OS_PASSWORD);
		String osUser = applicationProperties.get("os.user", DEFAULT_OS_USER);
		String osEndpoint = applicationProperties.get("os.domain", DEFAULT_OS_DOMAIN);
		String twitterRagIndex = applicationProperties.get("os.index", DEFAULT_OS_TWITTER_INDEX);
		String customMessageIndex = applicationProperties.get("os.custom.index", DEFAULT_OS_TWITTER_CUSTOM_INDEX);
		String region = applicationProperties.get("region", DEFAULT_AWS_REGION);

		// Kinesis Consumer configuration
		Properties kinesisConsumerConfig = new Properties();
		kinesisConsumerConfig.put(AWSConfigConstants.AWS_REGION, region);
		kinesisConsumerConfig.put(STREAM_INITIAL_POSITION, "LATEST");

		// Source for rules stream
		FlinkKinesisConsumer<String> sourceRules = new FlinkKinesisConsumer<>(
				applicationProperties.get("kinesis.rules.stream", DEFAULT_RULES_STREAM),
				new SimpleStringSchema(),
				kinesisConsumerConfig
		);

		// Create DataStream of Rules from rules stream
		DataStream<Rules> rulesDataStream = env.addSource(sourceRules).uid("source-rules")
				.map(new RulesMapFunction()).uid("rules-map");

		// Process rules and tweets, convert to JSON, and filter duplicates
		DataStream<TweetPojo> tweetPojoDataStream = rulesDataStream
				.keyBy(x -> x.getId())
				.process(new XProcessFunction()).uid("x-process")
				.map(new TweetParsingMapFunction()).uid("tweet-clean-map");

		DataStream<JSONObject> inputJSON = tweetPojoDataStream
				.keyBy(TweetPojo::getAuthor_id)
				.filter(new DeduplicationFilter()).uid("deduplication-filter")
				.map(new TweetPojoToJSONObject()).uid("tweet-pojo-map")
				.returns(TypeInformation.of(JSONObject.class));

		// Perform asynchronous embedding model function
		DataStream<JSONObject> resultStream = AsyncDataStream.unorderedWait(
				inputJSON,
				new BedRockEmbeddingModelAsyncTweetFunction(region),
				15000,
				TimeUnit.MILLISECONDS,
				1000
		).uid("tweet-bedrock-async-function");

		OpensearchSink<JSONObject> twitterOpensearchSink = new OpensearchSinkBuilder<JSONObject>()
				.setHosts(new HttpHost(osEndpoint, 443, "https"))
				.setEmitter((element, context, indexer) -> indexer.add(createIndexRequest(element, twitterRagIndex)))
				.setConnectionUsername(osUser)
				.setConnectionPassword(osPassword)
				.setBulkFlushMaxActions(1)
				.build();

		// Sink the result to OpenSearch
		resultStream.sinkTo(twitterOpensearchSink);

		// Source for custom stream
		FlinkKinesisConsumer<String> sourceCustom = new FlinkKinesisConsumer<>(
				applicationProperties.get("kinesis.source.stream", DEFAULT_SOURCE_STREAM),
				new SimpleStringSchema(),
				kinesisConsumerConfig
		);

		// Create DataStream of CustomMessage from custom stream
		DataStream<String> input = env.addSource(sourceCustom).uid("source-custom");

		// Convert CustomMessage to JSON and perform asynchronous embedding
		DataStream<JSONObject> customMessageStream = input
				.map(new CustomMessageMapFunction()).uid("custom-message-map-function")
				.map(new CustomMessageToJSONObject()).uid("custom-message-jsonobject-map-function");

		DataStream<JSONObject> customMessageEmbedded = AsyncDataStream.unorderedWait(
				customMessageStream,
				new BedRockEmbeddingModelAsyncCustomMessage(region),
				15000,
				TimeUnit.MILLISECONDS,
				1000
		).uid("custom-message-bedrock-async-function");

		OpensearchSink<JSONObject> objectOpensearchSink = new OpensearchSinkBuilder<JSONObject>()
				.setHosts(new HttpHost(osEndpoint, 443, "https"))
				.setEmitter((element, context, indexer) -> indexer.add(createIndexRequest(element, customMessageIndex)))
				.setConnectionUsername(osUser)
				.setConnectionPassword(osPassword)
				.setBulkFlushMaxActions(1)
				.build();

		customMessageEmbedded.sinkTo(objectOpensearchSink);

		// Execute the Flink application
		env.execute("X Flink Bedrock Application");
	}

	public static IndexRequest createIndexRequest(JSONObject element, String index) {
		Map<String, Object> json = new HashMap<>();
		json.put("embeddings", element.getJSONArray("embedding"));
		json.put("@timestamp", element.get("@timestamp"));
		// Add additional fields based on the indexName
		if (index.contains("custom")) {
			json.put("text", element.get("text"));
		} else {
			json.put("tweet", element.get("tweet"));
			json.put("likes", element.get("likes"));
			json.put("retweet_count", element.get("retweet_count"));
			json.put("impression_count", element.get("impression_count"));

		}
		// Create and return an IndexRequest
		return Requests.indexRequest()
				.index(index)
				.id(element.get("_id").toString())
				.source(json, XContentType.JSON);
	}

}
