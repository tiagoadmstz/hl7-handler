package io.github.tdteixeira.hl7handler.parser;

import io.github.tdteixeira.hl7handler.model.Hl7Component;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.model.Hl7Version;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Standard robust HL7 v2 parser supporting delimiters, repetitions,
 * components, and subcomponents.
 */
public class DefaultHl7Parser implements Hl7Parser {

    @Override
    public boolean supports(Hl7Version version) {
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
            if (line.isEmpty() || line.trim().isEmpty()) {
                continue;
            }
            segments.add(parseSegment(line, delimiters));
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
            return new Hl7Field(delims.componentSeparator(), List.of());
        }

        String repSepRegex = Pattern.quote(String.valueOf(delims.repetitionSeparator()));
        String compSepRegex = Pattern.quote(String.valueOf(delims.componentSeparator()));
        String subSepRegex = Pattern.quote(String.valueOf(delims.subcomponentSeparator()));

        String[] repetitions = rawField.split(repSepRegex, -1);
        List<List<Hl7Component>> repetitionList = new ArrayList<>();

        for (String rep : repetitions) {
            String[] rawComponents = rep.split(compSepRegex, -1);
            List<Hl7Component> components = new ArrayList<>();

            for (String rawComp : rawComponents) {
                if (rawComp.indexOf(delims.subcomponentSeparator()) != -1) {
                    String[] rawSubs = rawComp.split(subSepRegex, -1);
                    List<String> rawSubList = new ArrayList<>();
                    List<String> decodedSubList = new ArrayList<>();
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
                    components.add(new Hl7Component(decodedComp, rawComp, List.of(), List.of()));
                }
            }
            repetitionList.add(components);
        }

        return new Hl7Field(delims.componentSeparator(), repetitionList);
    }

    private Hl7Field createSimpleField(String val, char compSep) {
        return new Hl7Field(compSep, List.of(List.of(new Hl7Component(val, val, List.of(), List.of()))));
    }
}
