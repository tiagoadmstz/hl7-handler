package io.github.tdteixeira.hl7handler.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for reading and converting HL7 network byte streams,
 * supporting both MLLP (Minimal Lower Layer Protocol) framing and raw byte streams.
 *
 * MLLP framing standard:
 * - Start Block (SB): 0x0B (Vertical Tab - <VT>)
 * - End Block (EB):   0x1C (File Separator - <FS>)
 * - Trailer:          0x0D (Carriage Return - <CR>)
 */
public class MllpStreamReader {

    public static final byte START_BLOCK = 0x0B;
    public static final byte END_BLOCK = 0x1C;
    public static final byte TRAILER = 0x0D;

    private final Charset charset;

    public MllpStreamReader() {
        this(StandardCharsets.UTF_8);
    }

    public MllpStreamReader(Charset charset) {
        this.charset = charset != null ? charset : StandardCharsets.UTF_8;
    }

    /**
     * Reads a single MLLP-framed HL7 message from an InputStream.
     * Blocks until the end-of-block sequence is received or stream reaches EOF.
     *
     * @param in the network input stream
     * @return the HL7 message string without framing characters, or null if EOF before message
     * @throws IOException on I/O error
     */
    public String readMessage(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        boolean inMessage = false;

        while ((b = in.read()) != -1) {
            byte current = (byte) b;

            if (!inMessage) {
                if (current == START_BLOCK) {
                    inMessage = true;
                }
                // Ignore leading bytes before START_BLOCK
                continue;
            }

            // Message started, check for END_BLOCK
            if (current == END_BLOCK) {
                // Next byte should be TRAILER (0x0D)
                int next = in.read();
                if (next != -1 && next != TRAILER) {
                    // In some non-strict implementations, put back or include
                    buffer.write(next);
                }
                return new String(buffer.toByteArray(), charset);
            }

            buffer.write(current);
        }

        // If stream ended and we captured content without formal MLLP frame, return it if not empty
        if (buffer.size() > 0) {
            return new String(buffer.toByteArray(), charset);
        }

        return null;
    }

    /**
     * Converts a raw byte array into an HL7 string, automatically stripping MLLP framing if present.
     */
    public String fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        int start = 0;
        int end = bytes.length;

        // Skip leading MLLP START_BLOCK if present
        while (start < end && bytes[start] == START_BLOCK) {
            start++;
        }

        // Strip trailing END_BLOCK and TRAILER if present
        while (end > start && (bytes[end - 1] == TRAILER || bytes[end - 1] == END_BLOCK || bytes[end - 1] == 0x0A)) {
            end--;
        }

        if (start >= end) {
            return "";
        }

        return new String(bytes, start, end - start, charset);
    }

    /**
     * Wraps an HL7 message string into standard MLLP framed byte array (e.g. for sending ACK responses).
     */
    public byte[] wrap(String hl7Message) {
        if (hl7Message == null) {
            hl7Message = "";
        }
        byte[] payload = hl7Message.getBytes(charset);
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
        String msg;
        while ((msg = readMessage(in)) != null) {
            if (!msg.trim().isEmpty()) {
                messages.add(msg);
            }
        }
        return messages;
    }
}
