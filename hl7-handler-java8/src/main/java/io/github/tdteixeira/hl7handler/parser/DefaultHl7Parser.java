package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7CharsetNames;
import io.github.tdteixeira.hl7handler.model.Hl7Component;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.model.Hl7VersionProfile;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Standard robust HL7 v2 parser supporting delimiters, repetitions,
 * components, subcomponents, version profiles, and conformance warnings.
 */
public class DefaultHl7Parser implements Hl7Parser {

    private final Hl7Delimiters defaultDelimiters;

    public DefaultHl7Parser() {
        this(Hl7Delimiters.DEFAULT);
    }

    public DefaultHl7Parser(Hl7Delimiters defaultDelimiters) {
        this.defaultDelimiters = defaultDelimiters != null ? defaultDelimiters : Hl7Delimiters.DEFAULT;
    }

    @Override
    public boolean supports(Hl7Version version) {
        return true;
    }

    @Override
    public Hl7Message parse(String rawHl7) {
        return parse(rawHl7, null);
    }

    @Override
    public Hl7Message parse(String rawHl7, Hl7Version detectedVersion) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            return new Hl7Message(Hl7Version.UNKNOWN, defaultDelimiters, Collections.<Hl7Segment>emptyList(), Collections.<String>emptyList());
        }

        Hl7Version version = detectedVersion != null ? detectedVersion : Hl7VersionDetector.detect(rawHl7);
        Hl7Delimiters delimiters = extractDelimiters(rawHl7);
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(version);
        Hl7VersionProfile profile = strategy.getProfile();

        List<String> warnings = new ArrayList<String>();
        if (version.isUnknown()) {
            warnings.add("Unknown HL7 version detected; falling back to profile " + profile.getVersion().getIdentifier());
        } else if (profile.isFallbackFor(version)) {
            warnings.add("HL7 version " + version.getIdentifier() + " does not have an exact profile; falling back to profile " + profile.getVersion().getIdentifier());
        }
        if (delimiters.truncationCharacter() != null && !strategy.isTruncationSupported()) {
            warnings.add("MSH-2 defines truncation character '" + delimiters.truncationCharacter() + "' which is not supported in HL7 version " + strategy.getVersion().getIdentifier());
        }

        List<String> rawLines = new ArrayList<String>();
        int lineStart = 0;
        int i = 0;
        while (i < rawHl7.length()) {
            char c = rawHl7.charAt(i);
            if (c == '\r' || c == '\n') {
                rawLines.add(rawHl7.substring(lineStart, i));
                if (c == '\r' && i + 1 < rawHl7.length() && rawHl7.charAt(i + 1) == '\n') {
                    i++;
                }
                lineStart = i + 1;
            }
            i++;
        }
        rawLines.add(rawHl7.substring(lineStart));
        Charset charset = Hl7CharsetNames.resolve(msh18Value(rawLines, delimiters), StandardCharsets.UTF_8);
        List<Hl7Segment> segments = new ArrayList<Hl7Segment>();

        for (String line : rawLines) {
            if (line.isEmpty() || line.trim().isEmpty()) {
                continue;
            }
            segments.add(parseSegment(line, delimiters, charset));
        }

        return new Hl7Message(version, delimiters, segments, warnings);
    }

    private Hl7Delimiters extractDelimiters(String rawHl7) {
        int mshIdx = Hl7VersionDetector.findMshIndex(rawHl7);
        if (mshIdx != -1) {
            return Hl7Delimiters.fromMshHeader(rawHl7.substring(mshIdx));
        }
        return defaultDelimiters;
    }

    private static String msh18Value(List<String> rawLines, Hl7Delimiters delims) {
        for (String line : rawLines) {
            int firstSep = line.indexOf(delims.fieldSeparator());
            String name = firstSep == -1 ? line : line.substring(0, firstSep);
            if ("MSH".equalsIgnoreCase(name.trim())) {
                List<String> tokens = splitKeepingEmpty(line, delims.fieldSeparator());
                if (tokens.size() <= 17) {
                    return "";
                }
                String msh18 = tokens.get(17);
                int compSep = msh18.indexOf(delims.componentSeparator());
                return compSep == -1 ? msh18 : msh18.substring(0, compSep);
            }
        }
        return "";
    }

    private Hl7Segment parseSegment(String line, Hl7Delimiters delims, Charset charset) {
        List<String> tokens = splitKeepingEmpty(line, delims.fieldSeparator());
        String segmentName = tokens.get(0).trim();
        Hl7Segment segment = new Hl7Segment(segmentName);

        if ("MSH".equalsIgnoreCase(segmentName)) {
            // MSH-1 is the field separator itself
            segment.addField(createSimpleField(String.valueOf(delims.fieldSeparator()), delims.componentSeparator()));

            // tokens[1] is MSH-2 (Encoding characters)
            for (int i = 1; i < tokens.size(); i++) {
                if (i == 1) {
                    segment.addField(createSimpleField(tokens.get(i), delims.componentSeparator()));
                } else {
                    segment.addField(parseField(tokens.get(i), delims, charset));
                }
            }
        } else {
            for (int i = 1; i < tokens.size(); i++) {
                segment.addField(parseField(tokens.get(i), delims, charset));
            }
        }

        return segment;
    }

    private Hl7Field parseField(String rawField, Hl7Delimiters delims, Charset charset) {
        if (rawField == null || rawField.isEmpty()) {
            return new Hl7Field(delims.componentSeparator(), Collections.<List<Hl7Component>>emptyList());
        }

        List<List<Hl7Component>> repetitionList = new ArrayList<List<Hl7Component>>();

        for (String rep : splitKeepingEmpty(rawField, delims.repetitionSeparator())) {
            List<Hl7Component> components = new ArrayList<Hl7Component>();

            for (String rawComp : splitKeepingEmpty(rep, delims.componentSeparator())) {
                if (rawComp.indexOf(delims.subcomponentSeparator()) != -1) {
                    List<String> rawSubs = splitKeepingEmpty(rawComp, delims.subcomponentSeparator());
                    List<String> rawSubList = new ArrayList<String>();
                    List<String> decodedSubList = new ArrayList<String>();
                    StringBuilder decodedCompBuilder = new StringBuilder();

                    for (int s = 0; s < rawSubs.size(); s++) {
                        String rawSub = rawSubs.get(s);
                        String decodedSub = Hl7Escapes.decode(rawSub, delims, charset);
                        rawSubList.add(rawSub);
                        decodedSubList.add(decodedSub);
                        if (s > 0) {
                            decodedCompBuilder.append(delims.subcomponentSeparator());
                        }
                        decodedCompBuilder.append(decodedSub);
                    }
                    components.add(new Hl7Component(decodedCompBuilder.toString(), rawComp, decodedSubList, rawSubList));
                } else {
                    String decodedComp = Hl7Escapes.decode(rawComp, delims, charset);
                    components.add(new Hl7Component(decodedComp, rawComp, Collections.<String>emptyList(), Collections.<String>emptyList()));
                }
            }
            repetitionList.add(components);
        }

        return new Hl7Field(delims.componentSeparator(), repetitionList);
    }

    private static List<String> splitKeepingEmpty(String text, char separator) {
        List<String> parts = new ArrayList<String>();
        int start = 0;
        int idx = text.indexOf(separator, start);
        while (idx != -1) {
            parts.add(text.substring(start, idx));
            start = idx + 1;
            idx = text.indexOf(separator, start);
        }
        parts.add(text.substring(start));
        return parts;
    }

    private Hl7Field createSimpleField(String val, char compSep) {
        return new Hl7Field(compSep, Collections.singletonList(Collections.singletonList(new Hl7Component(val, val, Collections.<String>emptyList(), Collections.<String>emptyList()))));
    }
}
