package io.github.tiagoadmstz.hl7handler.fhir;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard mapping from IEEE 11073-10101 (MDC) dimension codes to Unified Code for Units of Measure (UCUM).
 */
public final class FhirUnits {

    private static final Map<String, String> MDC_TO_UCUM;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        // Percent
        map.put("MDC_DIM_PERCENT", "%");
        map.put("262688", "%");
        // Rates
        map.put("MDC_DIM_BEAT_PER_MIN", "/min");
        map.put("264864", "/min");
        map.put("MDC_DIM_PULS_RATE_PER_MIN", "/min");
        map.put("MDC_DIM_RESP_PER_MIN", "/min");
        map.put("264992", "/min");
        map.put("MDC_DIM_PER_MIN", "/min");
        map.put("264832", "/min");
        // Pressures
        map.put("MDC_DIM_MMHG", "mm[Hg]");
        map.put("266016", "mm[Hg]");
        map.put("MDC_DIM_KPA", "kPa");
        map.put("266048", "kPa");
        map.put("MDC_DIM_BAR", "bar");
        map.put("MDC_DIM_MILLI_BAR", "mbar");
        // Temperatures
        map.put("MDC_DIM_DEGC", "Cel");
        map.put("268192", "Cel");
        map.put("MDC_DIM_CEL", "Cel");
        map.put("MDC_DIM_FAHR", "[degF]");
        map.put("268224", "[degF]");
        map.put("MDC_DIM_KELVIN", "K");
        // Electrical
        map.put("MDC_DIM_MILLI_VOLT", "mV");
        map.put("266432", "mV");
        map.put("MDC_DIM_MICRO_VOLT", "uV");
        map.put("266400", "uV");
        map.put("MDC_DIM_VOLT", "V");
        // Time
        map.put("MDC_DIM_SEC", "s");
        map.put("264320", "s");
        map.put("MDC_DIM_MILLI_SEC", "ms");
        map.put("264352", "ms");
        map.put("MDC_DIM_MIN", "min");
        map.put("MDC_DIM_HR", "h");
        map.put("MDC_DIM_DAY", "d");
        // Mass / Weight
        map.put("MDC_DIM_KG", "kg");
        map.put("263872", "kg");
        map.put("MDC_DIM_G", "g");
        map.put("263840", "g");
        map.put("MDC_DIM_MILLI_G", "mg");
        map.put("MDC_DIM_MICRO_G", "ug");
        // Length / Distance
        map.put("MDC_DIM_CENTI_M", "cm");
        map.put("263456", "cm");
        map.put("MDC_DIM_M", "m");
        map.put("263424", "m");
        map.put("MDC_DIM_MILLI_M", "mm");
        map.put("MDC_DIM_INCH", "[in_i]");
        // Volume / Flow
        map.put("MDC_DIM_L", "L");
        map.put("263744", "L");
        map.put("MDC_DIM_MILLI_L", "mL");
        map.put("263776", "mL");
        map.put("MDC_DIM_L_PER_MIN", "L/min");
        map.put("265216", "L/min");
        map.put("MDC_DIM_MILLI_L_PER_MIN", "mL/min");
        map.put("265248", "mL/min");
        MDC_TO_UCUM = Collections.unmodifiableMap(map);
    }

    private FhirUnits() {
    }

    public static String toUcum(String mdcCodeOrName) {
        if (mdcCodeOrName == null || mdcCodeOrName.trim().isEmpty()) {
            return null;
        }
        return MDC_TO_UCUM.get(mdcCodeOrName.trim());
    }

    public static boolean hasUcumEquivalence(String mdcCodeOrName) {
        return toUcum(mdcCodeOrName) != null;
    }
}
