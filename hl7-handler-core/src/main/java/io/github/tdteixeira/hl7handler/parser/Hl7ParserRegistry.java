package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Version;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry holding parser strategies (SOLID - Open/Closed).
 * Can be used as a global singleton or instantiated per builder for test/pipeline isolation.
 */
public class Hl7ParserRegistry {

    private static final Hl7ParserRegistry INSTANCE = new Hl7ParserRegistry();

    private final List<Hl7Parser> parsers = new CopyOnWriteArrayList<>();
    private final Hl7Parser fallbackParser;

    public Hl7ParserRegistry() {
        this(new DefaultHl7Parser());
    }

    public Hl7ParserRegistry(Hl7Parser fallbackParser) {
        this.fallbackParser = fallbackParser != null ? fallbackParser : new DefaultHl7Parser();
        register(this.fallbackParser);
    }

    public static Hl7ParserRegistry getInstance() {
        return INSTANCE;
    }

    public void register(Hl7Parser parser) {
        if (parser != null) {
            parsers.add(0, parser); // Newest/custom parsers take precedence
        }
    }

    public Hl7Parser getParserFor(Hl7Version version) {
        for (Hl7Parser p : parsers) {
            if (p.supports(version)) {
                return p;
            }
        }
        return fallbackParser;
    }
}
