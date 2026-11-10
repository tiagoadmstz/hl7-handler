package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Version;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry holding parser strategies (SOLID - Open/Closed).
 */
public class Hl7ParserRegistry {

    private static final Hl7ParserRegistry INSTANCE = new Hl7ParserRegistry();

    private final List<Hl7Parser> parsers = new CopyOnWriteArrayList<>();
    private final Hl7Parser fallbackParser = new DefaultHl7Parser();

    private Hl7ParserRegistry() {
        // Register default base parser
        register(fallbackParser);
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
