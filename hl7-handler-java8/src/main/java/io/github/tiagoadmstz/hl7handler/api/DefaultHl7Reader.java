package io.github.tiagoadmstz.hl7handler.api;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingValidator;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.mapping.YamlMappingLoader;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.network.MllpStreamReader;
import io.github.tiagoadmstz.hl7handler.parser.Hl7ParserRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * Default implementation of the fluent Hl7Reader API. Parsing and mapping are delegated to {@link Hl7Result},
 * which is created once per input and discarded when the input or the mapping changes.
 */
public class DefaultHl7Reader implements Hl7Reader {

    private String rawHl7;
    private Map<String, Object> mappingConfig;
    private Hl7Result result;

    private final Hl7ParserRegistry registry;
    private final YamlMappingLoader yamlLoader;
    private final Hl7ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final MllpStreamReader mllpReader;

    public DefaultHl7Reader(
        Hl7ParserRegistry registry,
        YamlMappingLoader yamlLoader,
        Hl7ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        MllpStreamReader mllpReader
    ) {
        this(registry, yamlLoader, transformer, jsonBinder, mllpReader, null);
    }

    public DefaultHl7Reader(
        Hl7ParserRegistry registry,
        YamlMappingLoader yamlLoader,
        Hl7ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        MllpStreamReader mllpReader,
        Map<String, Object> initialMappingConfig
    ) {
        this.registry = registry;
        this.yamlLoader = yamlLoader;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mllpReader = mllpReader;
        this.mappingConfig = initialMappingConfig;
    }

    @Override
    public Hl7Reader from(String hl7Text) {
        this.rawHl7 = hl7Text;
        this.result = null;
        return this;
    }

    @Override
    public Hl7Reader fromBytes(byte[] bytes) {
        this.rawHl7 = mllpReader.fromBytes(bytes);
        this.result = null;
        return this;
    }

    @Override
    public Hl7Reader fromMllp(InputStream inputStream) {
        try {
            this.rawHl7 = mllpReader.readMessage(inputStream);
        } catch (IOException e) {
            throw new Hl7ParseException("Error reading MLLP byte stream", e);
        }
        this.result = null;
        return this;
    }

    @Override
    public Hl7Reader withMapping(File yamlFile) {
        try {
            return applyMapping(yamlLoader.load(yamlFile));
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading YAML mapping file: " + yamlFile, e);
        }
    }

    @Override
    public Hl7Reader withMapping(Path yamlPath) {
        try {
            return applyMapping(yamlLoader.load(yamlPath));
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading YAML mapping path: " + yamlPath, e);
        }
    }

    @Override
    public Hl7Reader withMapping(InputStream yamlInputStream) {
        try {
            return applyMapping(yamlLoader.load(yamlInputStream));
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading YAML mapping stream", e);
        }
    }

    @Override
    public Hl7Reader withMappingContent(String yamlContent) {
        try {
            return applyMapping(yamlLoader.load(yamlContent));
        } catch (IOException e) {
            throw new Hl7MappingException("Error parsing YAML content", e);
        }
    }

    @Override
    public Hl7Reader withMapping(Map<String, Object> mappingConfig) {
        return applyMapping(mappingConfig);
    }

    private Hl7Reader applyMapping(Map<String, Object> mapping) {
        Hl7MappingValidator.validate(mapping, transformer.getConverterNames());
        this.mappingConfig = mapping;
        this.result = null;
        return this;
    }

    private Hl7Result currentResult() {
        if (result == null) {
            result = new Hl7Result(rawHl7, registry, transformer, jsonBinder, mappingConfig);
        }
        return result;
    }

    @Override
    public Hl7Message toMessage() {
        return currentResult().message();
    }

    @Override
    public String toRawJson() {
        return currentResult().toRawJson();
    }

    @Override
    public String toJson() {
        return currentResult().toJson();
    }

    @Override
    public Map<String, Object> toMap() {
        return Hl7Result.deepCopy(currentResult().toMap(), false);
    }

    @Override
    public <T> T bindTo(Class<T> targetClass) {
        return currentResult().bindTo(targetClass);
    }
}
