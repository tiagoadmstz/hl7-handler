package io.github.tiagoadmstz.hl7handler.version.v22;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.version.v21.Hl7V21Strategy;

/**
 * HL7 v2.2 strategy. Version data comes from the 2.2 profile.
 */
public class Hl7V22Strategy extends Hl7V21Strategy {

    public Hl7V22Strategy() {
        this(Hl7Version.V2_2);
    }

    protected Hl7V22Strategy(Hl7Version version) {
        super(version);
    }
}
