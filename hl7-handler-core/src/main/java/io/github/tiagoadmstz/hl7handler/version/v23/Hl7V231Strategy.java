package io.github.tiagoadmstz.hl7handler.version.v23;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;

/**
 * HL7 v2.3.1 strategy. Version data comes from the 2.3.1 profile.
 */
public class Hl7V231Strategy extends Hl7V23Strategy {

    public Hl7V231Strategy() {
        this(Hl7Version.V2_3_1);
    }

    protected Hl7V231Strategy(Hl7Version version) {
        super(version);
    }
}
