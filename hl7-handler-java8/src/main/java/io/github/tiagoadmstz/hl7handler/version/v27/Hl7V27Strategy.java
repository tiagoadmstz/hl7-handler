package io.github.tiagoadmstz.hl7handler.version.v27;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.version.v26.Hl7V26Strategy;

/**
 * HL7 v2.7 strategy. Version data comes from the 2.7 profile (MSH-2 truncation supported).
 */
public class Hl7V27Strategy extends Hl7V26Strategy {

    public Hl7V27Strategy() {
        this(Hl7Version.V2_7);
    }

    protected Hl7V27Strategy(Hl7Version version) {
        super(version);
    }
}
