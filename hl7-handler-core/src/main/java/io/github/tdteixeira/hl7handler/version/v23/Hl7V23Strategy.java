package io.github.tdteixeira.hl7handler.version.v23;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v22.Hl7V22Strategy;

/**
 * HL7 v2.3 strategy: MSH-18 supported, patient ID in PID.2.1.
 */
public class Hl7V23Strategy extends Hl7V22Strategy {

    public Hl7V23Strategy() {
        this(Hl7Version.V2_3);
    }

    protected Hl7V23Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public boolean isMsh18Supported() {
        return true;
    }

    @Override
    public String getDefaultPatientIdCoordinate() {
        return "PID.2.1";
    }
}
