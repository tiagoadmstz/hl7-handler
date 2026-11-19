package io.github.tiagoadmstz.hl7handler.fhir;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Supported HL7 FHIR releases and detection logic.
 */
public enum FhirVersion {
    R4("4.0.1"),
    R5("5.0.0"),
    UNKNOWN("unknown");

    private final String releaseNumber;

    FhirVersion(String releaseNumber) {
        this.releaseNumber = releaseNumber;
    }

    public String releaseNumber() {
        return releaseNumber;
    }

    public static FhirVersion fromName(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.trim().toUpperCase();
        if ("R4".equals(clean) || "4.0".equals(clean) || "4.0.1".equals(clean) || "R4B".equals(clean)) {
            return R4;
        }
        if ("R5".equals(clean) || "5.0".equals(clean) || "5.0.0".equals(clean)) {
            return R5;
        }
        return UNKNOWN;
    }

    /**
     * Detects the FHIR release from a resource JSON tree.
     */
    public static FhirVersion detect(JsonNode root) {
        if (root == null || !root.isObject()) {
            return UNKNOWN;
        }
        JsonNode meta = root.get("meta");
        if (meta != null && meta.isObject()) {
            JsonNode profile = meta.get("profile");
            if (profile != null && profile.isArray()) {
                for (JsonNode p : profile) {
                    if (p.isTextual()) {
                        String text = p.asText().toUpperCase();
                        if (text.contains("/R5/") || text.contains(".R5.")) {
                            return R5;
                        }
                        if (text.contains("/R4/") || text.contains(".R4.") || text.contains("/R4B/")) {
                            return R4;
                        }
                    }
                }
            }
        }
        JsonNode device = root.get("device");
        if (device != null && device.isObject()) {
            if (device.has("concept") || (device.has("reference") && device.get("reference").isObject())) {
                return R5;
            }
        }
        if (root.has("resourceType")) {
            return R4;
        }
        return UNKNOWN;
    }
}

