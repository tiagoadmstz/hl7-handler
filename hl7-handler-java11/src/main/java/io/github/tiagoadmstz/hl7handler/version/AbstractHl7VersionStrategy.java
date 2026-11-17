package io.github.tiagoadmstz.hl7handler.version;

import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.model.Hl7VersionProfile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Base class for version strategies. Every version-specific data value is read from the
 * {@link Hl7VersionProfile} loaded from {@code hl7/profiles/*.properties}; subclasses only add behavior.
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
        return profile.getMsh2MinLength();
    }

    @Override
    public int getMaxEncodingCharacters() {
        return profile.getMsh2MaxLength();
    }

    @Override
    public boolean isTruncationSupported() {
        return profile.isMsh2TruncationSupported();
    }

    @Override
    public boolean isMsh18Supported() {
        return profile.isMsh18Supported();
    }

    /**
     * Builds the ACK MSH-9 from the profile's {@code msh9.format}: "MSG" gives {@code ACK};
     * "MSG_EVENT" gives {@code ACK^event}; any other value gives {@code ACK^event^ACK}.
     * A missing event trigger is replaced by "R01".
     */
    @Override
    public String formatMsh9(String eventTrigger, char componentSeparator) {
        String format = profile.getMsh9Format();
        if ("MSG".equals(format)) {
            return "ACK";
        }
        String event = eventTrigger != null && !eventTrigger.isEmpty() ? eventTrigger : "R01";
        if ("MSG_EVENT".equals(format)) {
            return "ACK" + componentSeparator + event;
        }
        return "ACK" + componentSeparator + event + componentSeparator + "ACK";
    }

    /**
     * Formats MSH-7 as DTM (with microseconds and offset) when the profile's {@code date.type} is "DTM",
     * otherwise as TS. Returns an empty string for a null timestamp.
     */
    @Override
    public String formatHeaderTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        if ("DTM".equals(profile.getDateType())) {
            return dateTime.format(DTM_FORMATTER);
        }
        return dateTime.format(TS_FORMATTER);
    }

    @Override
    public String getDefaultEventCoordinate() {
        return profile.getEventCoordinate();
    }

    @Override
    public String getDefaultPatientIdCoordinate() {
        return profile.getPatientIdCoordinate();
    }

    @Override
    public Hl7VersionProfile getProfile() {
        return profile;
    }
}
