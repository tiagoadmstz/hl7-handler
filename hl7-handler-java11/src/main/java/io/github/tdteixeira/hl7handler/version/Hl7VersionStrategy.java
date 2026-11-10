package io.github.tdteixeira.hl7handler.version;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.model.Hl7VersionProfile;

import java.time.OffsetDateTime;

/**
 * Strategy interface defining HL7 v2 version-specific behavior and rules.
 */
public interface Hl7VersionStrategy {

    /**
     * Returns the canonical HL7 version represented by this strategy.
     *
     * @return the canonical version
     */
    Hl7Version getVersion();

    /**
     * Returns whether this strategy supports the given HL7 version (exact or fallback).
     *
     * @param version the HL7 version to check
     * @return true if supported, false otherwise
     */
    boolean supports(Hl7Version version);

    /**
     * Minimum length of MSH-2 encoding characters.
     *
     * @return minimum length
     */
    int getMinEncodingCharacters();

    /**
     * Maximum length of MSH-2 encoding characters.
     *
     * @return maximum length
     */
    int getMaxEncodingCharacters();

    /**
     * Indicates whether the 5th truncation character ('#') is supported.
     *
     * @return true if truncation is supported
     */
    boolean isTruncationSupported();

    /**
     * Indicates whether MSH-18 (Character Set) is supported in this version.
     *
     * @return true if MSH-18 is supported
     */
    boolean isMsh18Supported();

    /**
     * Formats the MSH-9 message type field for an ACK message in this version.
     *
     * @param eventTrigger the trigger event (e.g. "A01", "R01")
     * @param componentSeparator the component separator delimiter
     * @return the formatted MSH-9 string
     */
    String formatMsh9(String eventTrigger, char componentSeparator);

    /**
     * Formats a timestamp for MSH-7 according to this version's date type (TS or DTM).
     *
     * @param dateTime the timestamp to format
     * @return formatted timestamp string
     */
    String formatHeaderTimestamp(OffsetDateTime dateTime);

    /**
     * Default coordinate where the event trigger is located ("EVN.1" in v2.1, "MSH.9.2" in v2.2+).
     *
     * @return the default event coordinate
     */
    String getDefaultEventCoordinate();

    /**
     * Default coordinate where the patient identifier is located ("PID.2.1" before v2.3.1, "PID.3.1" in v2.3.1+).
     *
     * @return the default patient ID coordinate
     */
    String getDefaultPatientIdCoordinate();

    /**
     * Returns the version profile associated with this strategy.
     *
     * @return the version profile
     */
    Hl7VersionProfile getProfile();
}
