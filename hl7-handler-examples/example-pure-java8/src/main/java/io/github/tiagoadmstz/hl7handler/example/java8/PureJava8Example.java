package io.github.tiagoadmstz.hl7handler.example.java8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Processor;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Result;
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3Writer;

/**
 * Runs six cases on Java 8 with the hl7-handler-java8 artifact: a lab result read as a list, a 2.6
 * monitor message with its ACK, a legacy 2.3 admission with PID-2 as the patient id, a CDA R2 read
 * with a YAML mapping, a PRPA written from a Map, and a v3 ACK for that PRPA.
 */
public final class PureJava8Example {

    private PureJava8Example() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Exemplo HL7 Handler: Java 8 (hl7-handler-java8) ");
        System.out.println("=================================================");

        System.out.println("\n1. Laboratório: ORU_R01 2.5 com lista OBX[*]");
        executarCasoMapeado(recurso("/samples/oru-hemograma-v25.hl7"), "/mappings/laboratorio-resultados-mapping.yaml");

        System.out.println("\n2. Monitor multiparamétrico: ORU_R01 2.6 e ACK");
        String monitorRaw = recurso("/samples/oru-monitor-v26.hl7");
        Hl7Message monitor = Hl7Handler.reader().from(monitorRaw).toMessage();
        System.out.println("   Versão: " + monitor.getVersion() + " | MSH-18: " + monitor.get("MSH.18"));
        executarCasoMapeado(monitorRaw, "/mappings/monitor-sinais-vitais-mapping.yaml");
        String ackMonitor = Hl7Handler.generateAck(monitor);
        Hl7Message ackMonitorMsg = Hl7Handler.reader().from(ackMonitor).toMessage();
        System.out.println("   ACK: MSA-1=" + ackMonitorMsg.get("MSA.1") + " | MSH-9=" + ackMonitorMsg.get("MSH.9")
                + " | MSA-2=" + ackMonitorMsg.get("MSA.2"));

        System.out.println("\n3. Legado: ADT_A01 2.3 (ID em PID-2) e 2.5 com o mesmo mapeamento");
        executarCasoMapeado(recurso("/samples/adt-legado-v23.hl7"), "/mappings/admissao-legado-mapping.yaml");
        executarCasoMapeado(recurso("/samples/adt-admissao-v25.hl7"), "/mappings/admissao-legado-mapping.yaml");

        System.out.println("\n4. Leitura de CDA R2 com mapeamento (v3)");
        Hl7V3Processor processadorCda = processadorCdaComMapeamento();
        Hl7V3Result cda = processadorCda.read(recurso("/samples/v3/cda-r2-sumario-alta.xml"));
        System.out.println("   Formato: " + cda.document().format());
        System.out.println("   Versão: " + cda.document().versionIdentifier());
        System.out.println("   JSON: " + cda.toJson());

        System.out.println("\n5. Escrita de PRPA_IN201301UV02 a partir de um Map");
        Map<String, Object> paciente = new LinkedHashMap<String, Object>();
        paciente.put("id", "PAC-EXEMPLO-5");
        paciente.put("nome", "Ana");
        paciente.put("sobrenome", "Teste");
        Map<String, Object> mensagemPrpa = new LinkedHashMap<String, Object>();
        mensagemPrpa.put("mensagemId", "MSG-EXEMPLO-5");
        mensagemPrpa.put("paciente", paciente);
        Hl7V3Writer escritor = escritorPrpaComMapeamento();
        String prpaXml = escritor.write(mensagemPrpa);
        System.out.println("   XML:\n" + prpaXml);

        // A amostra traz o envelope completo (sender, receiver, interactionId) que o ACK copia;
        // o XML da seção 5 só tem o corpo do paciente.
        System.out.println("\n6. ACK v3 da PRPA_IN201301UV02");
        Hl7V3Document prpaRecebida = Hl7Handler.v3().build().read(recurso("/samples/v3/prpa-in201301uv02.xml")).document();
        String ackV3 = Hl7Handler.generateV3Ack(prpaRecebida);
        Hl7V3Document ackDocumento = Hl7Handler.v3().build().read(ackV3).document();
        System.out.println("   Raiz: " + ackDocumento.rootName()
                + " | typeCode: " + ackDocumento.get("hl7:acknowledgement/@typeCode"));

        System.out.println("\nExecução finalizada com sucesso!");
    }

    private static Hl7V3Processor processadorCdaComMapeamento() throws IOException {
        InputStream mapping = recursoStream("/mappings/v3/cda-sumario-mapping.yaml");
        try {
            return Hl7Handler.v3().withMapping(mapping).build();
        } finally {
            mapping.close();
        }
    }

    private static Hl7V3Writer escritorPrpaComMapeamento() throws IOException {
        InputStream mapping = recursoStream("/mappings/v3/prpa-escrita-mapping.yaml");
        try {
            return Hl7Handler.v3().withMapping(mapping).buildWriter();
        } finally {
            mapping.close();
        }
    }

    private static void executarCasoMapeado(String rawHl7, String caminhoMapeamento) throws IOException {
        Hl7Message mensagem = Hl7Handler.reader().from(rawHl7).toMessage();
        System.out.println("   Versão: " + mensagem.getVersion());
        InputStream mapping = recursoStream(caminhoMapeamento);
        try {
            String json = Hl7Handler.reader().from(rawHl7).withMapping(mapping).toJson();
            System.out.println("   JSON: " + json);
        } finally {
            mapping.close();
        }
    }

    private static InputStream recursoStream(String caminho) throws IOException {
        InputStream in = PureJava8Example.class.getResourceAsStream(caminho);
        if (in == null) {
            throw new IOException("Recurso não encontrado: " + caminho);
        }
        return in;
    }

    private static String recurso(String caminho) throws IOException {
        InputStream in = recursoStream(caminho);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int lidos = in.read(buffer);
            while (lidos != -1) {
                out.write(buffer, 0, lidos);
                lidos = in.read(buffer);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
