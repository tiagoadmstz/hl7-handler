package io.github.tiagoadmstz.hl7handler;

import io.github.tiagoadmstz.hl7handler.api.Hl7Processor;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TemperatureUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Mapeamento pelo tipo do OBX-2 e conversão de temperatura")
class Hl7TypeByAndTemperatureTest {

    private static final String HEADER = "MSH|^~\\&|LAB_FICT|FAC_FICT|||20261119120000||ORU^R01|MSG001|P|2.6\r"
            + "PID|1||12345^^^FICT^MR||SILVA^MARIA\r";

    private static String oru(String... obxSegments) {
        StringBuilder message = new StringBuilder(HEADER);
        for (String obx : obxSegments) {
            message.append(obx).append('\r');
        }
        return message.toString();
    }

    private static String mapping(String cabecalho, String... itens) {
        StringBuilder yaml = new StringBuilder(cabecalho);
        yaml.append("mappings:\n  sinais:\n    lista: \"OBX[*]\"\n    item:\n");
        for (String item : itens) {
            yaml.append("      ").append(item).append('\n');
        }
        return yaml.toString();
    }

    private static Map<?, ?> primeiroSinal(String yaml, Hl7TemperatureUnit unidadeBuilder, String message) {
        Hl7Processor processor = Hl7Handler.builder().temperatureUnit(unidadeBuilder).withMappingContent(yaml).build();
        List<?> sinais = (List<?>) processor.read(message).toMap().get("sinais");
        return (Map<?, ?>) sinais.get(0);
    }

    private static Object valorDoPrimeiroSinal(String yaml, String message) {
        return primeiroSinal(yaml, null, message).get("valor");
    }

    @Test
    @DisplayName("Deve converter em número o valor de um OBX NM pelo tipo do OBX-2")
    void shouldConvertNmValueToNumberByTypeCoordinate() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.2);
    }

    @Test
    @DisplayName("Deve manter o campo inteiro de um OBX CNE como texto pelo tipo do OBX-2")
    void shouldKeepCneValueAsWholeTextByTypeCoordinate() {
        String message = oru("OBX|1|CNE|132^FICT_NIBP_POS^99FICT|1|60001^FICT_POS_SITTING^99FICT|262656^MDC_DIM_DIMLESS^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo("60001^FICT_POS_SITTING^99FICT");
    }

    @Test
    @DisplayName("Deve manter o valor de um OBX ST como texto pelo tipo do OBX-2")
    void shouldKeepStValueAsTextByTypeCoordinate() {
        String message = oru("OBX|1|ST|545^FICT_SYSTEM^99FICT|1|NEWS2|262656^MDC_DIM_DIMLESS^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo("NEWS2");
    }

    @Test
    @DisplayName("Deve converter em data/hora o valor de um OBX TS pelo tipo do OBX-2")
    void shouldConvertTsValueToDateTimeByTypeCoordinate() {
        String message = oru("OBX|1|TS|777^FICT_TIME^99FICT|1|20261119063158");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isInstanceOf(Temporal.class);
    }

    @Test
    @DisplayName("Deve sobrepor a tabela padrão com o mapa tipos")
    void shouldOverrideDefaultTableWithTiposMap() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\", tipos: { NM: texto } }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo("37.2");
    }

    @Test
    @DisplayName("Deve aceitar o código do tipo em minúsculas no mapa tipos e estender a tabela padrão")
    void shouldApplyTiposKeyCaseInsensitivelyAndExtendDefaultTable() {
        String message = oru("OBX|1|ST|545^FICT_COUNT^99FICT|1|12|262656^MDC_DIM_DIMLESS^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\", tipos: { st: numero } }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(12L);
    }

    @Test
    @DisplayName("Deve lançar Hl7MappingException quando o valor de um OBX NM não é número")
    void shouldFailWhenNmValueIsNotNumber() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|ABC|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThatThrownBy(() -> valorDoPrimeiroSinal(yaml, message))
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("ABC");
    }

    @Test
    @DisplayName("Deve devolver null para valor vazio de tipo texto pelo tipo do OBX-2")
    void shouldReturnNullForEmptyTextValueByTypeCoordinate() {
        String message = oru("OBX|1|CNE|132^FICT_NIBP_POS^99FICT|1||");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isNull();
    }

    @Test
    @DisplayName("Deve devolver string vazia para valor vazio de tipo texto com keepEmptyStrings")
    void shouldReturnEmptyStringForEmptyTextValueWithKeepEmptyStrings() {
        String message = oru("OBX|1|CNE|132^FICT_NIBP_POS^99FICT|1||");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\" }");
        Hl7Processor processor = Hl7Handler.builder().keepEmptyStrings(true).withMappingContent(yaml).build();

        List<?> sinais = (List<?>) processor.read(message).toMap().get("sinais");

        assertThat(((Map<?, ?>) sinais.get(0)).get("valor")).isEqualTo("");
    }

    @Test
    @DisplayName("Deve rejeitar tipo e tipoPor juntos na validação do mapeamento")
    void shouldRejectTipoAndTipoPorTogether() {
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\", tipo: numero }");

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(yaml).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("sinais.item.valor")
            .hasMessageContaining("'tipo' and 'tipoPor'");
    }

    @Test
    @DisplayName("Deve rejeitar conversor desconhecido no mapa tipos na validação do mapeamento")
    void shouldRejectUnknownConverterInTiposMap() {
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\", tipos: { NM: inexistente } }");

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(yaml).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("sinais.item.valor.tipos")
            .hasMessageContaining("inexistente");
    }

    @Test
    @DisplayName("Deve rejeitar o mapa tipos sem tipoPor na validação do mapeamento")
    void shouldRejectTiposWithoutTipoPor() {
        String yaml = mapping("", "valor: { hl7: \"5\", tipos: { NM: numero } }");

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(yaml).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("sinais.item.valor")
            .hasMessageContaining("'tipos' requires 'tipoPor'");
    }

    @Test
    @DisplayName("Deve rejeitar temperatura inválida no metadado do YAML")
    void shouldRejectInvalidTemperatureUnitInYaml() {
        String yaml = mapping("temperatura: kelvin\n", "valor: { hl7: \"5\", tipoPor: \"2\" }");

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(yaml).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("temperatura")
            .hasMessageContaining("kelvin");
    }

    @Test
    @DisplayName("Deve rejeitar ajustarUnidade que não seja booleano na validação do mapeamento")
    void shouldRejectNonBooleanAdjustUnit() {
        String yaml = mapping("", "unidade: { hl7: \"6.2\", ajustarUnidade: \"sim\" }");

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(yaml).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("ajustarUnidade");
    }

    @Test
    @DisplayName("Deve converter temperatura de Fahrenheit para Celsius pelo código MDC")
    void shouldConvertFahrenheitToCelsiusWithMdcCode() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|99.0|266560^MDC_DIM_FAHR^MDC");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.2);
    }

    @Test
    @DisplayName("Deve converter temperatura de Celsius para Fahrenheit pelo nome MDC")
    void shouldConvertCelsiusToFahrenheitWithMdcName() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|MDC_DIM_DEGC^^MDC");
        String yaml = mapping("temperatura: fahrenheit\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(99.0);
    }

    @Test
    @DisplayName("Deve converter temperatura de Fahrenheit para Celsius pela unidade UCUM")
    void shouldConvertFahrenheitToCelsiusWithUcumUnit() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|99.0|[degF]^^UCUM");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.2);
    }

    @Test
    @DisplayName("Deve converter temperatura de Celsius para Fahrenheit pela unidade UCUM")
    void shouldConvertCelsiusToFahrenheitWithUcumUnit() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|Cel^^UCUM");
        String yaml = mapping("temperatura: fahrenheit\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(99.0);
    }

    @Test
    @DisplayName("Deve converter temperatura de Fahrenheit para Celsius pelo símbolo")
    void shouldConvertFahrenheitToCelsiusWithSymbol() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|99.0|°F");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.2);
    }

    @Test
    @DisplayName("Deve converter temperatura de Celsius para Fahrenheit pelo símbolo")
    void shouldConvertCelsiusToFahrenheitWithSymbol() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|°C");
        String yaml = mapping("temperatura: fahrenheit\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(99.0);
    }

    @Test
    @DisplayName("Deve trocar o nome da unidade pela unidade escolhida quando ajustarUnidade é verdadeiro")
    void shouldAdjustUnitNameToChosenTemperatureUnit() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("temperatura: fahrenheit\n",
            "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }",
            "unidade: { hl7: \"6.2\", ajustarUnidade: true }");

        Map<?, ?> sinal = primeiroSinal(yaml, null, message);

        assertThat(sinal.get("valor")).isEqualTo(99.0);
        assertThat(sinal.get("unidade")).isEqualTo("MDC_DIM_FAHR");
    }

    @Test
    @DisplayName("Deve manter intacta a unidade que não é de temperatura")
    void shouldKeepNonTemperatureUnitIntact() {
        String message = oru("OBX|1|NM|150021^MDC_PRESS_BLD_NONINV_SYS^MDC|1|119|266016^MDC_DIM_MMHG^MDC");
        String yaml = mapping("temperatura: fahrenheit\n",
            "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }",
            "unidade: { hl7: \"6.2\", ajustarUnidade: true }");

        Map<?, ?> sinal = primeiroSinal(yaml, null, message);

        assertThat(sinal.get("valor")).isEqualTo(119L);
        assertThat(sinal.get("unidade")).isEqualTo("MDC_DIM_MMHG");
    }

    @Test
    @DisplayName("Deve manter o valor quando a conversão de temperatura não está ligada")
    void shouldKeepValueWhenTemperatureConversionIsDisabled() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.2);
    }

    @Test
    @DisplayName("Deve usar a unidade do builder antes da unidade do YAML")
    void shouldPreferBuilderTemperatureUnitOverYamlUnit() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        Map<?, ?> sinal = primeiroSinal(yaml, Hl7TemperatureUnit.FAHRENHEIT, message);

        assertThat(sinal.get("valor")).isEqualTo(99.0);
    }

    @Test
    @DisplayName("Deve aceitar a chave temperature em inglês no metadado do YAML")
    void shouldAcceptEnglishTemperatureKeyInYaml() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|37.2|268192^MDC_DIM_DEGC^MDC");
        String yaml = mapping("temperature: fahrenheit\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(99.0);
    }

    @Test
    @DisplayName("Deve arredondar HALF_UP com pelo menos uma casa decimal")
    void shouldRoundHalfUpWithAtLeastOneDecimal() {
        String message = oru("OBX|1|NM|188428^MDC_TEMP_EAR^MDC|1|100|266560^MDC_DIM_FAHR^MDC");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo(37.8);
    }

    @Test
    @DisplayName("Deve manter o texto de um valor não numérico com unidadePor")
    void shouldKeepTextValueUnchangedWithUnitByCoordinate() {
        String message = oru("OBX|1|CNE|132^FICT_NIBP_POS^99FICT|1|60001^FICT_POS_SITTING^99FICT|266560^MDC_DIM_FAHR^MDC");
        String yaml = mapping("temperatura: celsius\n", "valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }");

        assertThat(valorDoPrimeiroSinal(yaml, message)).isEqualTo("60001^FICT_POS_SITTING^99FICT");
    }

    @Test
    @DisplayName("Deve reconhecer o nome da unidade de temperatura sem diferenciar maiúsculas")
    void shouldResolveTemperatureUnitNameIgnoringCase() {
        assertThat(Hl7TemperatureUnit.fromName(" Fahrenheit ")).isEqualTo(Hl7TemperatureUnit.FAHRENHEIT);
    }

    @Test
    @DisplayName("Deve devolver null para nome de unidade de temperatura desconhecido")
    void shouldReturnNullForUnknownTemperatureUnitName() {
        assertThat(Hl7TemperatureUnit.fromName("kelvin")).isNull();
    }
}
