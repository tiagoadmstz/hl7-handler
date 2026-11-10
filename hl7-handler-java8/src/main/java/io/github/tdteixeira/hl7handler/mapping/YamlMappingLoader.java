package io.github.tdteixeira.hl7handler.mapping;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads external YAML mapping files (De-Para specifications).
 */
public class YamlMappingLoader {

    private final YAMLMapper yamlMapper;

    public YamlMappingLoader(YAMLMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(File file) throws IOException {
        return yamlMapper.readValue(file, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(Path path) throws IOException {
        return yamlMapper.readValue(path.toFile(), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(InputStream inputStream) throws IOException {
        return yamlMapper.readValue(inputStream, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> load(String yamlContent) throws IOException {
        return yamlMapper.readValue(yamlContent, Map.class);
    }
}
