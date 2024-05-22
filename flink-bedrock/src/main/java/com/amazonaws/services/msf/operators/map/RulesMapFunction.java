package com.amazonaws.services.msf.operators.map;

import com.amazonaws.services.msf.pojo.Rules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.MapFunction;

/**
 * Map function for converting a JSON string to a Rules object.
 */
public class RulesMapFunction implements MapFunction<String, Rules> {

    /**
     * Converts a JSON string to a Rules object.
     *
     * @param jsonString The input JSON string representing Rules.
     * @return Rules object parsed from the input JSON string.
     * @throws Exception Thrown if an error occurs during JSON deserialization.
     */
    @Override
    public Rules map(String jsonString) throws Exception {
        // Initialize ObjectMapper for JSON deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        // Deserialize the JSON string into a Rules object
        return objectMapper.readValue(jsonString, Rules.class);
    }
}
