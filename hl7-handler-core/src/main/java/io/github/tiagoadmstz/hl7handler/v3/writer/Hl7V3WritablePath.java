package io.github.tiagoadmstz.hl7handler.v3.writer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsed form of the XPath subset that the HL7 v3 writer can create. A path is a list of element steps separated by
 * {@code /}, optionally ending in an attribute ({@code @attr} or {@code @prefix:attr}) or in {@code text()}. A step is
 * {@code prefix:name} or {@code name}, with zero or more {@code [@attr='literal']} predicates and at most one positional
 * {@code [n]}. Paths are relative to the root element. Axes, wildcards, unions, parent steps, descendant steps and
 * function calls are rejected, because the writer cannot create a node that they select.
 */
public final class Hl7V3WritablePath {

    private static final String NC_NAME = "[\\p{L}_][\\p{L}\\p{N}_.\\-]*";
    private static final Pattern QNAME = Pattern.compile("(?:" + NC_NAME + ":)?" + NC_NAME);
    private static final Pattern POSITION = Pattern.compile("\\d+");
    private static final Pattern PREDICATE = Pattern.compile(
        "@(" + NC_NAME + "(?::" + NC_NAME + ")?)\\s*=\\s*(?:'([^']*)'|\"([^\"]*)\")");
    private static final String TEXT_FUNCTION = "text()";

    private final List<Step> steps;
    private final String attributeQName;
    private final boolean endsWithText;

    private Hl7V3WritablePath(List<Step> steps, String attributeQName, boolean endsWithText) {
        this.steps = Collections.unmodifiableList(steps);
        this.attributeQName = attributeQName;
        this.endsWithText = endsWithText;
    }

    /**
     * Parses a writable XPath expression.
     *
     * @param expression the expression, relative to the root element
     * @return the parsed path
     * @throws IllegalArgumentException with the reason when the expression is blank or not writable
     */
    public static Hl7V3WritablePath parse(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("XPath must not be blank");
        }
        String text = expression.trim();
        String masked = maskLiterals(text);
        rejectUnsupportedTokens(masked);
        List<String> segments = splitSteps(text, masked);
        List<Step> steps = new ArrayList<>();
        String attributeQName = null;
        boolean endsWithText = false;
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("empty step");
            }
            if (segment.startsWith("@")) {
                if (!last) {
                    throw new IllegalArgumentException("an attribute step must be the last step");
                }
                attributeQName = attributeName(segment.substring(1).trim());
            } else if (TEXT_FUNCTION.equals(segment)) {
                if (!last) {
                    throw new IllegalArgumentException("text() must be the last step");
                }
                endsWithText = true;
            } else {
                steps.add(elementStep(segment));
            }
        }
        return new Hl7V3WritablePath(steps, attributeQName, endsWithText);
    }

    /**
     * Returns whether the expression can be written by the HL7 v3 writer.
     *
     * @param expression the expression to check
     * @return {@code true} when {@link #parse(String)} accepts it
     */
    public static boolean isWritable(String expression) {
        try {
            parse(expression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns the element steps, from the root element down, in order.
     */
    List<Step> steps() {
        return steps;
    }

    /**
     * Returns the qualified name of the final attribute, such as {@code root} or {@code xsi:nil}, or {@code null} when
     * the path does not end in an attribute.
     */
    String attributeQName() {
        return attributeQName;
    }

    /**
     * Returns whether the path ends in {@code text()}.
     */
    boolean endsWithText() {
        return endsWithText;
    }

    private static void rejectUnsupportedTokens(String masked) {
        if (masked.startsWith("/")) {
            throw new IllegalArgumentException("absolute paths are not writable; the path is relative to the root element");
        }
        if (masked.contains("//")) {
            throw new IllegalArgumentException("descendant steps ('//') are not writable");
        }
        if (masked.contains("::")) {
            throw new IllegalArgumentException("XPath axes ('::') are not writable");
        }
        if (masked.contains("..")) {
            throw new IllegalArgumentException("parent steps ('..') are not writable");
        }
        if (masked.contains("|")) {
            throw new IllegalArgumentException("unions ('|') are not writable");
        }
        if (masked.contains("*")) {
            throw new IllegalArgumentException("wildcards ('*') are not writable");
        }
        if (masked.replace(TEXT_FUNCTION, "").indexOf('(') >= 0) {
            throw new IllegalArgumentException("function calls and grouping '(' are not writable, except text()");
        }
    }

    private static List<String> splitSteps(String text, String masked) {
        List<String> segments = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < masked.length(); i++) {
            char c = masked.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth < 0) {
                    throw new IllegalArgumentException("unbalanced ']'");
                }
            } else if (c == '/' && depth == 0) {
                segments.add(text.substring(start, i).trim());
                start = i + 1;
            }
        }
        if (depth != 0) {
            throw new IllegalArgumentException("unbalanced '['");
        }
        segments.add(text.substring(start).trim());
        return segments;
    }

    private static Step elementStep(String segment) {
        if (".".equals(segment)) {
            throw new IllegalArgumentException("self steps ('.') are not writable");
        }
        int firstPredicate = indexOfOutsideLiteral(segment, '[');
        String name = firstPredicate < 0 ? segment : segment.substring(0, firstPredicate).trim();
        if (!QNAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid element name '" + name + "'");
        }
        List<Predicate> predicates = new ArrayList<>();
        int position = 0;
        int cursor = firstPredicate < 0 ? segment.length() : firstPredicate;
        while (cursor < segment.length()) {
            if (segment.charAt(cursor) != '[') {
                throw new IllegalArgumentException("unexpected text after a predicate in '" + segment + "'");
            }
            int close = indexOfOutsideLiteral(segment, ']', cursor + 1);
            if (close < 0) {
                throw new IllegalArgumentException("unclosed predicate in '" + segment + "'");
            }
            String content = segment.substring(cursor + 1, close).trim();
            if (POSITION.matcher(content).matches()) {
                if (position != 0) {
                    throw new IllegalArgumentException("only one positional predicate is allowed per step");
                }
                position = positionOf(content);
            } else {
                predicates.add(predicateOf(content));
            }
            cursor = close + 1;
        }
        return new Step(name, predicates, position);
    }

    private static int positionOf(String content) {
        try {
            int value = Integer.parseInt(content);
            if (value < 1) {
                throw new IllegalArgumentException("positional predicate must be 1 or greater");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("positional predicate is out of range: " + content, e);
        }
    }

    private static Predicate predicateOf(String content) {
        Matcher matcher = PREDICATE.matcher(content);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unsupported predicate '[" + content
                + "]': only [@attr='value'] and [n] are writable");
        }
        String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
        return new Predicate(matcher.group(1), value);
    }

    /**
     * Returns the prefix of a qualified name, or an empty string when it has none.
     */
    static String prefixOf(String qualifiedName) {
        int colon = qualifiedName.indexOf(':');
        return colon < 0 ? "" : qualifiedName.substring(0, colon);
    }

    /**
     * Returns the local part of a qualified name.
     */
    static String localNameOf(String qualifiedName) {
        int colon = qualifiedName.indexOf(':');
        return colon < 0 ? qualifiedName : qualifiedName.substring(colon + 1);
    }

    private static String attributeName(String name) {
        if (!QNAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid attribute name '" + name + "'");
        }
        return name;
    }

    private static int indexOfOutsideLiteral(String text, char target) {
        return indexOfOutsideLiteral(text, target, 0);
    }

    private static int indexOfOutsideLiteral(String text, char target, int from) {
        String masked = maskLiterals(text);
        return masked.indexOf(target, from);
    }

    private static String maskLiterals(String text) {
        StringBuilder masked = new StringBuilder(text.length());
        char quote = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote == 0) {
                if (c == '\'' || c == '"') {
                    quote = c;
                }
                masked.append(c);
            } else if (c == quote) {
                quote = 0;
                masked.append(c);
            } else {
                masked.append('_');
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("unterminated string literal");
        }
        return masked.toString();
    }

    /**
     * One element step of a writable path: a qualified name, its attribute predicates and an optional position.
     */
    static final class Step {

        private final String qualifiedName;
        private final List<Predicate> predicates;
        private final int position;

        Step(String qualifiedName, List<Predicate> predicates, int position) {
            this.qualifiedName = qualifiedName;
            this.predicates = Collections.unmodifiableList(predicates);
            this.position = position;
        }

        /** Returns the name as written, {@code prefix:name} or {@code name}. */
        String qualifiedName() {
            return qualifiedName;
        }

        /** Returns the prefix, or an empty string when the name has none. */
        String prefix() {
            return prefixOf(qualifiedName);
        }

        /** Returns the local name. */
        String localName() {
            return localNameOf(qualifiedName);
        }

        /** Returns the attribute predicates, in the order written. */
        List<Predicate> predicates() {
            return predicates;
        }

        /** Returns the 1-based position, or 0 when the step has no positional predicate. */
        int position() {
            return position;
        }
    }

    /**
     * One {@code [@attr='value']} predicate.
     */
    static final class Predicate {

        private final String attributeQName;
        private final String value;

        Predicate(String attributeQName, String value) {
            this.attributeQName = attributeQName;
            this.value = value;
        }

        /** Returns the qualified attribute name as written. */
        String attributeQName() {
            return attributeQName;
        }

        /** Returns the literal value, without quotes. */
        String value() {
            return value;
        }
    }
}
