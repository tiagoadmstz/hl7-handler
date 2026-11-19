package io.github.tiagoadmstz.hl7handler.v3.writer;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3DirectiveKind;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;

import javax.xml.XMLConstants;
import java.util.Map;

/**
 * Checks, on {@code buildWriter()}, that every XPath of a mapping can be written. A {@code #name} coordinate is
 * resolved through the strategy first, and a failure names the YAML path of the node, so a read-only XPath such as
 * {@code concat(...)} is reported before any document is written.
 */
public final class Hl7V3WriterValidator {

    private Hl7V3WriterValidator() {}

    /**
     * Validates every writable XPath of the template and every list directive.
     *
     * @param template the template of the mapping, as returned by {@code Hl7V3ToMapTransformer.templateOf}
     * @param namespaces the namespace declarations used by the XPath expressions
     * @param strategy the strategy whose semantic coordinates resolve {@code #name} expressions
     * @throws Hl7MappingException with the YAML path of the first node that cannot be written
     */
    public static void validate(Map<String, Object> template, Hl7V3Namespaces namespaces, Hl7V3Strategy strategy) {
        validateTemplate(template, "", namespaces, strategy);
    }

    private static void validateTemplate(Map<?, ?> template, String path, Hl7V3Namespaces namespaces, Hl7V3Strategy strategy) {
        for (Map.Entry<?, ?> entry : template.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String childPath = path.isEmpty() ? key : path + "." + key;
            validateNode(entry.getValue(), childPath, namespaces, strategy);
        }
    }

    private static void validateNode(Object node, String path, Hl7V3Namespaces namespaces, Hl7V3Strategy strategy) {
        if (node instanceof String) {
            checkExpression(String.valueOf(node), path, namespaces, strategy);
            return;
        }
        if (!(node instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        Hl7V3DirectiveKind kind = Hl7V3DirectiveKind.classify(map);
        if (kind == Hl7V3DirectiveKind.LIST) {
            Object spec = map.containsKey("lista") ? map.get("lista") : map.get("list");
            checkListExpression(String.valueOf(spec), path, namespaces, strategy);
            Object item = map.get("item");
            if (item instanceof Map<?, ?>) {
                validateTemplate((Map<?, ?>) item, path + ".item", namespaces, strategy);
            }
            return;
        }
        if (kind == Hl7V3DirectiveKind.CONSTANT) {
            Object constantXpath = xpathOf(map);
            if (constantXpath != null) {
                checkExpression(String.valueOf(constantXpath), path, namespaces, strategy);
            }
            return;
        }
        if (kind == Hl7V3DirectiveKind.COORDINATE) {
            checkExpression(String.valueOf(xpathOf(map)), path, namespaces, strategy);
            return;
        }
        validateTemplate(map, path, namespaces, strategy);
    }

    private static Object xpathOf(Map<?, ?> map) {
        return map.containsKey("xpath") ? map.get("xpath") : map.get("path");
    }

    private static void checkExpression(String expression, String path, Hl7V3Namespaces namespaces, Hl7V3Strategy strategy) {
        parseWritable(expression, path, namespaces, strategy);
    }

    private static void checkListExpression(String expression, String path, Hl7V3Namespaces namespaces, Hl7V3Strategy strategy) {
        Hl7V3WritablePath parsed = parseWritable(expression, path, namespaces, strategy);
        if (parsed.steps().isEmpty()) {
            throw new Hl7MappingException("Invalid writable XPath '" + expression + "' at '" + path
                + "': a lista must address an element below the root element");
        }
        if (parsed.attributeQName() != null || parsed.endsWithText()) {
            throw new Hl7MappingException("Invalid writable XPath '" + expression + "' at '" + path
                + "': a lista must end with an element step");
        }
        if (parsed.steps().get(parsed.steps().size() - 1).position() != 0) {
            throw new Hl7MappingException("Invalid writable XPath '" + expression + "' at '" + path
                + "': a positional predicate is not allowed on the last step of a lista");
        }
    }

    private static Hl7V3WritablePath parseWritable(String expression, String path, Hl7V3Namespaces namespaces,
                                                   Hl7V3Strategy strategy) {
        String resolved = resolve(expression, path, strategy);
        Hl7V3WritablePath parsed;
        try {
            parsed = Hl7V3WritablePath.parse(resolved);
        } catch (IllegalArgumentException e) {
            throw new Hl7MappingException("Invalid writable XPath '" + expression + "' at '" + path + "': "
                + e.getMessage(), e);
        }
        for (Hl7V3WritablePath.Step step : parsed.steps()) {
            checkPrefix(step.prefix(), expression, path, namespaces);
            for (Hl7V3WritablePath.Predicate predicate : step.predicates()) {
                checkPrefix(Hl7V3WritablePath.prefixOf(predicate.attributeQName()), expression, path, namespaces);
            }
        }
        if (parsed.attributeQName() != null) {
            checkPrefix(Hl7V3WritablePath.prefixOf(parsed.attributeQName()), expression, path, namespaces);
        }
        return parsed;
    }

    private static String resolve(String expression, String path, Hl7V3Strategy strategy) {
        String trimmed = expression.trim();
        if (!trimmed.startsWith("#")) {
            return trimmed;
        }
        String resolved = strategy.semanticCoordinates().get(trimmed.substring(1).trim());
        if (resolved == null) {
            throw new Hl7MappingException("Unknown semantic coordinate '" + trimmed + "' at '" + path + "'");
        }
        return resolved;
    }

    private static void checkPrefix(String prefix, String expression, String path, Hl7V3Namespaces namespaces) {
        if (prefix.isEmpty()) {
            return;
        }
        if (XMLConstants.NULL_NS_URI.equals(namespaces.getNamespaceURI(prefix))) {
            throw new Hl7MappingException("Invalid writable XPath '" + expression + "' at '" + path
                + "': unknown namespace prefix '" + prefix + "'");
        }
    }
}
