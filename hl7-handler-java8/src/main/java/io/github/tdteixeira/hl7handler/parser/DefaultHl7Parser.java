package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Component;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.model.Hl7VersionProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

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
        Hl7VersionProfile profile = Hl7VersionProfile.profileFor(version);

        List<String> warnings = new ArrayList<String>();
        if (version.isUnknown()) {
            warnings.add("Unknown HL7 version detected; falling back to profile " + profile.getVersion().getIdentifier());
        } else if (profile.isFallbackFor(version)) {
            warnings.add("HL7 version " + version.getIdentifier() + " does not have an exact profile; falling back to profile " + profile.getVersion().getIdentifier());
        }

        String[] rawLines = rawHl7.split("\r\n|\r|\n");
        List<Hl7Segment> segments = new ArrayList<Hl7Segment>();

        for (String line : rawLines) {
            if (line.isEmpty() || line.trim().isEmpty()) {
                continue;
            }
            segments.add(parseSegment(line, delimiters));
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

    private Hl7Segment parseSegment(String line, Hl7Delimiters delims) {
        String fieldSepRegex = Pattern.quote(String.valueOf(delims.fieldSeparator()));

        String[] tokens = line.split(fieldSepRegex, -1);
        String segmentName = tokens[0].trim();
        Hl7Segment segment = new Hl7Segment(segmentName);

        if ("MSH".equalsIgnoreCase(segmentName)) {
            // MSH-1 is the field separator itself
            segment.addField(createSimpleField(String.valueOf(delims.fieldSeparator()), delims.componentSeparator()));

            // tokens[1] is MSH-2 (Encoding characters)
            for (int i = 1; i < tokens.length; i++) {
                if (i == 1) {
                    segment.addField(createSimpleField(tokens[i], delims.componentSeparator()));
                } else {
                    segment.addField(parseField(tokens[i], delims));
                }
            }
        } else {
            for (int i = 1; i < tokens.length; i++) {
                segment.addField(parseField(tokens[i], delims));
            }
        }

        return segment;
    }

    private Hl7Field parseField(String rawField, Hl7Delimiters delims) {
        if (rawField == null || rawField.isEmpty()) {
            return new Hl7Field(delims.componentSeparator(), Collections.<List<Hl7Component>>emptyList());
        }

        String repSepRegex = Pattern.quote(String.valueOf(delims.repetitionSeparator()));
        String compSepRegex = Pattern.quote(String.valueOf(delims.componentSeparator()));
        String subSepRegex = Pattern.quote(String.valueOf(delims.subcomponentSeparator()));

        String[] repetitions = rawField.split(repSepRegex, -1);
        List<List<Hl7Component>> repetitionList = new ArrayList<List<Hl7Component>>();

        for (String rep : repetitions) {
            String[] rawComponents = rep.split(compSepRegex, -1);
            List<Hl7Component> components = new ArrayList<Hl7Component>();

            for (String rawComp : rawComponents) {
                if (rawComp.indexOf(delims.subcomponentSeparator()) != -1) {
                    String[] rawSubs = rawComp.split(subSepRegex, -1);
                    List<String> rawSubList = new ArrayList<String>();
                    List<String> decodedSubList = new ArrayList<String>();
                    StringBuilder decodedCompBuilder = new StringBuilder();

                    for (int s = 0; s < rawSubs.length; s++) {
                        String rawSub = rawSubs[s];
                        String decodedSub = Hl7Escapes.decode(rawSub, delims);
                        rawSubList.add(rawSub);
                        decodedSubList.add(decodedSub);
                        if (s > 0) {
                            decodedCompBuilder.append(delims.subcomponentSeparator());
                        }
                        decodedCompBuilder.append(decodedSub);
                    }
                    components.add(new Hl7Component(decodedCompBuilder.toString(), rawComp, decodedSubList, rawSubList));
                } else {
                    String decodedComp = Hl7Escapes.decode(rawComp, delims);
                    components.add(new Hl7Component(decodedComp, rawComp, Collections.<String>emptyList(), Collections.<String>emptyList()));
                }
            }
            repetitionList.add(components);
        }

        return new Hl7Field(delims.componentSeparator(), repetitionList);
    }

    private Hl7Field createSimpleField(String val, char compSep) {
        return new Hl7Field(compSep, Collections.singletonList(Collections.singletonList(new Hl7Component(val, val, Collections.<String>emptyList(), Collections.<String>emptyList()))));
    }
}
