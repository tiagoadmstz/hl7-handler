package io.github.tdteixeira.hl7handler.version.v25;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v24.Hl7V24Strategy;

/**
 * HL7 v2.5 strategy: classic baseline version.
 */
public class Hl7V25Strategy extends Hl7V24Strategy {

    public Hl7V25Strategy() {
        this(Hl7Version.V2_5);
    }

    protected Hl7V25Strategy(Hl7Version version) {
        super(version);
    }
}
