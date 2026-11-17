package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.writer.Hl7Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7ParserFidelityTest {

    private static final String SAMPLES = "/samples/versions/";

    private static final String EDGE_HEADER = "MSH|^~\\&|SIS|HOSP|LAB|HOSP|20261117120000||ADT^A01|MSG202611170001|P|2.5\r";

    private static String sample(String fileName) {
        return TestResources.read(SAMPLES + fileName);
    }

    private static String normalizeSegmentTerminators(String raw) {
        String[] lines = raw.split("\r\n|\r|\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.isEmpty() || line.trim().isEmpty()) {
                continue;
            }
            sb.append(line).append('\r');
        }
        return sb.toString();
    }

    @Test
    @DisplayName("Lê MSH-2 curto sem invadir o próximo campo")
    void shouldParseShortMsh2WithoutConsumingNextField() {
        Hl7Delimiters delims = Hl7Delimiters.fromMshHeader("MSH|^~|SIS|HOSP|");
        assertThat(delims.fieldSeparator()).isEqualTo('|');
        assertThat(delims.componentSeparator()).isEqualTo('^');
        assertThat(delims.repetitionSeparator()).isEqualTo('~');
        assertThat(delims.escapeCharacter()).isEqualTo('\\');
        assertThat(delims.subcomponentSeparator()).isEqualTo('&');
        assertThat(delims.truncationCharacter()).isNull();
    }

    @Test
    @DisplayName("Lê MSH-2 com caractere de truncamento da versão 2.7+")
    void shouldParseMsh2WithTruncationCharacter() {
        Hl7Delimiters delims = Hl7Delimiters.fromMshHeader("MSH|^~\\&#|SIS|HOSP|");
        assertThat(delims.componentSeparator()).isEqualTo('^');
        assertThat(delims.repetitionSeparator()).isEqualTo('~');
        assertThat(delims.escapeCharacter()).isEqualTo('\\');
        assertThat(delims.subcomponentSeparator()).isEqualTo('&');
        assertThat(delims.truncationCharacter()).isEqualTo('#');
    }

    @Test
    @DisplayName("Decodifica sequências de escape padrão e quebra de linha")
    void shouldDecodeStandardEscapeSequences() {
        String raw = "SILVA\\S\\JR\\F\\TESTE\\T\\SUB\\R\\REP\\E\\BARRA\\.br\\NOVALINHA";
        String decoded = Hl7Escapes.decode(raw, Hl7Delimiters.DEFAULT);
        assertThat(decoded).isEqualTo("SILVA^JR|TESTE&SUB~REP\\BARRA\nNOVALINHA");
    }

    @Test
    @DisplayName("Decodifica caracteres hexadecimais")
    void shouldDecodeHexEscapeSequences() {
        String raw = "ABC\\X4445\\\\F\\\\X20\\GHI";
        String decoded = Hl7Escapes.decode(raw, Hl7Delimiters.DEFAULT);
        assertThat(decoded).isEqualTo("ABCDE| GHI");
    }

    @Test
    @DisplayName("Decodifica escape hexadecimal UTF-8 quando o charset é UTF-8")
    void shouldDecodeHexEscapeWithExplicitUtf8Charset() {
        String decoded = Hl7Escapes.decode("\\XC3A7\\", Hl7Delimiters.DEFAULT, StandardCharsets.UTF_8);
        assertThat(decoded).isEqualTo("ç");
    }

    @Test
    @DisplayName("Decodifica escape hexadecimal ISO-8859-1 quando o charset é ISO-8859-1")
    void shouldDecodeHexEscapeWithExplicitIso88591Charset() {
        String decoded = Hl7Escapes.decode("\\XE7\\", Hl7Delimiters.DEFAULT, StandardCharsets.ISO_8859_1);
        assertThat(decoded).isEqualTo("ç");
    }

    @Test
    @DisplayName("Decodifica escape hexadecimal UTF-8 com MSH-18 UNICODE UTF-8")
    void shouldDecodeHexEscapeWithMsh18UnicodeUtf8() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5||||||UNICODE UTF-8\r"
                + "PID|1||12345||TESTE^\\XC3A7\\\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        assertThat(message.get("PID-5.2")).isEqualTo("ç");
    }

    @Test
    @DisplayName("Decodifica escape hexadecimal ISO-8859-1 com MSH-18 8859/1")
    void shouldDecodeHexEscapeWithMsh18Iso88591() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5||||||8859/1\r"
                + "PID|1||12345||TESTE^\\XE7\\\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        assertThat(message.get("PID-5.2")).isEqualTo("ç");
    }

    @Test
    @DisplayName("Decodifica escape hexadecimal como UTF-8 quando a mensagem não tem MSH-18")
    void shouldDecodeHexEscapeAsUtf8WhenMsh18Absent() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345||TESTE^\\XC3A7\\\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        assertThat(message.get("PID-5.2")).isEqualTo("ç");
    }

    @Test
    @DisplayName("Mantém como texto literal o escape hexadecimal com número ímpar de dígitos")
    void shouldKeepOddLengthHexEscapeAsLiteral() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345||TESTE^\\XABC\\\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        assertThat(message.get("PID-5.2")).isEqualTo("\\XABC\\");
    }

    @Test
    @DisplayName("Preserva sequências de destaque e customizadas como texto")
    void shouldPreserveHighlightingAndCustomEscapes() {
        String raw = "\\H\\DESTAQUE\\N\\ E \\ZCUSTOM\\";
        String decoded = Hl7Escapes.decode(raw, Hl7Delimiters.DEFAULT);
        assertThat(decoded).isEqualTo("\\H\\DESTAQUE\\N\\ E \\ZCUSTOM\\");
    }

    @Test
    @DisplayName("Preserva todas as repetições de campo no modelo e na escrita")
    void shouldPreserveFieldRepetitionsInModelAndWriter() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345~67890~ABCDE||SILVA^JOAO\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();
        Hl7Field field = message.getSegment("PID").orElseThrow(IllegalStateException::new).getField(3);

        assertThat(field.getRepetitions()).hasSize(3);
        assertThat(field.getValues()).containsExactly("12345", "67890", "ABCDE");
        assertThat(message.get("PID.3[1]")).isEqualTo("12345");
        assertThat(message.get("PID.3[2]")).isEqualTo("67890");
        assertThat(message.get("PID.3[3]")).isEqualTo("ABCDE");

        Hl7Writer writer = new Hl7Writer(message.getDelimiters(), "\r");
        String written = writer.write(message);
        assertThat(written).contains("12345~67890~ABCDE");
    }

    @Test
    @DisplayName("Expõe repetições em lista no toRawJson")
    void shouldExposeRepetitionsInRawJson() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345~67890||SILVA^JOAO\r";

        String json = Hl7Handler.reader().from(hl7).toRawJson();
        assertThat(json)
                .contains("\"12345\"")
                .contains("\"67890\"");
    }

    @Test
    @DisplayName("message.get(\"PID\") devolve o segmento bruto serializado")
    void shouldReturnRawSegmentForSegmentQuery() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345||SILVA^JOAO\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();
        assertThat(message.get("PID")).isEqualTo("PID|1||12345||SILVA^JOAO");
    }

    @Test
    @DisplayName("Aceita caminhos flexíveis com hífen e repetição")
    void shouldAcceptFlexiblePathsWithHyphenAndRepetition() {
        String hl7 = "MSH|^~\\&|SIS|HOSP|||20261110||ADT^A01|MSG01|P|2.5\r"
                + "PID|1||12345~67890||SILVA^JOAO\r"
                + "OBX|1|NM|GLU||99\r"
                + "OBX|2|NM|HEM||14\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();
        assertThat(message.get("PID-5.1")).isEqualTo("SILVA");
        assertThat(message.get("PID-5.2")).isEqualTo("JOAO");
        assertThat(message.get("PID-3[2]")).isEqualTo("67890");
        assertThat(message.get("OBX[2]-5")).isEqualTo("14");
    }

    @Test
    @DisplayName("Realiza roundtrip idêntico nas amostras 2.1 a 2.7")
    void shouldPerformIdenticalRoundtripForCoreVersions() {
        assertRoundtrip("v21-adt.hl7");
        assertRoundtrip("v22-adt.hl7");
        assertRoundtrip("v23-adt.hl7");
        assertRoundtrip("v231-oru.hl7");
        assertRoundtrip("v24-adt.hl7");
        assertRoundtrip("v251-adt.hl7");
        assertRoundtrip("v26-mindray-oru.hl7");
        assertRoundtrip("v27-adt.hl7");

        String v25 = TestResources.read("/samples/sample-v25.hl7");
        Hl7Message message = Hl7Handler.reader().from(v25).toMessage();
        Hl7Writer writer = new Hl7Writer(message.getDelimiters(), "\r");
        assertThat(writer.write(message)).isEqualTo(normalizeSegmentTerminators(v25));
    }

    @Test
    @DisplayName("Realiza roundtrip idêntico em versões 2.8, 2.9 e casos de borda")
    void shouldPerformIdenticalRoundtripForEdgeCases() {
        assertRoundtrip("v28-adt.hl7");
        assertRoundtrip("v29-adt.hl7");
        assertRoundtrip("no-msh12-adt.hl7");
        assertRoundtrip("short-msh2-adt.hl7");
    }

    @Test
    @DisplayName("Mantém campos vazios no fim do segmento")
    void shouldKeepTrailingEmptyFields() {
        Hl7Message message = Hl7Handler.reader().from(EDGE_HEADER + "PID|1||||").toMessage();

        assertThat(message.getSegment("PID").get().getFields()).hasSize(5);
    }

    @Test
    @DisplayName("Mantém repetição vazia no meio do campo")
    void shouldKeepEmptyRepetitionInTheMiddle() {
        Hl7Message message = Hl7Handler.reader().from(EDGE_HEADER + "OBX|1|ST|CODE||a~~b").toMessage();
        Hl7Field field = message.getSegment("OBX").get().getField(5);

        assertThat(field.getRepetitions()).hasSize(3);
        assertThat(field.getRepetitions().get(1).get(0).getValue()).isEmpty();
    }

    @Test
    @DisplayName("Mantém subcomponente vazio após separador final")
    void shouldKeepTrailingEmptySubcomponent() {
        Hl7Message message = Hl7Handler.reader().from(EDGE_HEADER + "OBX|1|ST|x&").toMessage();
        Hl7Field field = message.getSegment("OBX").get().getField(3);

        assertThat(field.getComponents().get(0).getSubcomponents()).containsExactly("x", "");
    }

    @Test
    @DisplayName("Lê segmento que contém apenas o nome")
    void shouldParseSegmentWithOnlyName() {
        Hl7Message message = Hl7Handler.reader().from(EDGE_HEADER + "ZZZ").toMessage();

        assertThat(message.getSegment("ZZZ").get().getFields()).isEmpty();
    }

    @Test
    @DisplayName("Separa segmentos com quebras CRLF, CR e LF na mesma mensagem")
    void shouldSplitSegmentsWithMixedLineEndings() {
        String raw = EDGE_HEADER + "\r\nPID|1\rOBX|1|ST|A\nOBX|2|ST|B\r\n";

        Hl7Message message = Hl7Handler.reader().from(raw).toMessage();

        assertThat(message.getSegments()).extracting(Hl7Segment::getName).containsExactly("MSH", "PID", "OBX", "OBX");
    }

    private void assertRoundtrip(String fileName) {
        String original = sample(fileName);
        Hl7Message message = Hl7Handler.reader().from(original).toMessage();
        Hl7Writer writer = new Hl7Writer(message.getDelimiters(), "\r");
        String written = writer.write(message);
        assertThat(written).isEqualTo(normalizeSegmentTerminators(original));
    }
}
