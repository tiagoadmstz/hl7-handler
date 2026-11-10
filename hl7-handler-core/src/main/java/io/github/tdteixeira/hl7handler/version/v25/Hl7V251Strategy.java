package io.github.tdteixeira.hl7handler.version.v25;

import io.github.tdteixeira.hl7handler.model.Hl7Version;

/**
 * HL7 v2.5.1 strategy.
 */
public class Hl7V251Strategy extends Hl7V25Strategy {

    public Hl7V251Strategy() {
        this(Hl7Version.V2_5_1);
    }

    protected Hl7V251Strategy(Hl7Version version) {
        super(version);
    }
}
