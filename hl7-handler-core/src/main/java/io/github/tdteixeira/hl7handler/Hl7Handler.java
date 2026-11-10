package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.api.DefaultHl7Reader;
import io.github.tdteixeira.hl7handler.api.Hl7Reader;
import io.github.tdteixeira.hl7handler.binding.Hl7JsonBinder;
import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.mapping.YamlMappingLoader;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;
import io.github.tdteixeira.hl7handler.parser.Hl7ParserRegistry;
import io.github.tdteixeira.hl7handler.parser.Hl7VersionDetector;
import io.github.tdteixeira.hl7handler.writer.Hl7Writer;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
     * Creates a new fluent reader instance.
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
}
