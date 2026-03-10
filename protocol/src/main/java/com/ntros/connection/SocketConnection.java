package com.ntros.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps one client socket and abstracts network I/O.
 * <p>
 * Optimizations in this implementation:
 * <p>
 * 1. Buffered streams for faster IO
 * Avoids syscall per byte and reduces kernel transitions.
 * <p>
 * 2. Asynchronous send queue
 * Game/session threads never block on socket writes.
 * <p>
 * 3. Lock-free send scheduling
 * Uses CAS (AtomicBoolean) instead of synchronized.
 * <p>
 * 4. Burst draining sender
 * A single sender task drains the queue fully to reduce executor scheduling.
 * <p>
 * 5. Shared IO thread pool
 * Prevents one thread per connection which would not scale.
 * <p>
 * 6. Line-buffered receive with memory reuse
 * Avoids allocating new objects per byte.
 */
@Slf4j
public class SocketConnection implements Connection {

  private static final int MAX_TIMEOUT_MILLIS = 5000;
  private static final int MAX_QUEUE_SIZE = 1024;
  private static final int MAX_LINE_LENGTH = 8192;

  /**
   * Shared IO thread pool.
   * Important scalability optimization:
   * Instead of creating a thread per connection (which would kill performance
   * with thousands of clients), all connections share a small IO pool.
   */
  private static final ExecutorService SEND_POOL =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  private final Socket socket;

  /**
   * Buffered streams drastically reduce the number of syscalls.
   * Without buffering, each read/write can trigger a kernel call.
   * With buffering, reads happen in chunks (8KB default).
   */
  private final BufferedInputStream input;
  private final BufferedOutputStream output;

  /**
   * Outgoing messages are queued so that game logic threads
   * never block on network IO.
   */
  private final BlockingQueue<String> sendQueue =
      new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

  /**
   * Ensures only one sender task writes to the socket at a time.
   * CAS is used instead of synchronized to avoid lock contention.
   */
  private final AtomicBoolean sending = new AtomicBoolean(false);

  /**
   * Reusable buffer for reading lines from the socket.
   * Avoids allocating new buffers on every receive() call.
   */
  private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(256);

  public SocketConnection(Socket socket) throws IOException {
    this.socket = socket;
    socket.setSoTimeout(MAX_TIMEOUT_MILLIS);

    this.input = new BufferedInputStream(socket.getInputStream());
    this.output = new BufferedOutputStream(socket.getOutputStream());
  }

  public SocketConnection(Socket socket, boolean shouldTimeout) throws IOException {
    this.socket = socket;
    if (shouldTimeout) {
      socket.setSoTimeout(MAX_TIMEOUT_MILLIS);
    }
    this.input = new BufferedInputStream(socket.getInputStream());
    this.output = new BufferedOutputStream(socket.getOutputStream());
  }

  /**
   * Enqueues a message to be sent to the client.
   * This method is intentionally very lightweight:
   * - No blocking IO
   * - No locks
   * - Just queue + scheduling
   * This allows many threads to call send() concurrently.
   */
  @Override
  public void send(String message) {
    if (!sendQueue.offer(message)) {
      throw new RuntimeException("Backpressure: client not reading data.");
    }
    trySend();
  }

  /**
   * Attempts to schedule a sender task.
   * Only one sender task may run at a time.
   * CAS prevents concurrent writers.
   */
  private void trySend() {
    if (!sending.compareAndSet(false, true)) {
      return;
    }
    SEND_POOL.execute(() -> {
      try {
        while (true) {
          String msg;
          /**
           * Drain the queue completely in a burst.
           * This significantly reduces executor scheduling overhead
           * when many messages are sent quickly.
           */
          while ((msg = sendQueue.poll()) != null) {
            output.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
          }
          output.flush();
          /**
           * Release sender ownership.
           */
          sending.set(false);
          /**
           * If new messages arrived during flush(), attempt to continue.
           */
          if (sendQueue.isEmpty() || !sending.compareAndSet(false, true)) {
            return;
          }
        }
      } catch (IOException ex) {
        log.error("Send failed: {}", ex.getMessage());
        close();
      }
    });
  }

  /**
   * Reads a single line terminated by '\n'.
   * <p>
   * Optimization vs naive implementations:
   * <p>
   * - Uses buffered input stream
   * - Reuses internal buffer
   * - Avoids per-byte allocations
   */
  @Override
  public String receive() {

    lineBuffer.reset();

    try {
      int nextByte;
      while ((nextByte = input.read()) != -1) {
        if (nextByte == '\n') {
          break;
        }
        if (nextByte != '\r') {
          lineBuffer.write(nextByte);
        }
        if (lineBuffer.size() > MAX_LINE_LENGTH) {
          throw new IOException("Line too long");
        }
      }
      if (nextByte == -1 && lineBuffer.size() == 0) {
        return null;
      }
    } catch (SocketTimeoutException ex) {
      return "_TIMEOUT_";
    } catch (IOException ex) {

      if ("Connection reset".equals(ex.getMessage())) {
        log.info("[SocketConnection]: Connection reset.");
      } else {
        log.error(
            "[SocketConnection]: Failed to read line from socket ({}): {}",
            getRemoteAddress(),
            ex.getMessage(),
            ex);
      }
      close();
    }
    return lineBuffer.toString(StandardCharsets.UTF_8);
  }

  @Override
  public void sendFrame(String headerLine, byte[] body) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public byte[] receiveBytesExactly(int length) throws IOException {
    byte[] data = new byte[length];
    int read = 0;
    while (read < length) {
      int r = input.read(data, read, length - read);
      if (r == -1) {
        throw new IOException("Unexpected EOF");
      }
      read += r;
    }
    return data;
  }

  @Override
  public void close() {
    try {
      if (isOpen()) {
        socket.close();
        log.info("[SocketConnection]: Closed socket {}", getRemoteAddress());
      }
    } catch (IOException ex) {
      log.error(
          "[SocketConnection]: Error while closing socket ({}): {}",
          getRemoteAddress(),
          ex.getMessage(),
          ex);
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