package io.github.tiagoadmstz.hl7handler;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.tiagoadmstz.hl7handler.api.DefaultHl7Reader;
import io.github.tiagoadmstz.hl7handler.api.Hl7Processor;
import io.github.tiagoadmstz.hl7handler.api.Hl7Reader;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingValidator;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.mapping.YamlMappingLoader;
import io.github.tiagoadmstz.hl7handler.model.Hl7Delimiters;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.network.Hl7AckGenerator;
import io.github.tiagoadmstz.hl7handler.network.MllpStreamReader;
import io.github.tiagoadmstz.hl7handler.parser.DefaultHl7Parser;
import io.github.tiagoadmstz.hl7handler.parser.Hl7Parser;
import io.github.tiagoadmstz.hl7handler.parser.Hl7ParserRegistry;
import io.github.tiagoadmstz.hl7handler.parser.Hl7VersionDetector;
import io.github.tiagoadmstz.hl7handler.writer.Hl7Writer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Main entry point and Fluent API for the HL7 Handler library.
 *
 * <pre>{@code
 * // Read HL7 and get JSON translated by external YAML
 * String json = Hl7Handler.reader()
 *     .from(rawHl7)
 *     .withMapping(new File("/path/to/mapping.yaml"))
 *     .toJson();
 *
 * // Bind directly to customer VO
 * PatientVO patient = Hl7Handler.reader()
 *     .from(rawHl7)
 *     .withMapping(new File("/path/to/mapping.yaml"))
 *     .bindTo(PatientVO.class);
 * }</pre>
 */
public final class Hl7Handler {

    private Hl7Handler() {}

    /**
     * Creates a new fluent reader instance backed by the default global parser registry.
     */
    public static Hl7Reader reader() {
        YAMLMapper yamlMapper = YAMLMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
        return new DefaultHl7Reader(
            Hl7ParserRegistry.getInstance(),
            new YamlMappingLoader(yamlMapper),
            new Hl7ToMapTransformer(),
            new Hl7JsonBinder(),
            new MllpStreamReader()
        );
    }

    /**
     * Creates a new Builder to configure an isolated HL7 processing pipeline.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new default HL7 serializer.
     */
    public static Hl7Writer writer() {
        return new Hl7Writer();
    }

    /**
     * Creates an MLLP network byte stream utility with UTF-8 encoding.
     */
    public static MllpStreamReader network() {
        return new MllpStreamReader(StandardCharsets.UTF_8);
    }

    /**
     * Creates an MLLP network byte stream utility with custom charset.
     */
    public static MllpStreamReader network(Charset charset) {
        return new MllpStreamReader(charset);
    }

    /**
     * Quickly detects the version of a raw HL7 message string without full parsing.
     */
    public static Hl7Version detectVersion(String rawHl7) {
        return Hl7VersionDetector.detect(rawHl7);
    }

    /**
     * Converts an Hl7Message domain model directly to standard HL7 v2 text.
     */
    public static String toHl7(Hl7Message message) {
        return writer().write(message);
    }

    /**
     * Generates a version-aware ACK (AA) response message for an incoming message.
     */
    public static String generateAck(Hl7Message message) {
        return Hl7AckGenerator.generateAck(message);
    }

    /**
     * Generates a version-aware ACK response message with explicit code and message text.
     */
    public static String generateAck(Hl7Message message, String ackCode, String textMessage) {
        return Hl7AckGenerator.generateAck(message, ackCode, textMessage);
    }

    /**
     * Builder for isolated Hl7Processor and Hl7Reader instances without global state side-effects.
     */
    public static final class Builder {
        private Hl7ParserRegistry registry;
        private Charset charset = StandardCharsets.UTF_8;
        private Hl7Delimiters defaultDelimiters = Hl7Delimiters.DEFAULT;
        private Map<String, Object> mappingConfig;
        private File mappingFile;
        private Path mappingPath;
        private InputStream mappingStream;
        private String mappingContent;
        private boolean keepEmptyStrings;
        private final Map<String, Hl7TypeConverter> converters = new LinkedHashMap<>();

        public Builder() {}

        /**
         * Keeps empty HL7 values as empty strings instead of null in the mapped output.
         */
        public Builder keepEmptyStrings(boolean keepEmptyStrings) {
            this.keepEmptyStrings = keepEmptyStrings;
            return this;
        }

        /**
         * Registers a custom type converter usable as {@code tipo} in mapping YAML. Ignored when the name or converter is null or blank.
         */
        public Builder converter(String name, Hl7TypeConverter converter) {
            if (name != null && !name.trim().isEmpty() && converter != null) {
                converters.put(name.trim().toLowerCase(Locale.ROOT), converter);
            }
            return this;
        }

        public Builder parserRegistry(Hl7ParserRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder parser(Hl7Parser parser) {
            if (parser != null) {
                if (this.registry == null) {
                    this.registry = new Hl7ParserRegistry(new DefaultHl7Parser(defaultDelimiters));
                }
                this.registry.register(parser);
            }
            return this;
        }

        public Builder charset(Charset charset) {
            if (charset != null) {
                this.charset = charset;
            }
            return this;
        }

        public Builder defaultDelimiters(Hl7Delimiters delimiters) {
            if (delimiters != null) {
                this.defaultDelimiters = delimiters;
            }
            return this;
        }

        public Builder withMapping(File file) {
            this.mappingFile = file;
            return this;
        }

        public Builder withMapping(Path path) {
            this.mappingPath = path;
            return this;
        }

        public Builder withMapping(InputStream inputStream) {
            this.mappingStream = inputStream;
            return this;
        }

        public Builder withMappingContent(String content) {
            this.mappingContent = content;
            return this;
        }

        public Builder withMapping(Map<String, Object> mapping) {
            this.mappingConfig = mapping;
            return this;
        }

        public Hl7Processor build() {
            Hl7ToMapTransformer transformer = newTransformer();
            Map<String, Object> mapping = loadMapping(newYamlLoader(), transformer);
            return new Hl7Processor(
                effectiveRegistry(),
                transformer,
                new Hl7JsonBinder(),
                new MllpStreamReader(charset),
                mapping
            );
        }

        public Hl7Reader reader() {
            YamlMappingLoader yamlLoader = newYamlLoader();
            Hl7ToMapTransformer transformer = newTransformer();
            return new DefaultHl7Reader(
                effectiveRegistry(),
                yamlLoader,
                transformer,
                new Hl7JsonBinder(),
                new MllpStreamReader(charset),
                loadMapping(yamlLoader, transformer)
            );
        }

        private YamlMappingLoader newYamlLoader() {
            YAMLMapper yamlMapper = YAMLMapper.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
            return new YamlMappingLoader(yamlMapper);
        }

        private Hl7ToMapTransformer newTransformer() {
            return new Hl7ToMapTransformer(keepEmptyStrings, converters);
        }

        private Hl7ParserRegistry effectiveRegistry() {
            return this.registry != null
                    ? this.registry
                    : new Hl7ParserRegistry(new DefaultHl7Parser(defaultDelimiters));
        }

        private Map<String, Object> loadMapping(YamlMappingLoader yamlLoader, Hl7ToMapTransformer transformer) {
            Map<String, Object> mapping = readMappingSource(yamlLoader);
            if (mapping != null) {
                Hl7MappingValidator.validate(mapping, transformer.getConverterNames());
            }
            return mapping;
        }

        private Map<String, Object> readMappingSource(YamlMappingLoader yamlLoader) {
            if (mappingConfig != null) {
                return mappingConfig;
            }
            try {
                if (mappingFile != null) {
                    return yamlLoader.load(mappingFile);
                }
                if (mappingPath != null) {
                    return yamlLoader.load(mappingPath);
                }
                if (mappingStream != null) {
                    return yamlLoader.load(mappingStream);
                }
                if (mappingContent != null) {
                    return yamlLoader.load(mappingContent);
                }
            } catch (IOException e) {
                throw new Hl7MappingException("Error reading YAML mapping", e);
            }
            return null;
        }
    }
}
