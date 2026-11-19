package io.github.tiagoadmstz.hl7handler.example.pure;

import module java.base;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.example.stub.Hl7MockData;
import io.github.tiagoadmstz.hl7handler.example.stub.model.PacienteVO;
import io.github.tiagoadmstz.hl7handler.example.stub.model.ResultadoExameVO;
import io.github.tiagoadmstz.hl7handler.example.stub.server.MockMllpServer;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Version;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Processor;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Result;
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3Writer;

public class PureJavaExample {

    public static void main(final String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  Exemplo HL7 Handler: Java Puro + Módulo Stub   ");
        System.out.println("=================================================");

        // 1. Consumindo dados do Stub
        String rawHl7 = Hl7MockData.ADT_A01_V25;
        Hl7Version version = Hl7Handler.detectVersion(rawHl7);
        System.out.println("1. Versão detectada automaticamente: " + version);

        // 2. Data binding direto no PacienteVO compartilhado usando YAML do stub
        try (InputStream mapping = PureJavaExample.class.getResourceAsStream("/mappings/paciente-stub-mapping.yaml")) {
            PacienteVO paciente = Hl7Handler.reader()
                    .from(rawHl7)
                    .withMapping(mapping)
                    .bindTo(PacienteVO.class);

            System.out.println("\n2. Data Binding em PacienteVO:");
            System.out.println("   Prontuário: " + paciente.prontuario());
            System.out.println("   Nome:       " + paciente.nome() + " " + paciente.sobrenome());
            System.out.println("   Endereço:   " + paciente.endereco());
        }

        // 3. Resultado de Exame Laboratorial usando ORU_R01
        try (InputStream mapping = PureJavaExample.class.getResourceAsStream("/mappings/laboratorio-stub-mapping.yaml")) {
            ResultadoExameVO exame = Hl7Handler.reader()
                    .from(Hl7MockData.ORU_R01_LAB)
                    .withMapping(mapping)
                    .bindTo(ResultadoExameVO.class);

            System.out.println("\n3. Data Binding em ResultadoExameVO:");
            System.out.println("   " + exame);
        }

        // 4. Teste com Servidor TCP/MLLP Mock em memória
        System.out.println("\n4. Iniciando MockMllpServer TCP...");
        try (MockMllpServer server = new MockMllpServer().start()) {
            System.out.println("   Mock Server escutando na porta: " + server.getPort());

            // Conecta cliente Socket ao Mock Server
            try (Socket clientSocket = new Socket("localhost", server.getPort());
                 OutputStream out = clientSocket.getOutputStream();
                 InputStream in = clientSocket.getInputStream()) {

                // Envia frame MLLP
                byte[] frame = Hl7MockData.asMllpFrame(rawHl7);
                out.write(frame);
                out.flush();

                // Recebe resposta ACK MLLP
                String ackMessage = Hl7Handler.network().readMessage(in);
                System.out.println("   ACK recebido do Mock Server:");
                Hl7Message ackMsg = Hl7Handler.reader().from(ackMessage).toMessage();
                System.out.println("   Status ACK: " + ackMsg.get("MSA.1") + " - " + ackMsg.get("MSA.3"));
            }
        }

        // 5. Laboratório: um item por OBX com lista: "OBX[*]"
        System.out.println("\n5. Laboratório: ORU_R01 2.5 com lista OBX[*]");
        executarCasoMapeado(Hl7MockData.ORU_R01_HEMOGRAMA_V25, "/mappings/laboratorio-resultados-mapping.yaml");

        // 6. Monitor 2.6: UTF-8 em MSH-18, DTM e ACK gerado a partir da mensagem recebida
        System.out.println("\n6. Monitor multiparamétrico: ORU_R01 2.6 e ACK");
        Hl7Message monitor = Hl7Handler.reader().from(Hl7MockData.ORU_R01_MONITOR_V26).toMessage();
        System.out.println("   Versão: " + monitor.getVersion() + " | MSH-18: " + monitor.get("MSH.18"));
        executarCasoMapeado(Hl7MockData.ORU_R01_MONITOR_V26, "/mappings/monitor-sinais-vitais-mapping.yaml");
        String ackMonitor = Hl7Handler.generateAck(monitor);
        Hl7Message ackMonitorMsg = Hl7Handler.reader().from(ackMonitor).toMessage();
        System.out.println("   ACK: MSA-1=" + ackMonitorMsg.get("MSA.1") + " | MSH-9=" + ackMonitorMsg.get("MSH.9")
                + " | MSA-2=" + ackMonitorMsg.get("MSA.2"));

        // 7. Legado 2.3: ID em PID-2, com porVersao aplicado no 2.3 e no 2.5 com o mesmo arquivo
        System.out.println("\n7. Legado: ADT_A01 2.3 (ID em PID-2) e 2.5 com o mesmo mapeamento");
        executarCasoMapeado(Hl7MockData.ADT_A01_V23_PID2, "/mappings/admissao-legado-mapping.yaml");
        executarCasoMapeado(Hl7MockData.ADT_A01_V25, "/mappings/admissao-legado-mapping.yaml");

        // 8. CDA R2 lido com mapeamento YAML: formato, versão e JSON semântico
        System.out.println("\n8. Leitura de CDA R2 com mapeamento (v3)");
        try (InputStream mapping = recurso("/mappings/v3/cda-sumario-mapping.yaml");
             InputStream documento = recurso("/samples/v3/cda-r2-sumario-alta.xml")) {
            Hl7V3Processor processadorCda = Hl7Handler.v3().withMapping(mapping).build();
            Hl7V3Result cda = processadorCda.read(documento);
            System.out.println("   Formato: " + cda.document().format());
            System.out.println("   Versão: " + cda.document().versionIdentifier());
            System.out.println("   JSON: " + cda.toJson());
        }

        // 9. PRPA_IN201301UV02 gerada a partir de um Map pelo mesmo tipo de mapeamento usado na leitura
        System.out.println("\n9. Escrita de PRPA_IN201301UV02 a partir de um Map");
        Map<String, Object> paciente = new LinkedHashMap<>();
        paciente.put("id", "PAC-EXEMPLO-9");
        paciente.put("nome", "Ana");
        paciente.put("sobrenome", "Teste");
        Map<String, Object> mensagemPrpa = new LinkedHashMap<>();
        mensagemPrpa.put("mensagemId", "MSG-EXEMPLO-9");
        mensagemPrpa.put("paciente", paciente);
        String prpaXml;
        try (InputStream mapping = recurso("/mappings/v3/prpa-escrita-mapping.yaml")) {
            Hl7V3Writer escritor = Hl7Handler.v3().withMapping(mapping).buildWriter();
            prpaXml = escritor.write(mensagemPrpa);
        }
        System.out.println("   XML:\n" + prpaXml);

        // 10. ACK v3 de uma PRPA recebida. A amostra traz o envelope completo (sender, receiver,
        // interactionId) que o ACK copia; o XML da seção 9 só tem o corpo do paciente.
        System.out.println("\n10. ACK v3 da PRPA_IN201301UV02");
        Hl7V3Document prpaRecebida;
        try (InputStream xml = recurso("/samples/v3/prpa-in201301uv02.xml")) {
            prpaRecebida = Hl7Handler.v3().build().read(xml).document();
        }
        String ackV3 = Hl7Handler.generateV3Ack(prpaRecebida);
        Hl7V3Document ackDocumento = Hl7Handler.v3().build().read(ackV3).document();
        System.out.println("   Raiz: " + ackDocumento.rootName()
                + " | typeCode: " + ackDocumento.get("hl7:acknowledgement/@typeCode"));

        System.out.println("\nExecução finalizada com sucesso!");
    }

    private static void executarCasoMapeado(String rawHl7, String mapeamento) throws IOException {
        Hl7Message mensagem = Hl7Handler.reader().from(rawHl7).toMessage();
        System.out.println("   Versão: " + mensagem.getVersion());
        try (InputStream mapping = PureJavaExample.class.getResourceAsStream(mapeamento)) {
            String json = Hl7Handler.reader().from(rawHl7).withMapping(mapping).toJson();
            System.out.println("   JSON: " + json);
        }
    }

    private static InputStream recurso(String caminho) throws IOException {
        InputStream in = PureJavaExample.class.getResourceAsStream(caminho);
        if (in == null) {
            throw new IOException("Recurso não encontrado: " + caminho);
        }
        return in;
    }
}
