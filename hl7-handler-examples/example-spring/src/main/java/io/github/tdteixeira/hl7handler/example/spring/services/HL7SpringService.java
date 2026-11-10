package io.github.tdteixeira.hl7handler.example.spring.services;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import io.github.tdteixeira.hl7handler.Hl7Handler;
import io.github.tdteixeira.hl7handler.api.Hl7Reader;

@Service
public class HL7SpringService {

    public <T extends Object> T processHL7Message(String message, String mappingFile, Class<T> mappingClass) {
        validateInputs(message, mappingFile, mappingClass);
        if (mappingClass == String.class) {
            return (T) processHL7Message(message, mappingFile).toJson();
        }
        return processHL7Message(message, mappingFile).bindTo(mappingClass);
    }

    private Hl7Reader processHL7Message(String message, String mappingFile) {
        try (InputStream mapping = getClass().getResourceAsStream(mappingFile)) {
            if (mapping == null) {
                throw new IllegalArgumentException("Mapping file not found: " + mappingFile);
            }
            return Hl7Handler.reader()
                    .from(message)
                    .withMapping(mapping);
        } catch (Exception exception) {
            throw new RuntimeException("Error processing HL7 message", exception);
        }
    }

    private <T extends Object> void validateInputs(String message, String mappingFile, Class<T> mappingClass) {
        if (mappingFile == null || mappingFile.isEmpty()) {
            throw new IllegalArgumentException("Mapping file not provided");
        }
        if (mappingClass == null) {
            throw new IllegalArgumentException("Mapping class not provided");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("HL7 message not provided");
        }
    }
}
