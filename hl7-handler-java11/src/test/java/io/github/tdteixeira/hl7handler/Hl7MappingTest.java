package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.mapping.Hl7MappingException;
import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.mapping.Hl7TypeConverter;
import io.github.tdteixeira.hl7handler.mapping.Hl7VersionRange;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7MappingTest {

    @Test
    @DisplayName("Deve transformar mensagem laboratorial com múltiplos segmentos OBX em lista estruturada")
    void shouldTransformRepeatedObxSegmentsIntoList() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ORU^R01|MSG001|P|2.5\r"
                + "PID|1||12345^^^HOSP^MR||SILVA^MARIA||19900101|F\r"
                + "OBX|1|NM|GLU^GLICOSE^LN||95|mg/dL|70-99|N|||F\r"
                + "OBX|2|NM|HEMO^HEMOGLOBINA^LN||14.2|g/dL|12-16|N|||F\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> itemTemplate = new LinkedHashMap<>();
        itemTemplate.put("codigo", "OBX.3.1");
        itemTemplate.put("descricao", "OBX.3.2");

        Map<String, Object> valorNode = new LinkedHashMap<>();
        valorNode.put("hl7", "OBX.5");
        valorNode.put("tipo", "numero");
        itemTemplate.put("valor", valorNode);
        itemTemplate.put("unidade", "OBX.6.1");

        Map<String, Object> examesList = new LinkedHashMap<>();
        examesList.put("lista", "OBX[*]");
        examesList.put("item", itemTemplate);

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("exames", examesList);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result).containsKey("exames");
        assertThat(result.get("exames")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> exames = (List<Map<String, Object>>) result.get("exames");
        assertThat(exames).hasSize(2);

        Map<String, Object> exame1 = exames.get(0);
        assertThat(exame1.get("codigo")).isEqualTo("GLU");
        assertThat(exame1.get("descricao")).isEqualTo("GLICOSE");
        assertThat(exame1.get("valor")).isEqualTo(95L);
        assertThat(exame1.get("unidade")).isEqualTo("mg/dL");

        Map<String, Object> exame2 = exames.get(1);
        assertThat(exame2.get("codigo")).isEqualTo("HEMO");
        assertThat(exame2.get("descricao")).isEqualTo("HEMOGLOBINA");
        assertThat(exame2.get("valor")).isEqualTo(14.2);
        assertThat(exame2.get("unidade")).isEqualTo("g/dL");
    }

    @Test
    @DisplayName("Deve converter tipos corretamente para Temporal, Long/Double e Boolean")
    void shouldConvertTypesCorrectly() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ORU^R01|MSG001|P|2.5\r"
                + "PID|1||12345||SILVA^MARIA||19900520|F||||||||||||||||||||||S\r"
                + "OBX|1|NM|GLU||95\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();

        Map<String, Object> nascimento = new LinkedHashMap<>();
        nascimento.put("hl7", "PID.7");
        nascimento.put("tipo", "data");
        mapping.put("nascimento", nascimento);

        Map<String, Object> timestamp = new LinkedHashMap<>();
        timestamp.put("hl7", "MSH.7");
        timestamp.put("tipo", "data");
        mapping.put("timestamp", timestamp);

        Map<String, Object> valor = new LinkedHashMap<>();
        valor.put("hl7", "OBX.5");
        valor.put("tipo", "numero");
        mapping.put("valor", valor);

        Map<String, Object> ativo = new LinkedHashMap<>();
        ativo.put("hl7", "PID.30");
        ativo.put("tipo", "booleano");
        mapping.put("ativo", ativo);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("nascimento")).isInstanceOf(Temporal.class);
        assertThat(result.get("nascimento")).isEqualTo(LocalDate.of(1990, 5, 20));

        assertThat(result.get("timestamp")).isInstanceOf(Temporal.class);
        assertThat(result.get("timestamp")).isEqualTo(LocalDateTime.of(2026, 11, 10, 12, 0, 0));

        assertThat(result.get("valor")).isEqualTo(95L);
        assertThat(result.get("ativo")).isEqualTo(Boolean.TRUE);
    }

    @Test
    @DisplayName("Deve aplicar sobreposição porVersao respeitando faixas e maior especificidade")
    void shouldApplyVersionOverlayForLegacyVsModern() {
        String hl7v25 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT^A01^ADT_A01|CTRL25|P|2.5\r"
                + "PID|1|EXT25|INT25||SILVA^JOSE\r";
        String hl7v23 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT^A01|CTRL23|P|2.3\r"
                + "PID|1|EXT23|INT23||SILVA^JOSE\r";
        String hl7v21 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT|CTRL21|P|2.1\r"
                + "EVN|A01|20261110\r"
                + "PID|1|EXT21|INT21||SILVA^JOSE\r";

        Hl7Message msg25 = Hl7Handler.reader().from(hl7v25).toMessage();
        Hl7Message msg23 = Hl7Handler.reader().from(hl7v23).toMessage();
        Hl7Message msg21 = Hl7Handler.reader().from(hl7v21).toMessage();

        Map<String, Object> baseMappings = new LinkedHashMap<>();
        baseMappings.put("pacienteId", "PID.3.1");
        baseMappings.put("evento", "MSH.9.2");

        Map<String, Object> porVersao = new LinkedHashMap<>();
        Map<String, Object> legacyOverlay = new LinkedHashMap<>();
        legacyOverlay.put("pacienteId", "PID.2.1");
        porVersao.put("<2.3.1", legacyOverlay);

        Map<String, Object> v21Overlay = new LinkedHashMap<>();
        v21Overlay.put("evento", "EVN.1");
        porVersao.put("2.1", v21Overlay);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("mappings", baseMappings);
        config.put("porVersao", porVersao);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();

        // 2.5 uses base mappings
        Map<String, Object> res25 = transformer.transform(msg25, config);
        assertThat(res25.get("pacienteId")).isEqualTo("INT25");
        assertThat(res25.get("evento")).isEqualTo("A01");

        // 2.3 uses <2.3.1 overlay for pacienteId, base for evento
        Map<String, Object> res23 = transformer.transform(msg23, config);
        assertThat(res23.get("pacienteId")).isEqualTo("EXT23");
        assertThat(res23.get("evento")).isEqualTo("A01");

        // 2.1 matches both <2.3.1 and 2.1: 2.1 is more specific for evento, <2.3.1 supplies pacienteId
        Map<String, Object> res21 = transformer.transform(msg21, config);
        assertThat(res21.get("pacienteId")).isEqualTo("EXT21");
        assertThat(res21.get("evento")).isEqualTo("A01");
    }

    @Test
    @DisplayName("Campos ausentes devem retornar null por padrão")
    void shouldReturnNullForMissingFieldsByDefault() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ADT^A01|MSG001|P|2.5\r"
                + "PID|1||12345||SILVA^MARIA\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("prontuario", "PID.3.1");
        mapping.put("campoAusente", "PID.99");

        Map<String, Object> objetoAusente = new LinkedHashMap<>();
        objetoAusente.put("hl7", "PV1.10");
        mapping.put("objetoAusente", objetoAusente);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("prontuario")).isEqualTo("12345");
        assertThat(result).containsKey("campoAusente");
        assertThat(result.get("campoAusente")).isNull();
        assertThat(result).containsKey("objetoAusente");
        assertThat(result.get("objetoAusente")).isNull();
    }

    @Test
    @DisplayName("Campos ausentes devem retornar string vazia quando keepEmptyStrings for true")
    void shouldKeepEmptyStringsWhenConfigured() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ADT^A01|MSG001|P|2.5\r"
                + "PID|1||12345||SILVA^MARIA\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("prontuario", "PID.3.1");
        mapping.put("campoAusente", "PID.99");

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer(true);
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("prontuario")).isEqualTo("12345");
        assertThat(result.get("campoAusente")).isEqualTo("");
    }

    @Test
    @DisplayName("Deve preencher constantes estáticas definidas via valor")
    void shouldPopulateStaticConstants() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ADT^A01|MSG001|P|2.5\r"
                + "PID|1||12345||SILVA^MARIA\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();
        Map<String, Object> hospitalNode = new LinkedHashMap<>();
        hospitalNode.put("valor", "HUSM");
        mapping.put("hospital", hospitalNode);

        Map<String, Object> setorNode = new LinkedHashMap<>();
        setorNode.put("valor", 100);
        mapping.put("setor", setorNode);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("hospital")).isEqualTo("HUSM");
        assertThat(result.get("setor")).isEqualTo(100);
    }

    @Test
    @DisplayName("Deve validar faixa versoes e lançar Hl7MappingException quando a versão não for suportada")
    void shouldValidateSupportedVersionsRange() {
        String hl7v21 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT|CTRL21|P|2.1\r"
                + "PID|1|EXT21|INT21||SILVA^JOSE\r";
        String hl7v25 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT^A01|CTRL25|P|2.5\r"
                + "PID|1|EXT25|INT25||SILVA^JOSE\r";

        Hl7Message msg21 = Hl7Handler.reader().from(hl7v21).toMessage();
        Hl7Message msg25 = Hl7Handler.reader().from(hl7v25).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("versoes", ">=2.3");
        mapping.put("mappings", Collections.singletonMap("id", "MSH.10"));

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();

        assertThatThrownBy(() -> transformer.transform(msg21, mapping))
                .isInstanceOf(Hl7MappingException.class)
                .hasMessageContaining("2.1")
                .hasMessageContaining(">=2.3");

        Map<String, Object> result25 = transformer.transform(msg25, mapping);
        assertThat(result25.get("id")).isEqualTo("CTRL25");
    }

    @Test
    @DisplayName("Deve transformar repetições de campo PID.13[*] em lista com template de item")
    void shouldTransformFieldRepetitionsIntoList() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110120000||ADT^A01|MSG001|P|2.5\r"
                + "PID|1||12345||SILVA^MARIA||||||||(11)99999-1111^CEL~(11)88888-2222^RES\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> itemTemplate = new LinkedHashMap<>();
        itemTemplate.put("numero", "PID.13[*].1");
        itemTemplate.put("uso", "PID.13[*].2");

        Map<String, Object> telefonesList = new LinkedHashMap<>();
        telefonesList.put("lista", "PID.13[*]");
        telefonesList.put("item", itemTemplate);

        Map<String, Object> mapping = new LinkedHashMap<>();
        mapping.put("telefones", telefonesList);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("telefones")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> telefones = (List<Map<String, Object>>) result.get("telefones");
        assertThat(telefones).hasSize(2);

        assertThat(telefones.get(0).get("numero")).isEqualTo("(11)99999-1111");
        assertThat(telefones.get(0).get("uso")).isEqualTo("CEL");

        assertThat(telefones.get(1).get("numero")).isEqualTo("(11)88888-2222");
        assertThat(telefones.get(1).get("uso")).isEqualTo("RES");
    }

    @Test
    @DisplayName("Deve usar caminho de fallback caso o hl7 primário venha vazio")
    void shouldUseFallbackWhenPrimaryFieldIsEmpty() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110||ADT|MSG001|P|2.5\r"
                + "EVN|A01|20261110\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("hl7", "MSH.9.2");
        evento.put("fallback", "EVN.1");
        mapping.put("evento", evento);

        Map<String, Object> literal = new LinkedHashMap<>();
        literal.put("hl7", "PID.99");
        literal.put("fallback", "VALOR_PADRAO");
        mapping.put("literal", literal);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, mapping);

        assertThat(result.get("evento")).isEqualTo("A01");
        assertThat(result.get("literal")).isEqualTo("VALOR_PADRAO");
    }

    @Test
    @DisplayName("Deve suportar conversor de tipo customizado registrado")
    void shouldSupportCustomTypeConverters() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110||ORU^R01|MSG001|P|2.5\r"
                + "OBX|1|NM|TEMP||36.5\r";

        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        transformer.registerConverter("celsius", raw -> raw + " °C");

        Map<String, Object> mapping = new LinkedHashMap<>();
        Map<String, Object> tempNode = new LinkedHashMap<>();
        tempNode.put("hl7", "OBX.5");
        tempNode.put("tipo", "celsius");
        mapping.put("temperatura", tempNode);

        Map<String, Object> result = transformer.transform(message, mapping);
        assertThat(result.get("temperatura")).isEqualTo("36.5 °C");
    }

    @Test
    @DisplayName("Deve lançar Hl7MappingException quando o tipo for desconhecido")
    void shouldThrowMappingExceptionOnUnknownType() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110||ADT^A01|MSG001|P|2.5\r";
        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> mapping = new LinkedHashMap<>();
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("hl7", "MSH.10");
        node.put("tipo", "desconhecido");
        mapping.put("campo", node);

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();

        assertThatThrownBy(() -> transformer.transform(message, mapping))
                .isInstanceOf(Hl7MappingException.class)
                .hasMessageContaining("desconhecido");
    }

    @Test
    @DisplayName("Deve ignorar metadados de topo no mapa de saída")
    void shouldIgnoreTopLevelMetadataKeys() {
        String hl7 = "MSH|^~\\&|LAB|HOSP|REC|FAC|20261110||ADT^A01|MSG001|P|2.5\r";
        Hl7Message message = Hl7Handler.reader().from(hl7).toMessage();

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("version", "2.5");
        config.put("description", "Exemplo de mapeamento");
        config.put("versoes", ">=2.0");
        config.put("porVersao", new HashMap<>());
        config.put("mensagemId", "MSH.10");

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();
        Map<String, Object> result = transformer.transform(message, config);

        assertThat(result).containsKey("mensagemId");
        assertThat(result.get("mensagemId")).isEqualTo("MSG001");
        assertThat(result).doesNotContainKeys("version", "description", "versoes", "porVersao");
    }

    @Test
    @DisplayName("Hl7VersionRange calcula especificidade corretamente")
    void shouldCalculateSpecificityInHl7VersionRange() {
        Hl7VersionRange all = Hl7VersionRange.parse("*");
        Hl7VersionRange majorWildcard = Hl7VersionRange.parse("2.x");
        Hl7VersionRange openLower = Hl7VersionRange.parse(">=2.3");
        Hl7VersionRange openUpperWithPatch = Hl7VersionRange.parse("<2.3.1");
        Hl7VersionRange bounded = Hl7VersionRange.parse(">=2.3, <2.7");
        Hl7VersionRange exact = Hl7VersionRange.parse("2.1");
        Hl7VersionRange exactWithPatch = Hl7VersionRange.parse("2.3.1");

        assertThat(all.getSpecificity()).isLessThan(majorWildcard.getSpecificity());
        assertThat(majorWildcard.getSpecificity()).isLessThan(openLower.getSpecificity());
        assertThat(openLower.getSpecificity()).isLessThan(openUpperWithPatch.getSpecificity());
        assertThat(openUpperWithPatch.getSpecificity()).isLessThan(bounded.getSpecificity());
        assertThat(bounded.getSpecificity()).isLessThan(exact.getSpecificity());
        assertThat(exact.getSpecificity()).isLessThan(exactWithPatch.getSpecificity());
    }

    @Test
    @DisplayName("Hl7VersionRange avalia adequadamente operadores de comparação")
    void shouldMatchVersionRangeExpressions() {
        Hl7VersionRange range = Hl7VersionRange.parse(">=2.3, <=2.6");

        assertThat(range.matches(Hl7Version.V2_1)).isFalse();
        assertThat(range.matches(Hl7Version.V2_3)).isTrue();
        assertThat(range.matches(Hl7Version.V2_5)).isTrue();
        assertThat(range.matches(Hl7Version.V2_6)).isTrue();
        assertThat(range.matches(Hl7Version.V2_7)).isFalse();
    }
}
