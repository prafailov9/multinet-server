package com.ntros.connection;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

public class DuplexClient implements Closeable {
    private final SocketConnection connection;
    private final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
    private final ExecutorService readerThread = Executors.newSingleThreadExecutor();

    public DuplexClient(Socket socket) throws IOException {
        this.connection = new SocketConnection(socket);
        startReaderLoop();
    }

    private void startReaderLoop() {
        readerThread.submit(() -> {
            while (connection.isOpen()) {
                String line = connection.receive();
                if (line != null) {
                    inbox.offer(line);
                }
            }
        });
    }

    public void send(String data) {
        connection.send(data);
    }

    public String nextMessage(int timeoutSec) throws InterruptedException {
        return inbox.poll(timeoutSec, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        readerThread.shutdownNow();
        connection.close();
    }
}
