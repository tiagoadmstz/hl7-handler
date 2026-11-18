package io.github.tiagoadmstz.hl7handler.mapping;

import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads external YAML mapping files (De-Para specifications).
 * Malformed YAML, including duplicate keys, is reported as {@link Hl7MappingException};
 * real I/O failures are reported as {@link IOException}.
 */
public class YamlMappingLoader {

    private final YAMLMapper yamlMapper;

    public YamlMappingLoader(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(File file) throws IOException {
        try {
            return yamlMapper.readValue(file, Map.class);
        } catch (JacksonIOException e) {
            throw e.getCause();
        } catch (JacksonException e) {
            throw invalidMapping(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(Path path) throws IOException {
        try {
            return yamlMapper.readValue(path.toFile(), Map.class);
        } catch (JacksonIOException e) {
            throw e.getCause();
        } catch (JacksonException e) {
            throw invalidMapping(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(InputStream inputStream) throws IOException {
        try {
            return yamlMapper.readValue(inputStream, Map.class);
        } catch (JacksonIOException e) {
            throw e.getCause();
        } catch (JacksonException e) {
            throw invalidMapping(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(String yamlContent) throws IOException {
        try {
            return yamlMapper.readValue(yamlContent, Map.class);
        } catch (JacksonIOException e) {
            throw e.getCause();
        } catch (JacksonException e) {
            throw invalidMapping(e);
        }
    }

    private static Hl7MappingException invalidMapping(JacksonException e) {
        return new Hl7MappingException("Invalid YAML mapping: " + e.getMessage(), e);
    }
}
