package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.writer.Hl7Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7ParserFidelityTest {

    private static final String SAMPLES = "/samples/versions/";

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

    private void assertRoundtrip(String fileName) {
        String original = sample(fileName);
        Hl7Message message = Hl7Handler.reader().from(original).toMessage();
        Hl7Writer writer = new Hl7Writer(message.getDelimiters(), "\r");
        String written = writer.write(message);
        assertThat(written).isEqualTo(normalizeSegmentTerminators(original));
    }
}
