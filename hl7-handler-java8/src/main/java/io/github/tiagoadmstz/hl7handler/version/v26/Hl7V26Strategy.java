package io.github.tiagoadmstz.hl7handler.version.v26;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.version.v25.Hl7V251Strategy;

/**
 * HL7 v2.6 strategy. Version data comes from the 2.6 profile (DTM timestamps).
 */
public class Hl7V26Strategy extends Hl7V251Strategy {

    public Hl7V26Strategy() {
        this(Hl7Version.V2_6);
    }

    protected Hl7V26Strategy(Hl7Version version) {
        super(version);
    }
}
