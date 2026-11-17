package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.network.Hl7AckGenerator;
import io.github.tdteixeira.hl7handler.network.MllpFramingException;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7MllpAndAckTest {

    private static final String MSG_01 = "MSH|^~\\&|SYS1||||20261110||ADT^A01|MSG01|P|2.5\r";
    private static final String MSG_02 = "MSH|^~\\&|SYS2||||20261110||ADT^A01|MSG02|P|2.5\r";

    private static byte[] frame(String hl7, boolean includeTrailer) {
        byte[] payload = hl7.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x0B);
        out.write(payload, 0, payload.length);
        out.write(0x1C);
        if (includeTrailer) {
            out.write(0x0D);
        }
        return out.toByteArray();
    }

    @Test
    @DisplayName("Lê mensagens coladas sem trailer 0x0D devolvendo o byte ao stream")
    void shouldHandleGluedFramesWithoutTrailerCr() throws IOException {
        ByteArrayOutputStream streamBytes = new ByteArrayOutputStream();
        streamBytes.write(frame("MSH|^~\\&|SYS1||||20261110||ADT^A01|MSG01|P|2.5\r", false)); // sem 0x0D
        streamBytes.write(frame("MSH|^~\\&|SYS2||||20261110||ADT^A01|MSG02|P|2.5\r", true));

        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(streamBytes.toByteArray()), 2);
        MllpStreamReader reader = new MllpStreamReader();

        String msg1 = reader.readMessage(in);
        String msg2 = reader.readMessage(in);

        assertThat(msg1).contains("MSG01");
        assertThat(msg2).contains("MSG02");
    }

    @Test
    @DisplayName("Lança MllpFramingException em stream truncado antes do end block")
    void shouldThrowMllpFramingExceptionOnTruncatedStream() {
        byte[] truncated = new byte[]{0x0B, 'M', 'S', 'H', '|'};
        InputStream in = new ByteArrayInputStream(truncated);
        MllpStreamReader reader = new MllpStreamReader();

        assertThatThrownBy(() -> reader.readMessage(in))
                .isInstanceOf(MllpFramingException.class)
                .hasMessageContaining("Truncated MLLP frame");
    }

    @Test
    @DisplayName("Lança MllpFramingException quando o stream não possui 0x0B inicial")
    void shouldThrowMllpFramingExceptionWhenStreamLacksStartBlock() {
        byte[] unframed = "MSH|^~\\&|SYS||||20261110||ADT^A01|MSG01|P|2.5\r".getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(unframed);
        MllpStreamReader reader = new MllpStreamReader();

        assertThatThrownBy(() -> reader.readMessage(in))
                .isInstanceOf(MllpFramingException.class)
                .hasMessageContaining("Expected MLLP start block");
    }

    @Test
    @DisplayName("Permite stream sem moldura quando configurado explicitamente")
    void shouldAllowUnframedStreamWhenConfigured() throws IOException {
        String content = "MSH|^~\\&|SYS||||20261110||ADT^A01|MSG01|P|2.5\r";
        byte[] unframed = content.getBytes(StandardCharsets.UTF_8);
        InputStream in = new ByteArrayInputStream(unframed);
        MllpStreamReader reader = new MllpStreamReader(StandardCharsets.UTF_8, 1024, false);

        String msg = reader.readMessage(in);
        assertThat(msg).isEqualTo(content);
    }

    @Test
    @DisplayName("Lança MllpFramingException quando a mensagem excede o limite configurado")
    void shouldThrowMllpFramingExceptionWhenMessageExceedsLimit() {
        byte[] payload = frame("MSH|^~\\&|SYS||||20261110||ADT^A01|MSG01|P|2.5\rPAYLOAD_MUITO_LONGO", true);
        InputStream in = new ByteArrayInputStream(payload);
        MllpStreamReader reader = new MllpStreamReader(StandardCharsets.UTF_8, 20, true);

        assertThatThrownBy(() -> reader.readMessage(in))
                .isInstanceOf(MllpFramingException.class)
                .hasMessageContaining("exceeded limit");
    }

    @Test
    @DisplayName("Detecta charset ISO-8859-1 do MSH-18 e decodifica acentuação corretamente")
    void shouldDetectIso88591CharsetWithAccentsFromMsh18() throws IOException {
        String text = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.3||||||8859/1\r"
                + "PID|1||12345||CONCEIÇÃO^JOÃO\r";

        byte[] isoBytes = text.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x0B);
        out.write(isoBytes);
        out.write(0x1C);
        out.write(0x0D);

        MllpStreamReader reader = new MllpStreamReader();
        String decoded = reader.readMessage(new ByteArrayInputStream(out.toByteArray()));

        assertThat(decoded).contains("CONCEIÇÃO^JOÃO");
    }

    @Test
    @DisplayName("Gera ACK ciente da versão 2.1 com MSH-9 simples")
    void shouldGenerateAckForVersion21() {
        String hl7 = "MSH|^~\\&|APP_ORIGEM|FAC_ORIGEM|APP_DESTINO|FAC_DESTINO|20261110120000||ADT|V21-001|P|2.1\r"
                + "EVN|A01|20261110120000\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg);

        assertThat(ack).contains("MSH|^~\\&|APP_DESTINO|FAC_DESTINO|APP_ORIGEM|FAC_ORIGEM|");
        assertThat(ack).contains("|ACK|ACK-V21-001|P|2.1\r");
        assertThat(ack).contains("MSA|AA|V21-001|Message accepted\r");
    }

    @Test
    @DisplayName("Gera ACK ciente da versão 2.3 com MSH-9 tipo e evento")
    void shouldGenerateAckForVersion23() {
        String hl7 = "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|20261110120000||ADT^A01|V23-001|P|2.3\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg);

        assertThat(ack).contains("MSH|^~\\&|APP_B|FAC_B|APP_A|FAC_A|");
        assertThat(ack).contains("|ACK^A01|ACK-V23-001|P|2.3\r");
        assertThat(ack).contains("MSA|AA|V23-001|Message accepted\r");
    }

    @Test
    @DisplayName("Gera ACK ciente da versão 2.5 com MSH-9 de 3 partes")
    void shouldGenerateAckForVersion25() {
        String hl7 = "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|20261110120000||ADT^A01^ADT_A01|V25-001|P|2.5\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg);

        assertThat(ack).contains("|ACK^A01^ACK|ACK-V25-001|P|2.5\r");
        assertThat(ack).contains("MSA|AA|V25-001|Message accepted\r");
    }

    @Test
    @DisplayName("Gera ACK ciente da versão 2.6 com precisão DTM e eco de MSH-18")
    void shouldGenerateAckForVersion26Mindray() {
        String hl7 = "MSH|^~\\&|MINDRAY_EGATEWAY|MINDRAY|HOSP|CENTRAL|20261110120000.0000-0300||ORU^R01^ORU_R01|V26-001|P|2.6||||||UNICODE UTF-8\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg);

        assertThat(ack).contains("MSH|^~\\&|HOSP|CENTRAL|MINDRAY_EGATEWAY|MINDRAY|");
        assertThat(ack).contains("|ACK^R01^ACK|ACK-V26-001|P|2.6||||||UNICODE UTF-8\r");
        assertThat(ack).contains("MSA|AA|V26-001|Message accepted\r");
    }

    @Test
    @DisplayName("Lê duas mensagens consecutivas de stream com mark sem perder byte entre as chamadas")
    void shouldReadConsecutiveMessagesFromMarkSupportedStream() throws IOException {
        InputStream in = new ByteArrayInputStream(concat(frame(MSG_01, true), frame(MSG_02, true)));
        MllpStreamReader reader = new MllpStreamReader();

        assertThat(reader.readMessage(in)).contains("MSG01");
        assertThat(reader.readMessage(in)).contains("MSG02");
    }

    @Test
    @DisplayName("Lê duas mensagens consecutivas de stream sem suporte a mark sem perder byte entre as chamadas")
    void shouldReadConsecutiveMessagesFromStreamWithoutMarkSupport() throws IOException {
        InputStream in = nonMarkStream(concat(frame(MSG_01, true), frame(MSG_02, true)));
        MllpStreamReader reader = new MllpStreamReader();

        assertThat(reader.readMessage(in)).contains("MSG01");
        assertThat(reader.readMessage(in)).contains("MSG02");
    }

    @Test
    @DisplayName("Lança MllpFramingException em stream sem mark quando falta o trailer 0x0D")
    void shouldThrowMllpFramingExceptionWhenTrailerMissingOnStreamWithoutMark() {
        InputStream in = nonMarkStream(concat(frame(MSG_01, false), frame(MSG_02, false)));
        MllpStreamReader reader = new MllpStreamReader();

        assertThatThrownBy(() -> reader.readMessage(in))
                .isInstanceOf(MllpFramingException.class)
                .hasMessageContaining("Missing MLLP trailer");
    }

    @Test
    @DisplayName("Lê duas mensagens sem trailer quando o chamador passa um PushbackInputStream")
    void shouldReadConsecutiveMessagesFromCallerPushbackStream() throws IOException {
        InputStream in = new PushbackInputStream(new ByteArrayInputStream(concat(frame(MSG_01, false), frame(MSG_02, false))), 2);
        MllpStreamReader reader = new MllpStreamReader();

        assertThat(reader.readMessage(in)).contains("MSG01");
        assertThat(reader.readMessage(in)).contains("MSG02");
    }

    @Test
    @DisplayName("Ignora LF após o trailer 0x0D ao ler todas as mensagens do stream")
    void shouldSkipLineFeedAfterTrailerInReadAll() throws IOException {
        InputStream in = new ByteArrayInputStream(concat(frame(MSG_01, true), ascii("\n"), frame(MSG_02, true), ascii("\n")));
        MllpStreamReader reader = new MllpStreamReader();

        List<String> messages = reader.readAll(in);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).contains("MSG01");
        assertThat(messages.get(1)).contains("MSG02");
    }

    @Test
    @DisplayName("Devolve null sem exceção quando só restam CR e LF após o último frame")
    void shouldReturnNullWhenOnlyCrLfRemainsAfterLastFrame() throws IOException {
        InputStream in = new ByteArrayInputStream(concat(frame(MSG_01, true), ascii("\r\n")));
        MllpStreamReader reader = new MllpStreamReader();

        assertThat(reader.readMessage(in)).contains("MSG01");
        assertThat(reader.readMessage(in)).isNull();
    }

    @Test
    @DisplayName("Escapa o texto do ACK em MSA-3 e o decodifica de volta no parser")
    void shouldEscapeAckTextAndDecodeItBackWithParser() {
        String hl7 = "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|20261110120000||ADT^A01|ACK-ESC-01|P|2.5\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg, "AE", "Erro|campo^obrigatorio&x");

        assertThat(ack).contains("MSA|AE|ACK-ESC-01|Erro\\F\\campo\\S\\obrigatorio\\T\\x\r");
        Hl7Message parsedAck = Hl7Handler.reader().from(ack).toMessage();
        assertThat(parsedAck.get("MSA.3")).isEqualTo("Erro|campo^obrigatorio&x");
    }

    @Test
    @DisplayName("Copia MSH-3 com componentes idênticos ao MSH-5 do ACK")
    void shouldCopyIncomingMsh3ComponentsToAckMsh5() {
        String hl7 = "MSH|^~\\&|APP^1.2.3^ISO|FAC|DEST|FACD|20261110120000||ADT^A01|ACK-RAW-01|P|2.5\r";
        Hl7Message msg = Hl7Handler.reader().from(hl7).toMessage();

        String ack = Hl7AckGenerator.generateAck(msg);

        assertThat(ack).contains("MSH|^~\\&|DEST|FACD|APP^1.2.3^ISO|FAC|");
    }

    @Test
    @DisplayName("Codifa o payload no charset do MSH-18 ao fazer wrap")
    void shouldWrapWithCharsetFromMsh18() {
        String msg = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.3||||||8859/1\rPID|1||12345||TESTE^ação";
        byte[] expected = concat(new byte[]{0x0B}, msg.getBytes(StandardCharsets.ISO_8859_1), new byte[]{0x1C, 0x0D});

        assertThat(new MllpStreamReader().wrap(msg)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Codifa o payload em UTF-8 no wrap quando o MSH-18 está ausente")
    void shouldWrapWithUtf8WhenMsh18Absent() {
        String msg = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG02|P|2.3\rPID|1||12345||TESTE^ação";
        byte[] expected = concat(new byte[]{0x0B}, msg.getBytes(StandardCharsets.UTF_8), new byte[]{0x1C, 0x0D});

        assertThat(new MllpStreamReader().wrap(msg)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Mantém o texto original ao fazer wrap e fromBytes com MSH-18 ISO-8859-1")
    void shouldRoundTripWrappedMessageWithMsh18Charset() {
        String msg = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.3||||||8859/1\rPID|1||12345||TESTE^ação";

        assertThat(new MllpStreamReader().fromBytes(new MllpStreamReader().wrap(msg))).isEqualTo(msg);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static InputStream nonMarkStream(byte[] bytes) {
        return new FilterInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            public boolean markSupported() {
                return false;
            }
        };
    }
}
