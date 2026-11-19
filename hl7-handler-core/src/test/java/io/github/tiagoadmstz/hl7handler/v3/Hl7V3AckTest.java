package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.v3.ack.Hl7V3AckCode;
import io.github.tiagoadmstz.hl7handler.v3.ack.Hl7V3AckGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ACK HL7 v3 MCCI_IN000002UV01 gerado a partir de uma mensagem recebida")
class Hl7V3AckTest {

    private static final String PRPA_PATH = "/samples/v3/prpa-in201301uv02.xml";
    private static final String CDA_PATH = "/samples/v3/cda-r2-sumario-alta.xml";
    private static final String RECEIVED_ID_ROOT = "2.16.840.1.113883.19.3";
    private static final String RECEIVED_ID_EXTENSION = "MSG-0001";
    private static final String RECEIVER_DEVICE_ROOT = "2.16.840.1.113883.19.3.2";
    private static final String SENDER_DEVICE_ROOT = "2.16.840.1.113883.19.3.1";

    private static Hl7V3Document receivedMessage() {
        return Hl7Handler.v3().build().read(V3TestResources.read(PRPA_PATH)).document();
    }

    private static Hl7V3Document reread(String ackXml) {
        return Hl7Handler.v3().build().read(ackXml).document();
    }

    @Test
    @DisplayName("Deve gerar um ACK AA com a raiz MCCI_IN000002UV01 e o ITSVersion XML_1.0")
    void shouldGenerateAaAckWithMcciRoot() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        Hl7V3Document document = reread(ack);
        assertThat(document.rootName()).isEqualTo("MCCI_IN000002UV01");
        assertThat(ack).contains("ITSVersion=\"XML_1.0\"");
        assertThat(document.get("hl7:acknowledgement/@typeCode")).isEqualTo("AA");
        assertThat(document.getAll("hl7:acknowledgement/hl7:acknowledgementDetail")).isEmpty();
    }

    @Test
    @DisplayName("Deve gerar o detalhe de erro com typeCode E para o código AE")
    void shouldAddErrorDetailForAeCode() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage(), Hl7V3AckCode.AE, "Paciente sem CNS");

        Hl7V3Document document = reread(ack);
        assertThat(document.get("hl7:acknowledgement/@typeCode")).isEqualTo("AE");
        assertThat(document.get("hl7:acknowledgement/hl7:acknowledgementDetail/@typeCode")).isEqualTo("E");
        assertThat(document.get("hl7:acknowledgement/hl7:acknowledgementDetail/hl7:text"))
            .isEqualTo("Paciente sem CNS");
    }

    @Test
    @DisplayName("Deve gerar o detalhe informativo com typeCode I para o código CA")
    void shouldAddInformationDetailForCaCode() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage(), Hl7V3AckCode.CA, "Recebido");

        Hl7V3Document document = reread(ack);
        assertThat(document.get("hl7:acknowledgement/@typeCode")).isEqualTo("CA");
        assertThat(document.get("hl7:acknowledgement/hl7:acknowledgementDetail/@typeCode")).isEqualTo("I");
    }

    @Test
    @DisplayName("Deve omitir o detalhe quando o texto é vazio ou só espaços")
    void shouldOmitDetailForBlankText() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage(), Hl7V3AckCode.AE, "   ");

        assertThat(reread(ack).getAll("hl7:acknowledgement/hl7:acknowledgementDetail")).isEmpty();
    }

    @Test
    @DisplayName("Deve trocar receiver e sender: o receiver do ACK são os ids do sender recebido")
    void shouldSwapSenderAndReceiver() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        Hl7V3Document document = reread(ack);
        assertThat(document.get("hl7:receiver/hl7:device/hl7:id/@root")).isEqualTo(SENDER_DEVICE_ROOT);
        assertThat(document.get("hl7:sender/hl7:device/hl7:id/@root")).isEqualTo(RECEIVER_DEVICE_ROOT);
    }

    @Test
    @DisplayName("Deve apontar o targetMessage para o id da mensagem recebida")
    void shouldTargetReceivedMessageId() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        Hl7V3Document document = reread(ack);
        assertThat(document.get("hl7:acknowledgement/hl7:targetMessage/hl7:id/@root")).isEqualTo(RECEIVED_ID_ROOT);
        assertThat(document.get("hl7:acknowledgement/hl7:targetMessage/hl7:id/@extension"))
            .isEqualTo(RECEIVED_ID_EXTENSION);
    }

    @Test
    @DisplayName("Deve gravar o interactionId MCCI_IN000002UV01 com a raiz 2.16.840.1.113883.1.6")
    void shouldWriteAckInteractionId() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        Hl7V3Document document = reread(ack);
        assertThat(document.get("hl7:interactionId/@root")).isEqualTo("2.16.840.1.113883.1.6");
        assertThat(document.get("hl7:interactionId/@extension")).isEqualTo("MCCI_IN000002UV01");
    }

    @Test
    @DisplayName("Deve copiar o processingCode da mensagem recebida")
    void shouldCopyProcessingCodeFromReceivedMessage() {
        String xml = V3TestResources.read(PRPA_PATH).replace("<processingCode code=\"P\"/>",
            "<processingCode code=\"D\"/>");
        Hl7V3Document received = Hl7Handler.v3().build().read(xml).document();

        String ack = Hl7V3AckGenerator.generateAck(received);

        assertThat(reread(ack).get("hl7:processingCode/@code")).isEqualTo("D");
    }

    @Test
    @DisplayName("Deve usar P como processingCode quando a mensagem recebida não o informa")
    void shouldDefaultProcessingCodeToPWhenMissing() {
        String xml = V3TestResources.read(PRPA_PATH).replace("<processingCode code=\"P\"/>", "");
        Hl7V3Document received = Hl7Handler.v3().build().read(xml).document();

        String ack = Hl7V3AckGenerator.generateAck(received);

        assertThat(reread(ack).get("hl7:processingCode/@code")).isEqualTo("P");
    }

    @Test
    @DisplayName("Deve gerar o creationTime no formato TS com fuso")
    void shouldWriteCreationTimeAsTimestamp() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        assertThat(reread(ack).get("hl7:creationTime/@value")).matches("\\d{14}[+-]\\d{4}");
    }

    @Test
    @DisplayName("Deve gerar um id do ACK com UUID aleatório a cada chamada")
    void shouldGenerateRandomUuidForEachAck() {
        Hl7V3Document received = receivedMessage();

        String first = reread(Hl7V3AckGenerator.generateAck(received)).get("hl7:id/@root");
        String second = reread(Hl7V3AckGenerator.generateAck(received)).get("hl7:id/@root");

        assertThat(first).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("Deve reler o ACK gerado pelo próprio Hl7Handler.v3() como MESSAGE, sem avisos")
    void shouldReadGeneratedAckAsMessage() {
        String ack = Hl7V3AckGenerator.generateAck(receivedMessage());

        Hl7V3Document document = reread(ack);
        assertThat(document.format()).isEqualTo(Hl7V3Format.MESSAGE);
        assertThat(document.rootName()).isEqualTo("MCCI_IN000002UV01");
        assertThat(document.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve gerar um ACK AA pelo atalho Hl7Handler.generateV3Ack")
    void shouldGenerateAaThroughHandlerShortcut() {
        String ack = Hl7Handler.generateV3Ack(receivedMessage());

        assertThat(reread(ack).get("hl7:acknowledgement/@typeCode")).isEqualTo("AA");
    }

    @Test
    @DisplayName("Deve recusar um ACK de documento CDA com IllegalArgumentException")
    void shouldRejectAckForCdaDocument() {
        Hl7V3Document cda = Hl7Handler.v3().build().read(V3TestResources.read(CDA_PATH)).document();

        assertThatThrownBy(() -> Hl7V3AckGenerator.generateAck(cda))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ACK applies only to HL7 v3 messages");
    }

    @Test
    @DisplayName("Deve recusar um código de ACK nulo com IllegalArgumentException")
    void shouldRejectNullAckCode() {
        assertThatThrownBy(() -> Hl7V3AckGenerator.generateAck(receivedMessage(), null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
