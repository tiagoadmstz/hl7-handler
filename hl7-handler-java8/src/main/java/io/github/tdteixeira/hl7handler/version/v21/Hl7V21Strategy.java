package io.github.tdteixeira.hl7handler.version.v21;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.AbstractHl7VersionStrategy;

/**
 * HL7 v2.1 strategy. Version data comes from the 2.1 profile.
 */
public class Hl7V21Strategy extends AbstractHl7VersionStrategy {

    public Hl7V21Strategy() {
        this(Hl7Version.V2_1);
    }

    protected Hl7V21Strategy(Hl7Version version) {
        super(version);
    }
}
