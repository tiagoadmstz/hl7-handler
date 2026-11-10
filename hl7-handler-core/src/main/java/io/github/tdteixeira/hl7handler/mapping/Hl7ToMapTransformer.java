package io.github.tdteixeira.hl7handler.mapping;

import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforms an Hl7Message into a semantic Map according to the YAML mapping configuration.
 */
public class Hl7ToMapTransformer {

    /**
     * Transforms an HL7 message using the loaded YAML configuration.
     *
     * @param message the parsed HL7 message
     * @param mappingConfig the YAML mapping structure
     * @return structured semantic Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> transform(Hl7Message message, Map<String, Object> mappingConfig) {
        if (message == null || mappingConfig == null) {
            return new LinkedHashMap<>();
        }

        // Check if mappings are nested under a "mappings" key
        Object targetStructure = mappingConfig.getOrDefault("mappings", mappingConfig);
        if (targetStructure instanceof Map<?, ?> targetMap) {
            return processMap(message, (Map<String, Object>) targetMap);
        }

        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processMap(Hl7Message message, Map<String, Object> template) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (val instanceof Map<?, ?> nested) {
                result.put(key, processMap(message, (Map<String, Object>) nested));
            } else if (val instanceof String pathStr) {
                // Leaf node containing HL7 path, e.g. "PID.5.1" or semantic coordinate "@event" / "@patientId"
                String extracted = message.get(pathStr);
                if (extracted.isEmpty()) {
                    if ("@event".equalsIgnoreCase(pathStr) || "EVENT".equalsIgnoreCase(pathStr)) {
                        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(message.getVersion());
                        extracted = message.get(strategy.getDefaultEventCoordinate());
                    } else if ("@patientId".equalsIgnoreCase(pathStr) || "PATIENT_ID".equalsIgnoreCase(pathStr)) {
                        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(message.getVersion());
                        extracted = message.get(strategy.getDefaultPatientIdCoordinate());
                    }
                }
                result.put(key, extracted);
            } else {
                result.put(key, val);
            }
        }

        return result;
    }
}
