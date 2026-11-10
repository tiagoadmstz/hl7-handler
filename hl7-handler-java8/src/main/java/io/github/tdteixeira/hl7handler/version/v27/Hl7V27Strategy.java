package io.github.tdteixeira.hl7handler.version.v27;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v26.Hl7V26Strategy;

/**
 * HL7 v2.7 strategy: 5th char truncation '#' supported, max 5 encoding chars.
 */
public class Hl7V27Strategy extends Hl7V26Strategy {

    public Hl7V27Strategy() {
        this(Hl7Version.V2_7);
    }

    protected Hl7V27Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public int getMaxEncodingCharacters() {
        return 5;
    }

    @Override
    public boolean isTruncationSupported() {
        return true;
    }
}
