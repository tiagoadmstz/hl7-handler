package io.github.tiagoadmstz.hl7handler.v3.version;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for HL7 v3 strategies. It keeps the document format and an unmodifiable copy of the semantic
 * coordinates; subclasses add the detection, version and validation rules.
 */
public abstract class AbstractHl7V3Strategy implements Hl7V3Strategy {

    private final Hl7V3Format format;
    private final Map<String, String> semanticCoordinates;

    /**
     * Creates the base strategy.
     *
     * @param format the document format handled by the strategy
     * @param semanticCoordinates name to XPath map; copied, {@code null} means none
     */
    protected AbstractHl7V3Strategy(Hl7V3Format format, Map<String, String> semanticCoordinates) {
        this.format = format;
        Map<String, String> copy = new LinkedHashMap<>();
        if (semanticCoordinates != null) {
            copy.putAll(semanticCoordinates);
        }
        this.semanticCoordinates = Collections.unmodifiableMap(copy);
    }

    @Override
    public Hl7V3Format format() {
        return format;
    }

    @Override
    public Map<String, String> semanticCoordinates() {
        return semanticCoordinates;
    }
}
