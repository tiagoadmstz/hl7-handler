package io.github.tiagoadmstz.hl7handler.version.v27;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;

/**
 * HL7 v2.7.1 strategy. Version data comes from the 2.7.1 profile.
 */
public class Hl7V271Strategy extends Hl7V27Strategy {

    public Hl7V271Strategy() {
        this(Hl7Version.V2_7_1);
    }

    protected Hl7V271Strategy(Hl7Version version) {
        super(version);
    }
}
