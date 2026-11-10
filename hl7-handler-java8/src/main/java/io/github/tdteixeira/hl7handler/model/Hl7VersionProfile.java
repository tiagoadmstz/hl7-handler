package io.github.tdteixeira.hl7handler.model;

import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Version profile loaded from resources/hl7/profiles/*.properties.
 * Defines date formats, MSH-9 requirements, and MSH-2 encoding rules per HL7 v2 version.
 */
public final class Hl7VersionProfile {

    private static final String[] PROFILE_IDENTIFIERS = {
        "2.1", "2.2", "2.3", "2.3.1", "2.4", "2.5", "2.5.1", "2.6", "2.7", "2.7.1", "2.8"
    };

    private static final NavigableMap<Hl7Version, Hl7VersionProfile> PROFILES = loadProfiles();

    private final Hl7Version version;
    private final String dateType;
    private final String msh9Format;
    private final boolean msh18Supported;
    private final int msh2MinLength;
    private final int msh2MaxLength;
    private final boolean msh2TruncationSupported;

    public Hl7VersionProfile(
            Hl7Version version,
            String dateType,
            String msh9Format,
            boolean msh18Supported,
            int msh2MinLength,
            int msh2MaxLength,
            boolean msh2TruncationSupported
    ) {
        this.version = version != null ? version : Hl7Version.UNKNOWN;
        this.dateType = dateType != null ? dateType : "TS";
        this.msh9Format = msh9Format != null ? msh9Format : "MSG_EVENT";
        this.msh18Supported = msh18Supported;
        this.msh2MinLength = msh2MinLength;
        this.msh2MaxLength = msh2MaxLength;
        this.msh2TruncationSupported = msh2TruncationSupported;
    }

    private static NavigableMap<Hl7Version, Hl7VersionProfile> loadProfiles() {
        NavigableMap<Hl7Version, Hl7VersionProfile> map = new TreeMap<Hl7Version, Hl7VersionProfile>();
        for (String id : PROFILE_IDENTIFIERS) {
            Hl7Version ver = Hl7Version.fromIdentifier(id);
            Hl7VersionProfile profile = loadFromClasspath(id, ver);
            if (profile != null) {
                map.put(ver, profile);
            }
        }
        return Collections.unmodifiableNavigableMap(map);
    }

    private static Hl7VersionProfile loadFromClasspath(String id, Hl7Version ver) {
        String path = "/hl7/profiles/" + id + ".properties";
        InputStream in = Hl7VersionProfile.class.getResourceAsStream(path);
        if (in == null) {
            in = Hl7VersionProfile.class.getClassLoader().getResourceAsStream("hl7/profiles/" + id + ".properties");
        }
        if (in == null) {
            return createDefaultFallback(ver);
        }
        try {
            Properties props = new Properties();
            props.load(in);
            String v = props.getProperty("version", id);
            String dateType = props.getProperty("date.type", "TS");
            String msh9Format = props.getProperty("msh9.format", "MSG_EVENT_STRUCTURE");
            boolean msh18Supported = Boolean.parseBoolean(props.getProperty("msh18.supported", "true"));
            int minLen = Integer.parseInt(props.getProperty("msh2.min_length", "4"));
            int maxLen = Integer.parseInt(props.getProperty("msh2.max_length", "4"));
            boolean trunc = Boolean.parseBoolean(props.getProperty("msh2.truncation_supported", "false"));

            return new Hl7VersionProfile(
                Hl7Version.fromIdentifier(v),
                dateType,
                msh9Format,
                msh18Supported,
                minLen,
                maxLen,
                trunc
            );
        } catch (IOException e) {
            return createDefaultFallback(ver);
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static Hl7VersionProfile createDefaultFallback(Hl7Version ver) {
        boolean dtm = ver.compareTo(Hl7Version.V2_6) >= 0;
        boolean trunc = ver.compareTo(Hl7Version.V2_7) >= 0;
        return new Hl7VersionProfile(
            ver,
            dtm ? "DTM" : "TS",
            "MSG_EVENT_STRUCTURE",
            true,
            4,
            trunc ? 5 : 4,
            trunc
        );
    }

    /**
     * Resolves the profile directly from loaded properties without registry lookup.
     * Used by strategy instances to initialize their profile.
     */
    public static Hl7VersionProfile getDirectProfile(Hl7Version version) {
        if (version == null) {
            return createDefaultFallback(Hl7Version.UNKNOWN);
        }
        Hl7VersionProfile exact = PROFILES.get(version);
        if (exact != null) {
            return exact;
        }
        return createDefaultFallback(version);
    }

    /**
     * Resolves the profile for the given version by delegating to the version strategy.
     * Looks for an exact match; if not found, selects the nearest known lower version (e.g. 2.9 falls back to 2.8).
     */
    public static Hl7VersionProfile profileFor(Hl7Version version) {
        return Hl7VersionStrategyRegistry.findStrategy(version).getProfile();
    }

    public boolean isFallbackFor(Hl7Version requestedVersion) {
        if (requestedVersion == null || requestedVersion.isUnknown()) {
            return true;
        }
        return !this.version.equals(requestedVersion);
    }

    public Hl7Version getVersion() {
        return version;
    }

    public String getDateType() {
        return dateType;
    }

    public String getMsh9Format() {
        return msh9Format;
    }

    public boolean isMsh18Supported() {
        return msh18Supported;
    }

    public int getMsh2MinLength() {
        return msh2MinLength;
    }

    public int getMsh2MaxLength() {
        return msh2MaxLength;
    }

    public boolean isMsh2TruncationSupported() {
        return msh2TruncationSupported;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hl7VersionProfile)) {
            return false;
        }
        Hl7VersionProfile that = (Hl7VersionProfile) o;
        return msh18Supported == that.msh18Supported
                && msh2MinLength == that.msh2MinLength
                && msh2MaxLength == that.msh2MaxLength
                && msh2TruncationSupported == that.msh2TruncationSupported
                && Objects.equals(version, that.version)
                && Objects.equals(dateType, that.dateType)
                && Objects.equals(msh9Format, that.msh9Format);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, dateType, msh9Format, msh18Supported, msh2MinLength, msh2MaxLength, msh2TruncationSupported);
    }

    @Override
    public String toString() {
        return "Hl7VersionProfile[version=" + version.getIdentifier()
                + ", dateType=" + dateType
                + ", msh9Format=" + msh9Format
                + ", msh18Supported=" + msh18Supported
                + ", msh2=" + msh2MinLength + ".." + msh2MaxLength
                + ", truncation=" + msh2TruncationSupported + "]";
    }
}
