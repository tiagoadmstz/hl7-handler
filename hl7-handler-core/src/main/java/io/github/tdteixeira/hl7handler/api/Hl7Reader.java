package io.github.tdteixeira.hl7handler.api;

import io.github.tdteixeira.hl7handler.model.Hl7Message;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Public fluent API interface for reading and transforming HL7 messages.
 */
public interface Hl7Reader {

    /**
     * Sets the input HL7 content from a raw String.
     */
    Hl7Reader from(String hl7Text);

    /**
     * Sets the input HL7 content from a byte array (with or without MLLP framing).
     */
    Hl7Reader fromBytes(byte[] bytes);

    /**
     * Sets the input HL7 content by reading an MLLP message from an InputStream.
     */
    Hl7Reader fromMllp(InputStream inputStream);

    /**
     * Configures an external YAML mapping file (De-Para) for semantic translation.
     */
    Hl7Reader withMapping(File yamlFile);

    /**
     * Configures an external YAML mapping file from Path.
     */
    Hl7Reader withMapping(Path yamlPath);

    /**
     * Configures an external YAML mapping from InputStream.
     */
    Hl7Reader withMapping(InputStream yamlInputStream);

    /**
     * Configures an external YAML mapping from String content.
     */
    Hl7Reader withMappingContent(String yamlContent);

    /**
     * Parses and returns the full structured Hl7Message domain model.
     */
    Hl7Message toMessage();

    /**
     * Transforms and returns raw structural JSON representation.
     */
    String toRawJson();

    /**
     * Transforms the message according to the YAML mapping and returns formatted JSON.
     * If no YAML mapping was specified, returns the structural JSON.
     */
    String toJson();

    /**
     * Transforms the message according to the YAML mapping and returns a Map.
     */
    Map<String, Object> toMap();

    /**
     * Transforms the message and binds the result into the client's custom Value Object (VO).
     */
    <T> T bindTo(Class<T> targetClass);
}
