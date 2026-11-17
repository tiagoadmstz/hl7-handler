package io.github.tiagoadmstz.hl7handler.example.spring.services;

import module java.base;

import org.springframework.stereotype.Service;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.api.Hl7Processor;
import io.github.tiagoadmstz.hl7handler.api.Hl7Result;

@Service
public class HL7SpringService {

    private final Map<String, Hl7Processor> processors = new ConcurrentHashMap<>();
    private final Hl7Processor plainProcessor = Hl7Handler.builder().build();

    public String generateAck(String rawHl7) {
        if (rawHl7 == null || rawHl7.isEmpty()) {
            throw new IllegalArgumentException("HL7 message not provided");
        }
        Hl7Result result = plainProcessor.read(rawHl7);
        return Hl7Handler.generateAck(result.message());
    }

    public <T extends Object> T processHL7Message(String message, String mappingFile, Class<T> mappingClass) {
        validateInputs(message, mappingFile, mappingClass);
        Hl7Result result = processorFor(mappingFile).read(message);
        if (mappingClass == String.class) {
            return (T) result.toJson();
        }
        return result.bindTo(mappingClass);
    }

    private Hl7Processor processorFor(String mappingFile) {
        return processors.computeIfAbsent(mappingFile, this::loadProcessor);
    }

    private Hl7Processor loadProcessor(String mappingFile) {
        try (InputStream mapping = getClass().getResourceAsStream(mappingFile)) {
            if (mapping == null) {
                throw new IllegalArgumentException("Mapping file not found: " + mappingFile);
            }
            return Hl7Handler.builder().withMapping(mapping).build();
        } catch (IOException exception) {
            throw new UncheckedIOException("Error closing mapping file: " + mappingFile, exception);
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
