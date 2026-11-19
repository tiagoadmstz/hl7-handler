package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Parser;
import io.github.tiagoadmstz.hl7handler.v3.version.AbstractHl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import io.github.tiagoadmstz.hl7handler.v3.version.cda.CdaR2Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.message.Hl7V3MessageStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7V3StrategyTest {

    private static final String CDA_TEMPLATE = "2.16.840.1.113883.10.20.22.1.8";
    private static final String OTHER_TEMPLATE = "2.16.840.1.113883.10.20.22.1.99";

    private static final class TemplateStrategy extends AbstractHl7V3Strategy {

        private final String templateRoot;

        TemplateStrategy(String templateRoot) {
            super(Hl7V3Format.CDA, Collections.singletonMap("title", "hl7:title"));
            this.templateRoot = templateRoot;
        }

        @Override
        public boolean supports(Element root) {
            if (!"ClinicalDocument".equals(root.getLocalName())) {
                return false;
            }
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element
                        && "templateId".equals(child.getLocalName())
                        && templateRoot.equals(((Element) child).getAttribute("root"))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String versionIdentifier(Hl7V3Document document) {
            return "CDA-SUMARIO";
        }

        @Override
        public List<String> validate(Hl7V3Document document) {
            return Collections.emptyList();
        }
    }

    private static Hl7V3Document parseText(String xml) {
        return new Hl7V3Parser(Hl7V3StrategyRegistry.defaultRegistry(), new Hl7V3Namespaces(null))
            .parse(new InputSource(new StringReader(xml)));
    }

    @Test
    @DisplayName("Deve escolher a estratégia CDA para a raiz ClinicalDocument")
    void shouldChooseCdaStrategyForClinicalDocument() {
        Element root = V3TestResources.root(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        Hl7V3Strategy strategy = Hl7V3StrategyRegistry.defaultRegistry().find(root).orElseThrow(IllegalStateException::new);

        assertThat(strategy).isInstanceOf(CdaR2Strategy.class);
        assertThat(strategy.format()).isEqualTo(Hl7V3Format.CDA);
    }

    @Test
    @DisplayName("Deve escolher a estratégia de mensagem para uma raiz com interactionId")
    void shouldChooseMessageStrategyForInteractionIdChild() {
        Element root = V3TestResources.root(V3TestResources.read("/samples/v3/prpa-in201301uv02.xml"));

        Hl7V3Strategy strategy = Hl7V3StrategyRegistry.defaultRegistry().find(root).orElseThrow(IllegalStateException::new);

        assertThat(strategy).isInstanceOf(Hl7V3MessageStrategy.class);
        assertThat(strategy.format()).isEqualTo(Hl7V3Format.MESSAGE);
    }

    @Test
    @DisplayName("Deve escolher a estratégia de mensagem para uma raiz no padrão XXXX_IN000000UV00")
    void shouldChooseMessageStrategyForInteractionNamedRoot() {
        Element root = V3TestResources.root(V3TestResources.read("/samples/v3/mcci-sem-interaction-id.xml"));

        Hl7V3Strategy strategy = Hl7V3StrategyRegistry.defaultRegistry().find(root).orElseThrow(IllegalStateException::new);

        assertThat(strategy).isInstanceOf(Hl7V3MessageStrategy.class);
    }

    @Test
    @DisplayName("Deve não encontrar estratégia quando o registry está vazio")
    void shouldFindNothingInEmptyRegistry() {
        Element root = V3TestResources.root(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(new Hl7V3StrategyRegistry().find(root)).isEmpty();
    }

    @Test
    @DisplayName("Deve dar prioridade à estratégia customizada registrada pelo builder sobre a CDA")
    void shouldPreferCustomStrategyRegisteredInBuilder() {
        Hl7V3StrategyRegistry registry = Hl7V3StrategyRegistry.defaultRegistry();
        registry.register(new TemplateStrategy(CDA_TEMPLATE));

        Hl7V3Processor processor = Hl7Handler.v3().strategyRegistry(registry).build();
        Hl7V3Document document = processor.read(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml")).document();

        assertThat(document.strategy()).isInstanceOf(TemplateStrategy.class);
        assertThat(document.versionIdentifier()).isEqualTo("CDA-SUMARIO");
    }

    @Test
    @DisplayName("Deve usar a CDA padrão quando o templateId da estratégia customizada não confere")
    void shouldFallBackToCdaWhenCustomTemplateDiffers() {
        Hl7V3StrategyRegistry registry = Hl7V3StrategyRegistry.defaultRegistry();
        registry.register(new TemplateStrategy(OTHER_TEMPLATE));

        Hl7V3Processor processor = Hl7Handler.v3().strategyRegistry(registry).build();
        Hl7V3Document document = processor.read(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml")).document();

        assertThat(document.strategy()).isInstanceOf(CdaR2Strategy.class);
        assertThat(document.versionIdentifier()).isEqualTo("CDA-R2");
    }

    @Test
    @DisplayName("Deve cair na estratégia de mensagem com aviso para raiz desconhecida no namespace HL7 v3")
    void shouldFallBackToMessageWithWarningForUnknownRoot() {
        String xml = "<Foo xmlns=\"urn:hl7-org:v3\"><id root=\"2.16.840.1.113883.19.3\" extension=\"X-1\"/></Foo>";

        Hl7V3Document document = parseText(xml);

        assertThat(document.format()).isEqualTo(Hl7V3Format.MESSAGE);
        assertThat(document.versionIdentifier()).isEqualTo("V3");
        assertThat(document.warnings())
            .contains("No HL7 v3 strategy supports root element 'Foo'; using MESSAGE fallback");
    }

    @Test
    @DisplayName("Deve expor as coordenadas semânticas da estratégia CDA")
    void shouldExposeCdaSemanticCoordinates() {
        assertThat(new CdaR2Strategy().semanticCoordinates())
            .containsKeys("documentId", "patientId", "effectiveTime", "documentType", "title");
    }

    @Test
    @DisplayName("Deve expor as coordenadas semânticas da estratégia de mensagem")
    void shouldExposeMessageSemanticCoordinates() {
        assertThat(new Hl7V3MessageStrategy().semanticCoordinates())
            .containsKeys("messageId", "interactionId", "creationTime", "patientId", "sender");
    }

    @Test
    @DisplayName("Deve resolver as coordenadas semânticas de um CDA")
    void shouldResolveCdaSemanticCoordinates() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.get("#documentId")).isEqualTo("DOC-0001");
        assertThat(document.get("#patientId")).isEqualTo("700000000000001");
        assertThat(document.get("#effectiveTime")).isEqualTo("20261109143000-0300");
        assertThat(document.get("#documentType")).isEqualTo("18842-5");
        assertThat(document.get("#title")).isEqualTo("Sumario de Alta Hospitalar");
    }

    @Test
    @DisplayName("Deve resolver as coordenadas semânticas de uma mensagem")
    void shouldResolveMessageSemanticCoordinates() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/prpa-in201301uv02.xml"));

        assertThat(document.get("#messageId")).isEqualTo("MSG-0001");
        assertThat(document.get("#interactionId")).isEqualTo("PRPA_IN201301UV02");
        assertThat(document.get("#creationTime")).isEqualTo("20261110091500-0300");
        assertThat(document.get("#patientId")).isEqualTo("PAC-777");
        assertThat(document.get("#sender")).isEqualTo("2.16.840.1.113883.19.3.1");
    }

    @Test
    @DisplayName("Deve devolver string vazia para uma coordenada semântica desconhecida")
    void shouldReturnEmptyForUnknownSemanticCoordinate() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.get("#naoExiste")).isEmpty();
    }
}
