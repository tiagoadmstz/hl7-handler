package io.github.tdteixeira.hl7handler.version.v28;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v27.Hl7V271Strategy;

import java.util.Objects;

/**
 * HL7 v2.8 strategy: supports 2.8, serves as fallback for 2.8.x and 2.9.
 */
public class Hl7V28Strategy extends Hl7V271Strategy {

    public Hl7V28Strategy() {
        this(Hl7Version.V2_8);
    }

    protected Hl7V28Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public boolean supports(Hl7Version version) {
        if (version == null || version.isUnknown()) {
            return false;
        }
        return Objects.equals(getVersion(), version) || version.compareTo(Hl7Version.V2_8) >= 0;
    }
}
