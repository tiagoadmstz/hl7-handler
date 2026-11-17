package io.github.tiagoadmstz.hl7handler.mapping;

import io.github.tiagoadmstz.hl7handler.model.Hl7Component;
import io.github.tiagoadmstz.hl7handler.model.Hl7Field;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Segment;
import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.version.Hl7VersionStrategy;
import io.github.tiagoadmstz.hl7handler.version.Hl7VersionStrategyRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Transforms an Hl7Message into a semantic Map according to YAML mapping configurations,
 * supporting version constraints, version overlays (porVersao), repeated lists (segments and fields),
 * constant values and typed conversions.
 */
public class Hl7ToMapTransformer {

    static final Pattern HL7_COORDINATE_PATTERN = Pattern.compile("^(@|[A-Za-z][A-Za-z0-9]{1,3}[.-]).*$");

    private final boolean keepEmptyStrings;
    private final Map<String, Hl7TypeConverter> customConverters;

    public Hl7ToMapTransformer() {
        this(false);
    }

    public Hl7ToMapTransformer(boolean keepEmptyStrings) {
        this(keepEmptyStrings, Collections.emptyMap());
    }

    public Hl7ToMapTransformer(boolean keepEmptyStrings, Map<String, Hl7TypeConverter> customConverters) {
        this.keepEmptyStrings = keepEmptyStrings;
        this.customConverters = new ConcurrentHashMap<>(customConverters != null ? customConverters : Collections.emptyMap());
    }

    public boolean isKeepEmptyStrings() {
        return keepEmptyStrings;
    }

    public Hl7ToMapTransformer registerConverter(String name, Hl7TypeConverter converter) {
        if (name != null && converter != null) {
            customConverters.put(name.trim().toLowerCase(Locale.ROOT), converter);
        }
        return this;
    }

    /**
     * Returns the names of the custom converters registered on this transformer (lower-cased, unmodifiable).
     *
     * @return the custom converter names
     */
    public Set<String> getConverterNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(customConverters.keySet()));
    }

    /**
     * Transforms an HL7 message using the loaded YAML configuration.
     *
     * @param message the parsed HL7 message
     * @param mappingConfig the YAML mapping structure
     * @return structured semantic Map
     * @throws Hl7MappingException if version is unsupported or mapping syntax is invalid
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> transform(Hl7Message message, Map<String, Object> mappingConfig) {
        if (message == null || mappingConfig == null) {
            return new LinkedHashMap<>();
        }

        // 1. Validate version range constraints (versoes / versions)
        validateVersionConstraint(message.getVersion(), mappingConfig);

        // 2. Extract base mappings template (ignoring top-level metadata)
        Map<String, Object> baseTemplate = extractBaseTemplate(mappingConfig);

        // 3. Apply porVersao / byVersion overlays (most specific wins)
        applyVersionOverlays(message.getVersion(), mappingConfig, baseTemplate);

        // 4. Transform template against HL7 message
        return processTemplateMap(message, null, 0, 0, baseTemplate);
    }

    static Object versionConstraintOf(Map<String, Object> mappingConfig) {
        Object versoesObj = mappingConfig.get("versoes");
        if (versoesObj == null) {
            versoesObj = mappingConfig.get("versions");
        }
        return versoesObj;
    }

    private void validateVersionConstraint(Hl7Version version, Map<String, Object> mappingConfig) {
        Object versoesObj = versionConstraintOf(mappingConfig);
        if (versoesObj != null) {
            String expr = String.valueOf(versoesObj).trim();
            if (!expr.isEmpty()) {
                Hl7VersionRange range = Hl7VersionRange.parse(expr);
                if (!range.matches(version)) {
                    throw new Hl7MappingException("Message HL7 version '" + version.getIdentifier()
                            + "' does not satisfy supported versions: '" + expr + "'");
                }
            }
        }
    }

    private Map<String, Object> extractBaseTemplate(Map<String, Object> mappingConfig) {
        return deepCopyMap(rawBaseTemplate(mappingConfig));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> rawBaseTemplate(Map<String, Object> mappingConfig) {
        Object mappingsObj = mappingConfig.get("mappings");
        if (mappingsObj instanceof Map<?, ?>) {
            return (Map<String, Object>) mappingsObj;
        }

        Map<String, Object> baseTemplate = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mappingConfig.entrySet()) {
            if (!isTopLevelMetadata(entry.getKey())) {
                baseTemplate.put(entry.getKey(), entry.getValue());
            }
        }
        return baseTemplate;
    }

    static boolean isTopLevelMetadata(String key) {
        if (key == null) {
            return false;
        }
        String clean = key.trim().toLowerCase(Locale.ROOT);
        return clean.equals("version")
                || clean.equals("description")
                || clean.equals("versoes")
                || clean.equals("versions")
                || clean.equals("porversao")
                || clean.equals("byversion")
                || clean.equals("mappings");
    }

    private static final class VersionOverlayRule {
        private final Hl7VersionRange range;
        private final Map<?, ?> overlayMap;

        VersionOverlayRule(Hl7VersionRange range, Map<?, ?> overlayMap) {
            this.range = range;
            this.overlayMap = overlayMap;
        }

        Hl7VersionRange getRange() {
            return range;
        }

        Map<?, ?> getOverlayMap() {
            return overlayMap;
        }
    }

    static Map<?, ?> overlaysOf(Map<String, Object> mappingConfig) {
        Object porVersaoObj = mappingConfig.get("porVersao");
        if (porVersaoObj == null) {
            porVersaoObj = mappingConfig.get("byVersion");
        }
        if (porVersaoObj instanceof Map<?, ?>) {
            return (Map<?, ?>) porVersaoObj;
        }
        return null;
    }

    private void applyVersionOverlays(Hl7Version currentVersion, Map<String, Object> mappingConfig, Map<String, Object> target) {
        Map<?, ?> porVersaoMap = overlaysOf(mappingConfig);
        if (porVersaoMap == null) {
            return;
        }

        List<VersionOverlayRule> matchingRules = new ArrayList<>();

        for (Map.Entry<?, ?> entry : porVersaoMap.entrySet()) {
            String rangeExpr = String.valueOf(entry.getKey()).trim();
            if (entry.getValue() instanceof Map<?, ?>) {
                Hl7VersionRange range = Hl7VersionRange.parse(rangeExpr);
                if (range.matches(currentVersion)) {
                    matchingRules.add(new VersionOverlayRule(range, (Map<?, ?>) entry.getValue()));
                }
            }
        }

        // Sort ascending by specificity (least specific first, most specific last so it overwrites)
        matchingRules.sort(Comparator.comparingInt(r -> r.getRange().getSpecificity()));

        for (VersionOverlayRule rule : matchingRules) {
            deepMerge(target, rule.getOverlayMap());
        }
    }

    @SuppressWarnings("unchecked")
    static void deepMerge(Map<String, Object> target, Map<?, ?> overlay) {
        for (Map.Entry<?, ?> entry : overlay.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object overlayVal = entry.getValue();
            Object targetVal = target.get(key);

            if (targetVal instanceof Map<?, ?> && overlayVal instanceof Map<?, ?>) {
                deepMerge((Map<String, Object>) targetVal, (Map<?, ?>) overlayVal);
            } else {
                target.put(key, deepCopyValue(overlayVal));
            }
        }
    }

    static Map<String, Object> deepCopyMap(Map<?, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return deepCopyMap((Map<?, ?>) value);
        }
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            List<Object> copy = new ArrayList<>(list.size());
            for (Object item : list) {
                copy.add(deepCopyValue(item));
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processTemplateMap(Hl7Message message, Hl7Segment contextSeg, int repField, int repNum, Map<String, Object> template) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            Object evaluated = evaluateNode(message, contextSeg, repField, repNum, val);
            result.put(key, evaluated);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Object evaluateNode(Hl7Message message, Hl7Segment contextSeg, int repField, int repNum, Object node) {
        if (node instanceof Map<?, ?>) {
            Map<String, Object> mapNode = (Map<String, Object>) node;
            Hl7DirectiveKind kind = Hl7DirectiveKind.classify(mapNode);

            // 1. List mapping directive (lista / list)
            if (kind == Hl7DirectiveKind.LIST) {
                Object listSpecObj = mapNode.containsKey("lista") ? mapNode.get("lista") : mapNode.get("list");
                String listSpec = String.valueOf(listSpecObj);
                Object itemTemplate = mapNode.get("item");
                return processList(message, contextSeg, listSpec, itemTemplate);
            }

            // 2. Constant value directive (valor / value)
            if (kind == Hl7DirectiveKind.CONSTANT) {
                Object val = mapNode.containsKey("valor") ? mapNode.get("valor") : mapNode.get("value");
                if (mapNode.containsKey("tipo") || mapNode.containsKey("type")) {
                    String typeName = String.valueOf(mapNode.containsKey("tipo") ? mapNode.get("tipo") : mapNode.get("type"));
                    return applyTypeConversion(typeName, val != null ? String.valueOf(val) : null);
                }
                return val;
            }

            // 3. HL7 coordinate directive (hl7 / path / campo)
            if (kind == Hl7DirectiveKind.COORDINATE) {
                Object coordinateObj;
                if (mapNode.containsKey("hl7")) {
                    coordinateObj = mapNode.get("hl7");
                } else if (mapNode.containsKey("path")) {
                    coordinateObj = mapNode.get("path");
                } else {
                    coordinateObj = mapNode.get("campo");
                }
                String hl7Path = String.valueOf(coordinateObj);
                String raw = resolveHl7Path(message, contextSeg, repField, repNum, hl7Path);

                if ((raw == null || raw.isEmpty()) && (mapNode.containsKey("fallback") || mapNode.containsKey("padrao"))) {
                    Object fallbackObj = mapNode.containsKey("fallback") ? mapNode.get("fallback") : mapNode.get("padrao");
                    raw = resolveFallback(message, contextSeg, repField, repNum, fallbackObj);
                }

                if (mapNode.containsKey("tipo") || mapNode.containsKey("type")) {
                    String typeName = String.valueOf(mapNode.containsKey("tipo") ? mapNode.get("tipo") : mapNode.get("type"));
                    return applyTypeConversion(typeName, raw);
                }

                if (raw == null || raw.isEmpty()) {
                    if (keepEmptyStrings) {
                        return "";
                    }
                    return null;
                }
                return raw;
            }

            // 4. Nested standard Map
            return processTemplateMap(message, contextSeg, repField, repNum, mapNode);
        }

        if (node instanceof String) {
            String pathStr = (String) node;
            String raw = resolveHl7Path(message, contextSeg, repField, repNum, pathStr);
            if (raw == null || raw.isEmpty()) {
                if (keepEmptyStrings) {
                    return "";
                }
                return null;
            }
            return raw;
        }

        return node;
    }

    @SuppressWarnings("unchecked")
    private Object processList(Hl7Message message, Hl7Segment contextSeg, String listSpec, Object itemTemplate) {
        if (listSpec == null || listSpec.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String clean = listSpec.trim().replace('-', '.');

        // Check whether it is a segment list (e.g. "OBX[*]", "OBX") or field repetition list (e.g. "PID.13[*]", "PID.13")
        int dotIdx = clean.indexOf('.');
        if (dotIdx == -1) {
            // Segment repetition
            String segName = clean.replace("[*]", "").replace("[", "").replace("]", "").trim();
            List<Hl7Segment> segments = message.getSegments(segName);
            List<Object> resultList = new ArrayList<>(segments.size());

            for (Hl7Segment seg : segments) {
                if (itemTemplate instanceof Map<?, ?>) {
                    resultList.add(processTemplateMap(message, seg, 0, 0, (Map<String, Object>) itemTemplate));
                } else if (itemTemplate != null) {
                    resultList.add(evaluateNode(message, seg, 0, 0, itemTemplate));
                }
            }
            return resultList;
        } else {
            // Field repetition
            String segPart = clean.substring(0, dotIdx).replace("[*]", "").replace("[", "").replace("]", "").trim();
            String fieldPart = clean.substring(dotIdx + 1).replace("[*]", "").replace("[", "").replace("]", "").trim();

            Hl7Segment seg = (contextSeg != null && contextSeg.getName().equalsIgnoreCase(segPart))
                    ? contextSeg
                    : message.getSegment(segPart).orElse(null);

            if (seg == null) {
                return new ArrayList<>();
            }

            int fieldIndex;
            try {
                fieldIndex = Integer.parseInt(fieldPart);
            } catch (NumberFormatException e) {
                throw new Hl7MappingException("Invalid field index in list directive: '" + listSpec + "'", e);
            }

            Hl7Field field = seg.getField(fieldIndex);
            List<List<Hl7Component>> repetitions = field.getRepetitions();
            List<Object> resultList = new ArrayList<>(repetitions.size());

            for (int r = 1; r <= repetitions.size(); r++) {
                if (itemTemplate instanceof Map<?, ?>) {
                    resultList.add(processTemplateMap(message, seg, fieldIndex, r, (Map<String, Object>) itemTemplate));
                } else if (itemTemplate != null) {
                    resultList.add(evaluateNode(message, seg, fieldIndex, r, itemTemplate));
                }
            }
            return resultList;
        }
    }

    private String resolveHl7Path(Hl7Message message, Hl7Segment contextSeg, int repField, int repNum, String pathStr) {
        if (pathStr == null || pathStr.trim().isEmpty()) {
            return "";
        }
        String clean = pathStr.trim();

        // 1. Semantic coordinates (@event, @patientId)
        if ("@event".equalsIgnoreCase(clean) || "EVENT".equalsIgnoreCase(clean)) {
            Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(message.getVersion());
            return message.get(strategy.getDefaultEventCoordinate());
        }
        if ("@patientId".equalsIgnoreCase(clean) || "PATIENT_ID".equalsIgnoreCase(clean)) {
            Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(message.getVersion());
            return message.get(strategy.getDefaultPatientIdCoordinate());
        }

        String normalized = clean.replace('-', '.');

        // 2. Field repetition context
        if (repField > 0 && repNum > 0 && contextSeg != null) {
            String segName = contextSeg.getName();
            String fieldPrefix = segName + "." + repField;
            String noStar = normalized.replace("[*]", "");

            if (noStar.equalsIgnoreCase(fieldPrefix)) {
                return contextSeg.getFieldValue(repField, repNum);
            }
            if (noStar.toUpperCase(Locale.ROOT).startsWith(fieldPrefix.toUpperCase(Locale.ROOT) + ".")) {
                String sub = noStar.substring(fieldPrefix.length() + 1);
                return extractComponentFromRepetition(contextSeg, repField, repNum, sub);
            }
            if (Character.isDigit(normalized.charAt(0)) && normalized.indexOf('.') == -1) {
                return extractComponentFromRepetition(contextSeg, repField, repNum, normalized);
            }
        }

        // 3. Segment context
        if (contextSeg != null && repField == 0) {
            String segPrefix = contextSeg.getName() + ".";
            String noStar = normalized.replace("[*]", "");

            if (noStar.toUpperCase(Locale.ROOT).startsWith(segPrefix.toUpperCase(Locale.ROOT))) {
                String sub = noStar.substring(segPrefix.length());
                return extractFieldFromSegment(contextSeg, sub);
            }
            if (Character.isDigit(normalized.charAt(0))) {
                return extractFieldFromSegment(contextSeg, normalized);
            }
        }

        // 4. Global message coordinate
        return message.get(normalized);
    }

    private String extractComponentFromRepetition(Hl7Segment seg, int fieldIndex, int repNum, String compPath) {
        try {
            String[] parts = compPath.split("\\.");
            int compIndex = Integer.parseInt(parts[0]);
            if (parts.length == 1) {
                return seg.getComponentValue(fieldIndex, repNum, compIndex);
            }
            int subcompIndex = Integer.parseInt(parts[1]);
            Hl7Field field = seg.getField(fieldIndex);
            List<Hl7Component> comps = field.getComponents(repNum);
            int cIdx = compIndex - 1;
            if (cIdx >= 0 && cIdx < comps.size()) {
                return comps.get(cIdx).getSubcomponent(subcompIndex);
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private String extractFieldFromSegment(Hl7Segment seg, String fieldPath) {
        try {
            String[] parts = fieldPath.split("\\.");
            int fieldIndex = Integer.parseInt(parts[0]);
            if (parts.length == 1) {
                return seg.getFieldValue(fieldIndex);
            }
            int compIndex = Integer.parseInt(parts[1]);
            if (parts.length == 2) {
                return seg.getComponentValue(fieldIndex, compIndex);
            }
            int subcompIndex = Integer.parseInt(parts[2]);
            Hl7Field field = seg.getField(fieldIndex);
            List<Hl7Component> comps = field.getComponents(1);
            int cIdx = compIndex - 1;
            if (cIdx >= 0 && cIdx < comps.size()) {
                return comps.get(cIdx).getSubcomponent(subcompIndex);
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private String resolveFallback(Hl7Message message, Hl7Segment contextSeg, int repField, int repNum, Object fallbackObj) {
        if (fallbackObj == null) {
            return "";
        }
        if (!(fallbackObj instanceof String)) {
            return String.valueOf(fallbackObj);
        }
        String fallbackStr = (String) fallbackObj;
        if (HL7_COORDINATE_PATTERN.matcher(fallbackStr).matches()) {
            String resolved = resolveHl7Path(message, contextSeg, repField, repNum, fallbackStr);
            if (resolved != null && !resolved.isEmpty()) {
                return resolved;
            }
            return "";
        }
        return fallbackStr;
    }

    private Object applyTypeConversion(String typeName, String raw) {
        if (typeName == null || typeName.trim().isEmpty()) {
            if (raw == null || raw.isEmpty()) {
                if (keepEmptyStrings) {
                    return "";
                }
                return null;
            }
            return raw;
        }

        String cleanType = typeName.trim().toLowerCase(Locale.ROOT);
        Hl7TypeConverter converter = customConverters.get(cleanType);
        if (converter == null) {
            converter = Hl7TypeConverter.getConverter(cleanType);
        }
        if (converter == null) {
            throw new Hl7MappingException("Unknown type converter: '" + typeName + "'");
        }

        return converter.convert(raw);
    }
}
