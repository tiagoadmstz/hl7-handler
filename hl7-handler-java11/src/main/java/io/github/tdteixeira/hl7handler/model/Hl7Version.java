package io.github.tdteixeira.hl7handler.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Open, immutable and comparable HL7 v2 version representation.
 */
public final class Hl7Version implements Comparable<Hl7Version> {

    public static final Hl7Version V2_1 = new Hl7Version(2, 1, 0, "2.1");
    public static final Hl7Version V2_2 = new Hl7Version(2, 2, 0, "2.2");
    public static final Hl7Version V2_3 = new Hl7Version(2, 3, 0, "2.3");
    public static final Hl7Version V2_3_1 = new Hl7Version(2, 3, 1, "2.3.1");
    public static final Hl7Version V2_4 = new Hl7Version(2, 4, 0, "2.4");
    public static final Hl7Version V2_5 = new Hl7Version(2, 5, 0, "2.5");
    public static final Hl7Version V2_5_1 = new Hl7Version(2, 5, 1, "2.5.1");
    public static final Hl7Version V2_6 = new Hl7Version(2, 6, 0, "2.6");
    public static final Hl7Version V2_7 = new Hl7Version(2, 7, 0, "2.7");
    public static final Hl7Version V2_7_1 = new Hl7Version(2, 7, 1, "2.7.1");
    public static final Hl7Version V2_8 = new Hl7Version(2, 8, 0, "2.8");
    public static final Hl7Version V2_8_1 = new Hl7Version(2, 8, 1, "2.8.1");
    public static final Hl7Version V2_8_2 = new Hl7Version(2, 8, 2, "2.8.2");
    public static final Hl7Version V2_9 = new Hl7Version(2, 9, 0, "2.9");
    public static final Hl7Version UNKNOWN = new Hl7Version(0, 0, 0, "UNKNOWN");

    private static final Hl7Version[] KNOWN_VERSIONS = {
        V2_1, V2_2, V2_3, V2_3_1, V2_4, V2_5, V2_5_1, V2_6, V2_7, V2_7_1, V2_8, V2_8_1, V2_8_2, V2_9, UNKNOWN
    };

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String raw;

    public Hl7Version(int major, int minor, int patch, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.raw = raw != null ? raw : ((major == 0 && minor == 0 && patch == 0) ? "UNKNOWN" : (major + "." + minor + (patch > 0 ? "." + patch : "")));
    }

    public static Hl7Version[] values() {
        return KNOWN_VERSIONS.clone();
    }

    public static Hl7Version fromIdentifier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNKNOWN;
        }
        String clean = value.trim();
        if ("UNKNOWN".equalsIgnoreCase(clean)) {
            return UNKNOWN;
        }

        for (Hl7Version known : KNOWN_VERSIONS) {
            if (known.getIdentifier().equalsIgnoreCase(clean)) {
                return known;
            }
        }

        Matcher matcher = VERSION_PATTERN.matcher(clean);
        if (matcher.matches()) {
            int maj = Integer.parseInt(matcher.group(1));
            int min = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int pat = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new Hl7Version(maj, min, pat, clean);
        }

        return UNKNOWN;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String raw() {
        return raw;
    }

    public String getIdentifier() {
        return raw;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getRaw() {
        return raw;
    }

    public boolean isUnknown() {
        return this == UNKNOWN || "UNKNOWN".equalsIgnoreCase(raw) || (major == 0 && minor == 0 && patch == 0);
    }

    @Override
    public int compareTo(Hl7Version other) {
        if (other == null) {
            return 1;
        }
        if (this.isUnknown() && other.isUnknown()) {
            return 0;
        }
        if (this.isUnknown()) {
            return -1;
        }
        if (other.isUnknown()) {
            return 1;
        }
        int cmp = Integer.compare(this.major, other.major);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(this.minor, other.minor);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hl7Version)) {
            return false;
        }
        Hl7Version that = (Hl7Version) o;
        if (this.isUnknown() || that.isUnknown()) {
            return this.isUnknown() && that.isUnknown();
        }
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        if (isUnknown()) {
            return Objects.hash(0, 0, 0);
        }
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return raw;
    }
}
