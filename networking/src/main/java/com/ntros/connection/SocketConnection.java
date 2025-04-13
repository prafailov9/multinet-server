package com.ntros.connection;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Wraps one client socket, abstracts I/O
 */
@Slf4j
public class SocketConnection implements Connection {

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;


    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;

        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    /**
     * Sends data stream to the client.
     */
    @Override
    public void send(String data) {
        try {
            String message = data + "\n"; // always use newline to mark end-of-line
            synchronized (output) {
                output.write(message.getBytes(StandardCharsets.UTF_8)); // store message in internal buffer.
                output.flush(); // immediately send the stored message to the socket connection.
            }
        } catch (IOException ex) {
            log.error("[SocketConnection]: Failed to write to socket ({}): {}", getRemoteAddress(), ex.getMessage());
            close();
        }
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

        } catch (IOException ex) {
            String err = String.format("[SocketConnection]: Failed to read line from socket (%s): %s",
                    getRemoteAddress(), ex.getMessage());
            log.error(err, ex);
            close();
//            throw new ConnectionReceiveException(err, ex);
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
            log.error("[SocketConnection]: Error while closing socket ({}): {}", getRemoteAddress(), ex.getMessage(), ex);
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
}
