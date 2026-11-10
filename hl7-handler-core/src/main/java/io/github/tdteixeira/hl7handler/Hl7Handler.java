package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.api.DefaultHl7Reader;
import io.github.tdteixeira.hl7handler.api.Hl7Reader;
import io.github.tdteixeira.hl7handler.binding.Hl7JsonBinder;
import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.mapping.YamlMappingLoader;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.network.Hl7AckGenerator;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;
import io.github.tdteixeira.hl7handler.parser.DefaultHl7Parser;
import io.github.tdteixeira.hl7handler.parser.Hl7Parser;
import io.github.tdteixeira.hl7handler.parser.Hl7ParserRegistry;
import io.github.tdteixeira.hl7handler.parser.Hl7VersionDetector;
import io.github.tdteixeira.hl7handler.writer.Hl7Writer;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
        YAMLMapper yamlMapper = YAMLMapper.builder().build();
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
     * Builder for isolated Hl7Reader instances without global state side-effects.
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

        public Builder() {}

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

        public Hl7Reader build() {
            YAMLMapper yamlMapper = YAMLMapper.builder().build();
            YamlMappingLoader yamlLoader = new YamlMappingLoader(yamlMapper);
            Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
            Hl7JsonBinder jsonBinder = new Hl7JsonBinder();
            MllpStreamReader mllpReader = new MllpStreamReader(charset);

            Hl7ParserRegistry effectiveRegistry = this.registry != null
                    ? this.registry
                    : new Hl7ParserRegistry(new DefaultHl7Parser(defaultDelimiters));

            DefaultHl7Reader reader = new DefaultHl7Reader(
                effectiveRegistry,
                yamlLoader,
                transformer,
                jsonBinder,
                mllpReader
            );

            if (mappingConfig != null) {
                reader.withMapping(mappingConfig);
            } else if (mappingFile != null) {
                reader.withMapping(mappingFile);
            } else if (mappingPath != null) {
                reader.withMapping(mappingPath);
            } else if (mappingStream != null) {
                reader.withMapping(mappingStream);
            } else if (mappingContent != null) {
                reader.withMappingContent(mappingContent);
            }

            return reader;
        }

        public Hl7Reader reader() {
            return build();
        }
    }
}
