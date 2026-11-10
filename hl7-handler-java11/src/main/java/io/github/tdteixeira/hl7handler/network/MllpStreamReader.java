package io.github.tdteixeira.hl7handler.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for reading and converting HL7 network byte streams,
 * supporting MLLP (Minimal Lower Layer Protocol) framing, charset detection,
 * and message size protection.
 *
 * MLLP framing standard:
 * - Start Block (SB): 0x0B (Vertical Tab - &lt;VT&gt;)
 * - End Block (EB):   0x1C (File Separator - &lt;FS&gt;)
 * - Trailer:          0x0D (Carriage Return - &lt;CR&gt;)
 */
public class MllpStreamReader {

    public static final byte START_BLOCK = 0x0B;
    public static final byte END_BLOCK = 0x1C;
    public static final byte TRAILER = 0x0D;

    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1024 * 1024; // 1 MB

    private final Charset defaultCharset;
    private final int maxMessageSize;
    private final boolean requireFraming;

    public MllpStreamReader() {
        this(StandardCharsets.UTF_8, DEFAULT_MAX_MESSAGE_SIZE, true);
    }

    public MllpStreamReader(Charset defaultCharset) {
        this(defaultCharset, DEFAULT_MAX_MESSAGE_SIZE, true);
    }

    public MllpStreamReader(Charset defaultCharset, int maxMessageSize, boolean requireFraming) {
        this.defaultCharset = defaultCharset != null ? defaultCharset : StandardCharsets.UTF_8;
        this.maxMessageSize = maxMessageSize > 0 ? maxMessageSize : DEFAULT_MAX_MESSAGE_SIZE;
        this.requireFraming = requireFraming;
    }

    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public boolean isRequireFraming() {
        return requireFraming;
    }

    /**
     * Reads a single MLLP-framed HL7 message from an InputStream.
     * Detects message character set from MSH-18 and enforces size limits.
     *
     * @param in the network input stream
     * @return the HL7 message string without framing characters, or null on clean EOF
     * @throws MllpFramingException if framing is violated, truncated, or size limit exceeded
     * @throws IOException on I/O error
     */
    public String readMessage(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }

        PushbackInputStream pbin = (in instanceof PushbackInputStream)
                ? (PushbackInputStream) in
                : new PushbackInputStream(in, 2);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        boolean inMessage = false;

        while ((b = pbin.read()) != -1) {
            byte current = (byte) b;

            if (!inMessage) {
                if (current == START_BLOCK) {
                    inMessage = true;
                    continue;
                }

                if (requireFraming) {
                    throw new MllpFramingException(
                            "Expected MLLP start block (0x0B), but found byte: 0x" + Integer.toHexString(b & 0xFF)
                    );
                }

                inMessage = true;
                buffer.write(current);
                continue;
            }

            if (current == END_BLOCK) {
                int next = pbin.read();
                if (next != -1 && next != TRAILER) {
                    pbin.unread(next);
                }

                byte[] rawBytes = buffer.toByteArray();
                Charset charset = Hl7Charsets.detectCharset(rawBytes, defaultCharset);
                return new String(rawBytes, charset);
            }

            if (buffer.size() >= maxMessageSize) {
                throw new MllpFramingException(
                        "MLLP message size exceeded limit of " + maxMessageSize + " bytes"
                );
            }

            buffer.write(current);
        }

        if (inMessage && requireFraming) {
            throw new MllpFramingException("Truncated MLLP frame: stream reached EOF before end block (0x1C)");
        }

        if (buffer.size() > 0) {
            byte[] rawBytes = buffer.toByteArray();
            Charset charset = Hl7Charsets.detectCharset(rawBytes, defaultCharset);
            return new String(rawBytes, charset);
        }

        return null;
    }

    /**
     * Converts a raw byte array into an HL7 string, automatically stripping MLLP framing
     * and detecting encoding from MSH-18.
     */
    public String fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        int start = 0;
        int end = bytes.length;

        while (start < end && bytes[start] == START_BLOCK) {
            start++;
        }

        while (end > start && (bytes[end - 1] == TRAILER || bytes[end - 1] == END_BLOCK || bytes[end - 1] == 0x0A)) {
            end--;
        }

        if (start >= end) {
            return "";
        }

        byte[] payload = new byte[end - start];
        System.arraycopy(bytes, start, payload, 0, payload.length);
        Charset charset = Hl7Charsets.detectCharset(payload, defaultCharset);
        return new String(payload, charset);
    }

    /**
     * Wraps an HL7 message string into standard MLLP framed byte array using the detected or default encoding.
     */
    public byte[] wrap(String hl7Message) {
        if (hl7Message == null) {
            hl7Message = "";
        }

        byte[] payload = hl7Message.getBytes(defaultCharset);
        byte[] framed = new byte[payload.length + 3];

        framed[0] = START_BLOCK;
        System.arraycopy(payload, 0, framed, 1, payload.length);
        framed[framed.length - 2] = END_BLOCK;
        framed[framed.length - 1] = TRAILER;

        return framed;
    }

    /**
     * Reads all available messages from the stream until EOF.
     */
    public List<String> readAll(InputStream in) throws IOException {
        List<String> messages = new ArrayList<>();
        PushbackInputStream pbin = (in instanceof PushbackInputStream)
                ? (PushbackInputStream) in
                : new PushbackInputStream(in, 2);

        String msg;
        while ((msg = readMessage(pbin)) != null) {
            if (!msg.trim().isEmpty()) {
                messages.add(msg);
            }
        }
        return messages;
    }
}
