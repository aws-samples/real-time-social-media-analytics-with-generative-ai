package com.amazonaws.services.msf.operators.map;

import com.amazonaws.services.msf.pojo.CustomMessage;
import org.apache.flink.api.common.functions.MapFunction;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.UUID;

/**
 * Map function for converting a CustomMessage object to a JSONObject.
 */
public class CustomMessageToJSONObject implements MapFunction<CustomMessage, JSONObject> {

    /**
     * Converts a CustomMessage object to a JSONObject.
     *
     * @param customMessage The input CustomMessage object.
     * @return JSONObject representing the mapped fields of the CustomMessage.
     * @throws ParseException Thrown if there is an issue with parsing or formatting dates.
     */
    @Override
    public JSONObject map(CustomMessage customMessage) throws ParseException {
        // Generate a random UUID for the "_id" field
        UUID uuid = UUID.randomUUID();

        // Create a new JSONObject to store mapped fields from CustomMessage
        JSONObject result = new JSONObject();
        // Map "text" field from CustomMessage to "text" field in JSONObject
        result.put("text", customMessage.getText());
        // Map "created_at" field from CustomMessage to "created_at" field in JSONObject
        result.put("created_at", customMessage.getCreated_at());
        // Map generated UUID to the "_id" field in JSONObject
        result.put("_id", uuid.toString());

        // Return the final JSONObject
        return result;
    }
}
