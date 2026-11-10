package io.github.tdteixeira.hl7handler.example.stub.server;

import io.github.tdteixeira.hl7handler.Hl7Handler;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedded Mock MLLP TCP Server for testing network HL7 integration.
 * Listens for incoming HL7 messages, decodes them, and replies with automatic HL7 ACK frames.
 */
public class MockMllpServer implements AutoCloseable {

    private final int requestedPort;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService threadPool;
    private int actualPort;

    public MockMllpServer() {
        this(0); // Port 0 picks any available ephemeral port
    }

    public MockMllpServer(int port) {
        this.requestedPort = port;
    }

    /**
     * Starts the mock server in a background thread.
     */
    public synchronized MockMllpServer start() throws IOException {
        if (running.get()) {
            return this;
        }

        serverSocket = new ServerSocket(requestedPort);
        actualPort = serverSocket.getLocalPort();
        running.set(true);
        threadPool = Executors.newCachedThreadPool();

        threadPool.submit(this::listenLoop);
        return this;
    }

    private void listenLoop() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (!running.get()) {
                    break;
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            MllpStreamReader reader = Hl7Handler.network();
            String incomingHl7;

            while ((incomingHl7 = reader.readMessage(in)) != null) {
                // Parse message to get message control ID
                Hl7Message msg = Hl7Handler.reader().from(incomingHl7).toMessage();
                String controlId = msg.get("MSH.10");

                // Generate MLLP ACK response
                String ack = "MSH|^~\\&|MOCK_HL7_SERVER|MOCK_HOSPITAL|SENDER|ORIGIN|20261109120000||ACK|ACK-" + controlId + "|P|2.5\r" +
                             "MSA|AA|" + (controlId.isEmpty() ? "MSG001" : controlId) + "|Mensagem processada com sucesso\r";

                byte[] ackFrame = reader.wrap(ack);
                out.write(ackFrame);
                out.flush();
            }
        } catch (IOException ignored) {}
    }

    public int getPort() {
        return actualPort;
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public synchronized void close() {
        if (running.compareAndSet(true, false)) {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException ignored) {}

            if (threadPool != null) {
                threadPool.shutdownNow();
            }
        }
    }
}
