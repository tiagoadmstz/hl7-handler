package io.github.tiagoadmstz.hl7handler.fhir.parser;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.fhir.FhirResource;
import io.github.tiagoadmstz.hl7handler.fhir.FhirVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parses and validates raw FHIR JSON inputs into {@link FhirResource}.
 */
public final class FhirParser {

    private static final String EMPTY_INPUT_MESSAGE = "FHIR input content must not be null or blank";
    private static final String INVALID_JSON_MESSAGE = "FHIR content must be a valid JSON object with 'resourceType'";

    private final JsonMapper jsonMapper;
    private final FhirVersion defaultVersion;

    public FhirParser(JsonMapper jsonMapper, FhirVersion defaultVersion) {
        this.jsonMapper = jsonMapper != null ? jsonMapper : JsonMapper.builder().build();
        this.defaultVersion = defaultVersion;
    }

    public FhirResource parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        try {
            JsonNode root = jsonMapper.readTree(json);
            return toResource(root);
        } catch (IOException e) {
            throw new Hl7ParseException("Malformed FHIR JSON: " + e.getMessage(), e);
        }
    }

    public FhirResource parse(InputStream in) {
        if (in == null) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        try {
            JsonNode root = jsonMapper.readTree(in);
            return toResource(root);
        } catch (IOException e) {
            throw new Hl7ParseException("Malformed FHIR JSON: " + e.getMessage(), e);
        }
    }

    public FhirResource parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new Hl7ParseException(EMPTY_INPUT_MESSAGE);
        }
        return parse(new ByteArrayInputStream(bytes));
    }

    private FhirResource toResource(JsonNode root) {
        if (root == null || !root.isObject() || !root.hasNonNull("resourceType")) {
            throw new Hl7ParseException(INVALID_JSON_MESSAGE);
        }
        FhirVersion detected = FhirVersion.detect(root);
        FhirVersion resolved = (defaultVersion != null && defaultVersion != FhirVersion.UNKNOWN)
            ? defaultVersion
            : detected;
        return new FhirResource(root, resolved, jsonMapper);
    }
}

