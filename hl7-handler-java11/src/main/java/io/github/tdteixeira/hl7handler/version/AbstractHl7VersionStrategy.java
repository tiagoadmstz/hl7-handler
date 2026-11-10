package io.github.tdteixeira.hl7handler.version;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.model.Hl7VersionProfile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Abstract base class for version strategies providing common baseline behavior.
 */
public abstract class AbstractHl7VersionStrategy implements Hl7VersionStrategy {

    protected static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ");
    protected static final DateTimeFormatter DTM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSSZ");

    private final Hl7Version version;
    private final Hl7VersionProfile profile;

    protected AbstractHl7VersionStrategy(Hl7Version version) {
        this.version = version != null ? version : Hl7Version.UNKNOWN;
        this.profile = Hl7VersionProfile.getDirectProfile(this.version);
    }

    @Override
    public Hl7Version getVersion() {
        return version;
    }

    @Override
    public boolean supports(Hl7Version ver) {
        if (ver == null || ver.isUnknown()) {
            return false;
        }
        return Objects.equals(this.version, ver);
    }

    @Override
    public int getMinEncodingCharacters() {
        return 4;
    }

    @Override
    public int getMaxEncodingCharacters() {
        return 4;
    }

    @Override
    public boolean isTruncationSupported() {
        return false;
    }

    @Override
    public boolean isMsh18Supported() {
        return false;
    }

    @Override
    public String formatHeaderTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TS_FORMATTER);
    }

    @Override
    public String getDefaultEventCoordinate() {
        return "EVN.1";
    }

    @Override
    public String getDefaultPatientIdCoordinate() {
        return "PID.2.1";
    }

    @Override
    public Hl7VersionProfile getProfile() {
        return profile;
    }
}
