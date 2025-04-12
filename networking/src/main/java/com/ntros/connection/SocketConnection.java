package com.ntros.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps one client socket, abstracts I/O
 */
public class SocketConnection implements Connection {

    private static final Logger LOGGER = Logger.getLogger(SocketConnection.class.getName());

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;


    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;

        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    @Override
    public void send(String data) {
        try {
            String message = data + "\n"; // always use newline to mark end-of-line
            synchronized (output) {
                output.write(message.getBytes(StandardCharsets.UTF_8));
                output.flush(); // does this add new line?
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing line: {}", e.getMessage());
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
                    // Newline encountered; line is complete.
                    break;
                }
                // Optionally ignore carriage return if present.
                if (nextByte != '\r') {
                    lineBuffer.write(nextByte);
                }
                // Optional safeguard: limit line length to avoid potential issues with malicious clients.
                if (lineBuffer.size() > 8192) { // 8KB limit
                    throw new IOException("Line too long");
                }
            }
            // If we've reached end-of-stream without reading any data, return null.
            if (nextByte == -1 && lineBuffer.size() == 0) {
                return null;
            }
            // Convert the accumulated bytes to a String.
            return lineBuffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            String err = "Error reading line: " + e.getMessage();
            LOGGER.log(Level.SEVERE, err);
            close();
            throw new ConnectionReceiveException(err, e.getCause());
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "[SocketConnection]: Error closing connection: {}", e.getMessage());
        }
    }

    @Override
    public boolean isOpen() {
        return socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown();
    }
}
