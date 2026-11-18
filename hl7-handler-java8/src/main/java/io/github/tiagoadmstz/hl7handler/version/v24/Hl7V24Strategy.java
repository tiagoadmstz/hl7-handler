package io.github.tiagoadmstz.hl7handler.version.v24;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.version.v23.Hl7V231Strategy;

/**
 * HL7 v2.4 strategy. Version data comes from the 2.4 profile.
 */
public class Hl7V24Strategy extends Hl7V231Strategy {

    public Hl7V24Strategy() {
        this(Hl7Version.V2_4);
    }

    protected Hl7V24Strategy(Hl7Version version) {
        super(version);
    }
}
