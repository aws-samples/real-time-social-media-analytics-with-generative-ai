package com.amazonaws.services.msf.operators.map;

import com.amazonaws.services.msf.pojo.CustomMessage;
import com.amazonaws.services.msf.pojo.TweetPojo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * Map function for converting a JSON string to a CustomMessage object.
 */
public class CustomMessageMapFunction implements MapFunction<String, CustomMessage> {

    /**
     * Converts a JSON string to a CustomMessage object.
     *
     * @param jsonString The input JSON string representing a CustomMessage.
     * @return CustomMessage object parsed from the input JSON string.
     * @throws Exception Thrown if an error occurs during JSON deserialization.
     */
    @Override
    public CustomMessage map(String jsonString) throws Exception {
        // Initialize ObjectMapper for JSON deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        // Deserialize the JSON string into a CustomMessage object
        return objectMapper.readValue(jsonString, CustomMessage.class);
    }
}
