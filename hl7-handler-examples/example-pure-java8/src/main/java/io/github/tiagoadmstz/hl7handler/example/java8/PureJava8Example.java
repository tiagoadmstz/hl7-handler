package io.github.tiagoadmstz.hl7handler.example.java8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;

/**
 * Runs the three HL7 cases on Java 8 with the hl7-handler-java8 artifact: a lab result read as a
 * list, a 2.6 monitor message with its ACK, and a legacy 2.3 admission with PID-2 as the patient id.
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

        System.out.println("\nExecução finalizada com sucesso!");
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
