package io.github.tdteixeira.hl7handler.version.v21;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.AbstractHl7VersionStrategy;

/**
 * HL7 v2.1 strategy: MSH-9 is "ACK", EVN.1 event fallback, no MSH-18, TS format.
 */
public class Hl7V21Strategy extends AbstractHl7VersionStrategy {

    public Hl7V21Strategy() {
        this(Hl7Version.V2_1);
    }

    protected Hl7V21Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public boolean isMsh18Supported() {
        return false;
    }

    @Override
    public String formatMsh9(String eventTrigger, char componentSeparator) {
        return "ACK";
    }

    @Override
    public String getDefaultEventCoordinate() {
        return "EVN.1";
    }

    @Override
    public String getDefaultPatientIdCoordinate() {
        return "PID.2.1";
    }
}
