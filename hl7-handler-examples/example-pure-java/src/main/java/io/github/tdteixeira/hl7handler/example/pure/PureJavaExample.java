package io.github.tdteixeira.hl7handler.example.pure;

import io.github.tdteixeira.hl7handler.Hl7Handler;
import io.github.tdteixeira.hl7handler.example.stub.Hl7MockData;
import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;
import io.github.tdteixeira.hl7handler.example.stub.model.ResultadoExameVO;
import io.github.tdteixeira.hl7handler.example.stub.server.MockMllpServer;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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

        System.out.println("\nExecução finalizada com sucesso!");
    }
}
