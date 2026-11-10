package io.github.tdteixeira.hl7handler.network;

import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;

import java.time.OffsetDateTime;

/**
 * Generates version-aware HL7 v2 ACK (Acknowledgment) messages.
 */
public final class Hl7AckGenerator {

    private Hl7AckGenerator() {
    }

    /**
     * Generates a standard application accept (AA) ACK for the given message.
     */
    public static String generateAck(Hl7Message incomingMessage) {
        return generateAck(incomingMessage, "AA", "Message accepted");
    }

    /**
     * Generates an ACK message with custom acknowledgment code (AA, AE, AR) and message text.
     */
    public static String generateAck(Hl7Message incomingMessage, String ackCode, String textMessage) {
        if (incomingMessage == null) {
            return "";
        }

        Hl7Delimiters delims = incomingMessage.getDelimiters() != null
                ? incomingMessage.getDelimiters()
                : Hl7Delimiters.DEFAULT;

        char fs = delims.fieldSeparator();
        char cs = delims.componentSeparator();

        String rawVersion = incomingMessage.get("MSH.12");
        if (rawVersion.isEmpty()) {
            rawVersion = incomingMessage.getVersion().getIdentifier();
        }

        String incomingSendingApp = incomingMessage.get("MSH.3");
        String incomingSendingFacility = incomingMessage.get("MSH.4");
        String incomingReceivingApp = incomingMessage.get("MSH.5");
        String incomingReceivingFacility = incomingMessage.get("MSH.6");

        String ackSendingApp = incomingReceivingApp.isEmpty() ? "HL7_HANDLER" : incomingReceivingApp;
        String ackSendingFacility = incomingReceivingFacility.isEmpty() ? "LOCAL" : incomingReceivingFacility;
        String ackReceivingApp = incomingSendingApp;
        String ackReceivingFacility = incomingSendingFacility;

        OffsetDateTime now = OffsetDateTime.now();
        Hl7Version version = incomingMessage.getVersion();
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(version);
        String timestamp = strategy.formatHeaderTimestamp(now);

        String eventCoordinate = strategy.getDefaultEventCoordinate();
        String event = incomingMessage.get(eventCoordinate);
        if (event.isEmpty() && !"MSH.9.2".equals(eventCoordinate)) {
            event = incomingMessage.get("MSH.9.2");
        }
        if (event.isEmpty() && !"EVN.1".equals(eventCoordinate)) {
            event = incomingMessage.get("EVN.1");
        }
        if (event.isEmpty()) {
            event = "R01";
        }

        String messageType = strategy.formatMsh9(event, cs);

        String incomingControlId = incomingMessage.get("MSH.10");
        String ackControlId = "ACK-" + (incomingControlId.isEmpty() ? now.toEpochSecond() : incomingControlId);
        String processingId = incomingMessage.get("MSH.11");
        if (processingId.isEmpty()) {
            processingId = "P";
        }

        String charset = incomingMessage.get("MSH.18");

        StringBuilder msh = new StringBuilder("MSH");
        msh.append(fs);
        msh.append(delims.componentSeparator())
           .append(delims.repetitionSeparator())
           .append(delims.escapeCharacter())
           .append(delims.subcomponentSeparator());
        if (strategy.isTruncationSupported() && delims.truncationCharacter() != null) {
            msh.append(delims.truncationCharacter());
        }

        msh.append(fs).append(ackSendingApp);
        msh.append(fs).append(ackSendingFacility);
        msh.append(fs).append(ackReceivingApp);
        msh.append(fs).append(ackReceivingFacility);
        msh.append(fs).append(timestamp);
        msh.append(fs); // MSH-8 security
        msh.append(fs).append(messageType);
        msh.append(fs).append(ackControlId);
        msh.append(fs).append(processingId);
        msh.append(fs).append(rawVersion);

        if (strategy.isMsh18Supported() && !charset.isEmpty()) {
            // Fill fields up to MSH-18
            for (int i = 13; i <= 17; i++) {
                msh.append(fs);
            }
            msh.append(fs).append(charset);
        }

        StringBuilder msa = new StringBuilder("MSA");
        msa.append(fs).append(ackCode != null ? ackCode : "AA");
        msa.append(fs).append(incomingControlId);
        if (textMessage != null && !textMessage.isEmpty()) {
            msa.append(fs).append(textMessage);
        }

        return msh.toString() + "\r" + msa.toString() + "\r";
    }
}
