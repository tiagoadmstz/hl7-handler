package io.github.tdteixeira.hl7handler.binding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles JSON serialization and Value Object (VO) data binding.
 */
public class Hl7JsonBinder {

    private final JsonMapper jsonMapper;

    public Hl7JsonBinder() {
        this.jsonMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public Hl7JsonBinder(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonMapper getObjectMapper() {
        return jsonMapper;
    }

    /**
     * Converts a semantic Map (e.g. from YAML mapping) to formatted JSON string.
     */
    public String toJson(Map<String, Object> data) {
        try {
            return jsonMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing map to JSON", e);
        }
    }

    /**
     * Converts an Hl7Message to raw structural JSON representation.
     */
    public String toRawJson(Hl7Message message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", message.getVersion().getIdentifier());

        List<Map<String, Object>> segList = new ArrayList<>();
        for (Hl7Segment seg : message.getSegments()) {
            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("segment", seg.getName());

            List<Object> fieldVals = new ArrayList<>();
            seg.getFields().stream()
                .map(Hl7Field::getValue)
                .forEach(fieldVals::add);
            sMap.put("fields", fieldVals);
            segList.add(sMap);
        }
        root.put("segments", segList);

        return toJson(root);
    }

    /**
     * Binds a semantic Map into the client's custom Value Object (VO / DTO / POJO).
     */
    public <T> T bind(Map<String, Object> data, Class<T> targetClass) {
        return jsonMapper.convertValue(data, targetClass);
    }

    /**
     * Deserializes a JSON string into the client's custom Value Object.
     */
    public <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return jsonMapper.readValue(json, targetClass);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing JSON to " + targetClass.getName(), e);
        }
    }
}
