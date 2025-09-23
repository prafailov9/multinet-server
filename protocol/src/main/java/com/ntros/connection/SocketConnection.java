package com.ntros.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * Wraps one client socket, abstracts I/O
 */
@Slf4j
public class SocketConnection implements Connection {

    private static final int MAX_TIMEOUT_MILLIS = 5000;
    private static final int MAX_QUEUE_SIZE = 1024;
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final Queue<String> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);


    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        socket.setSoTimeout(MAX_TIMEOUT_MILLIS);

        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    /**
     * Sends data stream to the client. Blocks until socket's out-buffer is flushed.
     */
    @Override
    public void send(String message) {

        if (sendQueue.size() > MAX_QUEUE_SIZE) {
            throw new RuntimeException("Backpressure: client not reading data.");
        }
        sendQueue.add(message);
        // schedule flush
        trySend();
    }

    /**
     * Reads bytes until a new line(\n) is encountered.
     */
    @Override
    public String receive() {
        // Create a new buffer for every line. This ensures that each call is independent,
        // and you don't keep stale data from a previous read.
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        try {
            int nextByte;
            while ((nextByte = input.read()) != -1) {
                if (nextByte == '\n') {
                    break; // end of line
                }
                // ignore carriage return if present.
                if (nextByte != '\r') {
                    lineBuffer.write(nextByte);
                }
                // limited line length to avoid potential issues with malicious clients.
                if (lineBuffer.size() > 8192) { // 8KB limit
                    throw new IOException("Line too long");
                }
            }
            // return null if no data read
            if (nextByte == -1 && lineBuffer.size() == 0) {
                return null;
            }

        } catch (SocketTimeoutException ex) {
            // no data within MAX_TIMEOUT_MILLIS time. Signal to Session.
            return "_TIMEOUT_";
        } catch (IOException ex) {
            // CONN_RESET can happen when the server shuts down, while client connections are still active. Just log and move on.
            if ("Connection reset".equals(ex.getMessage())) {
                log.info("[SocketConnection]: Connection reset, likely due to shutdown.");
            } else {
                String err = String.format("[SocketConnection]: Failed to read line from socket (%s): %s",
                        getRemoteAddress(), ex.getMessage());

                log.error(err, ex);
            }
            close();
        }
        return lineBuffer.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        try {
            if (isOpen()) {
                socket.close();
                log.info("[SocketConnection]: Closed socket connection to {}", getRemoteAddress());

            }
        } catch (IOException ex) {
            log.error("[SocketConnection]: Error while closing socket ({}): {}", getRemoteAddress(),
                    ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isOpen() {
        return socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown();
    }

    private String getRemoteAddress() {
        try {
            return socket.getRemoteSocketAddress().toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }

    /**
     * Attempts to flush the {@code sendQueue} to the client socket asynchronously.
     * This method ensures that only one background send task runs at a time:
     * Uses the sending flag as a guard to prevent
     * overlapping concurrent writes to the socket's output stream.
     * If a send task is already in progress, the method returns immediately.
     * If no send task is active, a new async task is scheduled with
     * The background task:
     * Polls and writes all queued messages from {@code sendQueue} to the socket's output buffer.
     * Flushes the output stream to ensure delivery to the client.
     * On failure (e.g., client disconnect, socket error), logs the error and closes the connection.
     * Resets the {@code sending} flag to allow future sends, and retries immediately
     * if messages arrived in the meantime.
     */
    private void trySend() {
        // checks if the send tasks is already running.
        // If yes -> return to avoid multiple overlapping writes to the client buffer.
        if (!sending.compareAndSet(false, true)) {
            return;
        }

        // background task that writes to client buffer.
        CompletableFuture.runAsync(() -> {
            try {

                // write all queued messages to client buffer until queue is empty.
                while (!sendQueue.isEmpty()) {
                    String msg = sendQueue.poll();
                    if (msg != null) {
                        output.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
                // confirm and send the buffer changes to the client socket.
                output.flush();
            } catch (IOException ex) {
                // client disconnected or socket no longer valid.
                log.error("Send failed: {}", ex.getMessage());
                close();
            } finally {
                sending.set(false);
                // attempt retry if there are still messages in the queue.
                if (!sendQueue.isEmpty()) {
                    trySend();
                }
            }
        });
    }

    // Deprecated version of the send() method
    private void sendV1(String data) {
        try {
            String message = data + "\n"; // always use newline to mark end-of-line
            synchronized (output) {
                output.write(message.getBytes(StandardCharsets.UTF_8)); // store message in internal buffer.
                output.flush(); // immediately send the buffer to the socket connection.
            }
        } catch (IOException ex) {
            log.error("[SocketConnection]: Failed to write to socket ({}): {}", getRemoteAddress(),
                    ex.getMessage());
            close();
        }
    }

}
