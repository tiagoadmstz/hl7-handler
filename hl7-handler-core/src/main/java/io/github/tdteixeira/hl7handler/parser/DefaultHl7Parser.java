package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Standard robust HL7 v2 parser supporting delimiters, repetitions,
 * components, and subcomponents.
 */
public class DefaultHl7Parser implements Hl7Parser {

    @Override
    public boolean supports(Hl7Version version) {
        // Default parser handles generic HL7 v2 structure
        return true;
    }

    @Override
    public Hl7Message parse(String rawHl7) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            return new Hl7Message(Hl7Version.UNKNOWN, Hl7Delimiters.DEFAULT, List.of());
        }

        Hl7Version version = Hl7VersionDetector.detect(rawHl7);
        Hl7Delimiters delimiters = extractDelimiters(rawHl7);

        String[] rawLines = rawHl7.split("\r\n|\r|\n");
        List<Hl7Segment> segments = new ArrayList<>();

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            segments.add(parseSegment(trimmed, delimiters));
        }

        return new Hl7Message(version, delimiters, segments);
    }

    private Hl7Delimiters extractDelimiters(String rawHl7) {
        int mshIdx = rawHl7.indexOf("MSH");
        if (mshIdx != -1) {
            return Hl7Delimiters.fromMshHeader(rawHl7.substring(mshIdx));
        }
        return Hl7Delimiters.DEFAULT;
    }

    private Hl7Segment parseSegment(String line, Hl7Delimiters delims) {
        String fieldSepRegex = Pattern.quote(String.valueOf(delims.fieldSeparator()));
        String compSepRegex = Pattern.quote(String.valueOf(delims.componentSeparator()));
        String repSepRegex = Pattern.quote(String.valueOf(delims.repetitionSeparator()));
        String subSepRegex = Pattern.quote(String.valueOf(delims.subcomponentSeparator()));

        String[] tokens = line.split(fieldSepRegex, -1);
        String segmentName = tokens[0].trim();
        Hl7Segment segment = new Hl7Segment(segmentName);

        if ("MSH".equalsIgnoreCase(segmentName)) {
            // MSH-1 is the field separator itself
            segment.addField(createSimpleField(String.valueOf(delims.fieldSeparator())));

            // tokens[1] is MSH-2 (Encoding characters)
            for (int i = 1; i < tokens.length; i++) {
                if (i == 1) {
                    // MSH-2 is literal encoding characters, do not split by component separator
                    segment.addField(createSimpleField(tokens[i]));
                } else {
                    segment.addField(parseField(tokens[i], repSepRegex, compSepRegex, subSepRegex));
                }
            }
        } else {
            // Standard segment: tokens[1] is Field 1, tokens[2] is Field 2, etc.
            for (int i = 1; i < tokens.length; i++) {
                segment.addField(parseField(tokens[i], repSepRegex, compSepRegex, subSepRegex));
            }
        }

        return segment;
    }

    private Hl7Field parseField(String rawField, String repRegex, String compRegex, String subRegex) {
        if (rawField == null || rawField.isEmpty()) {
            return new Hl7Field();
        }

        String[] repetitions = rawField.split(repRegex, -1);
        List<List<Hl7Component>> repetitionList = new ArrayList<>();

        for (String rep : repetitions) {
            String[] rawComponents = rep.split(compRegex, -1);
            List<Hl7Component> components = new ArrayList<>();

            for (String rawComp : rawComponents) {
                if (rawComp.contains("&")) {
                    String[] rawSubs = rawComp.split(subRegex, -1);
                    components.add(new Hl7Component(rawComp, Arrays.asList(rawSubs)));
                } else {
                    components.add(new Hl7Component(rawComp));
                }
            }
            repetitionList.add(components);
        }

        return new Hl7Field(repetitionList);
    }

    private Hl7Field createSimpleField(String val) {
        Hl7Field field = new Hl7Field();
        field.addRepetition(List.of(new Hl7Component(val)));
        return field;
    }
}
