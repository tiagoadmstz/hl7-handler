package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.v3.version.AbstractHl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import io.github.tiagoadmstz.hl7handler.v3.version.cda.CcdaStrategy;
import io.github.tiagoadmstz.hl7handler.v3.version.cda.CdaR2Strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Estratégia C-CDA sobre documentos CDA R2")
class CcdaStrategyTest {

    private static final String R21_PATH = "/samples/v3/ccda-r21-ccd.xml";
    private static final String R11_PATH = "/samples/v3/ccda-r11-sem-extension.xml";
    private static final String CDA_PATH = "/samples/v3/cda-r2-sumario-alta.xml";
    private static final String LOINC_WARNING = "C-CDA code/@codeSystem is not LOINC (2.16.840.1.113883.6.1)";
    private static final String RECORD_TARGET_WARNING = "C-CDA document has no recordTarget";
    private static final String LANGUAGE_WARNING = "C-CDA document has no languageCode";
    private static final String CODE_SYSTEM_LOINC_R21 =
        "<code code=\"34133-9\" codeSystem=\"2.16.840.1.113883.6.1\" displayName=\"Resumo de cuidados\"/>";
    private static final String CODE_SYSTEM_NOT_LOINC =
        "<code code=\"34133-9\" codeSystem=\"2.16.840.1.113883.5.25\" displayName=\"Resumo de cuidados\"/>";

    private static Hl7V3Document read(String xml) {
        return Hl7Handler.v3().build().read(xml).document();
    }

    private static String replaced(String xml, String target, String replacement) {
        String result = xml.replace(target, replacement);
        assertThat(result).isNotEqualTo(xml);
        return result;
    }

    private static String withoutMatch(String xml, String regex) {
        String result = xml.replaceFirst(regex, "");
        assertThat(result).isNotEqualTo(xml);
        return result;
    }

    private static final class CustomClinicalDocumentStrategy extends AbstractHl7V3Strategy {

        CustomClinicalDocumentStrategy() {
            super(Hl7V3Format.CDA, null);
        }

        @Override
        public boolean supports(Element root) {
            return "ClinicalDocument".equals(root.getLocalName());
        }

        @Override
        public String versionIdentifier(Hl7V3Document document) {
            return "CUSTOM-CCDA";
        }

        @Override
        public List<String> validate(Hl7V3Document document) {
            return Collections.emptyList();
        }
    }

    @Test
    @DisplayName("Deve detectar C-CDA-R2.1 pela extension 2015-08-01 do templateId do US Realm Header")
    void shouldDetectCcdaR21FromRealmHeaderExtension() {
        Hl7V3Document document = read(V3TestResources.read(R21_PATH));

        assertThat(document.strategy()).isInstanceOf(CcdaStrategy.class);
        assertThat(document.format()).isEqualTo(Hl7V3Format.CDA);
        assertThat(document.versionIdentifier()).isEqualTo("C-CDA-R2.1");
    }

    @Test
    @DisplayName("Deve detectar C-CDA-R2.0 pela extension 2014-06-09 do templateId do US Realm Header")
    void shouldDetectCcdaR20FromRealmHeaderExtension() {
        String xml = replaced(V3TestResources.read(R21_PATH), "2015-08-01", "2014-06-09");

        Hl7V3Document document = read(xml);

        assertThat(document.strategy()).isInstanceOf(CcdaStrategy.class);
        assertThat(document.versionIdentifier()).isEqualTo("C-CDA-R2.0");
    }

    @Test
    @DisplayName("Deve detectar C-CDA-R1.1 quando o templateId do US Realm Header não tem extension")
    void shouldDetectCcdaR11WithoutExtension() {
        Hl7V3Document document = read(V3TestResources.read(R11_PATH));

        assertThat(document.strategy()).isInstanceOf(CcdaStrategy.class);
        assertThat(document.versionIdentifier()).isEqualTo("C-CDA-R1.1");
    }

    @Test
    @DisplayName("Deve devolver C-CDA genérico quando a extension do US Realm Header é desconhecida")
    void shouldReturnGenericCcdaForUnknownExtension() {
        String xml = replaced(V3TestResources.read(R21_PATH), "2015-08-01", "2099-01-01");

        assertThat(read(xml).versionIdentifier()).isEqualTo("C-CDA");
    }

    @Test
    @DisplayName("Deve expor as coordenadas do C-CDA além das coordenadas do CDA")
    void shouldExposeCcdaCoordinates() {
        Hl7V3Document document = read(V3TestResources.read(R21_PATH));

        assertThat(document.get("#documentTemplateId")).isEqualTo("2.16.840.1.113883.10.20.22.1.2");
        assertThat(document.get("#patientGiven")).isEqualTo("Maria");
        assertThat(document.get("#patientFamily")).isEqualTo("Exemplo");
        assertThat(document.get("#patientBirthTime")).isEqualTo("19800115");
        assertThat(document.get("#patientGender")).isEqualTo("F");
        assertThat(document.get("#authorTime")).isEqualTo("20261109143000-0300");
        assertThat(document.get("#custodianName")).isEqualTo("Hospital Ficticio");
        assertThat(document.get("#documentId")).isEqualTo("CCD-0001");
        assertThat(document.get("#patientId")).isEqualTo("700000000000002");
    }

    @Test
    @DisplayName("Deve não emitir avisos para um C-CDA completo, com LOINC, recordTarget e languageCode")
    void shouldNotWarnForCompleteCcda() {
        Hl7V3Document document = read(V3TestResources.read(R21_PATH));

        assertThat(document.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve avisar quando o codeSystem do documento não é LOINC")
    void shouldWarnWhenCodeSystemIsNotLoinc() {
        String xml = replaced(V3TestResources.read(R21_PATH), CODE_SYSTEM_LOINC_R21, CODE_SYSTEM_NOT_LOINC);

        assertThat(read(xml).warnings()).contains(LOINC_WARNING);
    }

    @Test
    @DisplayName("Deve avisar quando o documento não tem recordTarget")
    void shouldWarnWhenRecordTargetIsMissing() {
        String xml = withoutMatch(V3TestResources.read(R21_PATH), "(?s)<recordTarget>.*?</recordTarget>");

        assertThat(read(xml).warnings()).contains(RECORD_TARGET_WARNING);
    }

    @Test
    @DisplayName("Deve avisar quando o documento não tem languageCode")
    void shouldWarnWhenLanguageCodeIsMissing() {
        String xml = withoutMatch(V3TestResources.read(R21_PATH), "<languageCode code=\"pt-BR\"/>");

        assertThat(read(xml).warnings()).contains(LANGUAGE_WARNING);
    }

    @Test
    @DisplayName("Deve manter o CDA sem header C-CDA com a versão CDA-R2 e sem avisos de C-CDA")
    void shouldKeepPlainCdaAsCdaR2() {
        Hl7V3Document document = read(V3TestResources.read(CDA_PATH));

        assertThat(document.strategy()).isInstanceOf(CdaR2Strategy.class);
        assertThat(document.strategy()).isNotInstanceOf(CcdaStrategy.class);
        assertThat(document.versionIdentifier()).isEqualTo("CDA-R2");
        assertThat(document.warnings()).noneMatch(warning -> warning.startsWith("C-CDA"));
    }

    @Test
    @DisplayName("Deve dar prioridade à estratégia customizada registrada sobre a estratégia C-CDA")
    void shouldPreferCustomStrategyOverCcda() {
        Hl7V3StrategyRegistry registry = Hl7V3StrategyRegistry.defaultRegistry();
        Hl7V3Strategy custom = new CustomClinicalDocumentStrategy();
        registry.register(custom);

        Hl7V3Document document = Hl7Handler.v3().strategyRegistry(registry).build()
            .read(V3TestResources.read(R21_PATH)).document();

        assertThat(document.strategy()).isSameAs(custom);
        assertThat(document.versionIdentifier()).isEqualTo("CUSTOM-CCDA");
    }

    @Test
    @DisplayName("Deve listar a estratégia C-CDA antes da CDA R2 no registro padrão")
    void shouldListCcdaBeforeCdaInDefaultRegistry() {
        List<Hl7V3Strategy> strategies = new ArrayList<>(Hl7V3StrategyRegistry.defaultRegistry().strategies());

        assertThat(strategies.get(0)).isInstanceOf(CcdaStrategy.class);
        assertThat(strategies.get(1)).isInstanceOf(CdaR2Strategy.class);
    }
}
