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
 *
 * <h3>Optimizations</h3>
 * <ol>
 *   <li><b>Buffered streams</b> — avoids a syscall per byte.</li>
 *   <li><b>Unified {@code byte[]} send queue</b> — both text and binary frames are pre-encoded to
 *       bytes before enqueueing, so the drain loop is a single {@code output.write(byte[])} with
 *       no string manipulation on the hot path.</li>
 *   <li><b>Lock-free send scheduling</b> — CAS on {@link #sending} prevents concurrent drain
 *       tasks without using {@code synchronized}.</li>
 *   <li><b>Burst draining</b> — one sender task drains the full queue before releasing, reducing
 *       executor scheduling overhead during broadcast bursts.</li>
 *   <li><b>Shared IO thread pool</b> — {@link #SEND_POOL} is shared across all connections;
 *       one drain task per connection at a time, not one thread per connection.</li>
 *   <li><b>Line-buffered receive with buffer reuse</b> — {@link #lineBuffer} is reset and
 *       reused each call; no per-byte allocation.</li>
 * </ol>
 */
@Slf4j
public class SocketConnection implements Connection {

  private static final int MAX_TIMEOUT_MILLIS = 5000;
  // 8 slots: enough for burst handling without allowing hundreds of MB of stale frames
  // to queue up for a slow client. Excess frames are dropped (oldest-first) so the client
  // always sees the most recent state when it catches up.
  private static final int MAX_QUEUE_SIZE = 8;
  private static final int MAX_LINE_LENGTH = 8192;
  private static final int MAX_LINE_BUFFER_SIZE = 256;

  /**
   * Shared IO thread pool — one drain task per connection at a time, not one thread per connection.
   */
  private static final ExecutorService SEND_POOL =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  private final Socket socket;
  private final BufferedInputStream input;
  private final BufferedOutputStream output;

  /**
   * Unified outgoing queue. Every element is a complete, wire-ready byte array:
   * <ul>
   *   <li>text messages: {@code (message + "\n").getBytes(UTF-8)}</li>
   *   <li>binary frames: the raw frame from {@link com.ntros.codec.PacketCodec} (no suffix)</li>
   * </ul>
   * Pre-encoding strings at enqueue time keeps the drain loop allocation-free.
   */
  private final BlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

  /**
   * CAS gate — at most one drain task writes to the socket at a time.
   */
  private final AtomicBoolean sending = new AtomicBoolean(false);

  /**
   * Reused buffer for line reads — avoids per-call allocation.
   */
  private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(MAX_LINE_BUFFER_SIZE);

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
   * Enqueues {@code message + '\n'} encoded as UTF-8 bytes.
   * If the queue is full the oldest element is dropped to make room.
   */
  @Override
  public void send(String message) {
    enqueue((message + "\n").getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Enqueues a pre-encoded binary frame verbatim — no bytes are added.
   * If the queue is full the oldest element is dropped to make room.
   */
  @Override
  public void send(byte[] frame) {
    enqueue(frame);
  }

  /**
   * Enqueues bytes, dropping the oldest queued frame if the queue is at capacity.
   * State frames are superseded snapshots: losing an old one is always safe.
   */
  private void enqueue(byte[] bytes) {
    if (!sendQueue.offer(bytes)) {
      sendQueue.poll();     // drop the oldest stale frame
      var unused = sendQueue.offer(bytes);
    }
    trySend();
  }

  /**
   * Schedules a sender task if none is currently running.
   * CAS on {@link #sending} guarantees at most one task drains the queue at a time.
   */
  private void trySend() {
    if (!sending.compareAndSet(false, true)) {
      return;
    }
    SEND_POOL.execute(() -> {
      try {
        while (true) {
          byte[] frame;
          // Drain the full queue in one burst to amortize executor scheduling overhead.
          while ((frame = sendQueue.poll()) != null) {
            output.write(frame);
          }
          output.flush();
          sending.set(false);
          // If new frames arrived during flush(), re-acquire and continue.
          if (sendQueue.isEmpty() || !sending.compareAndSet(false, true)) {
            return;
          }
        }
      } catch (IOException ex) {
        int dropped = sendQueue.size();
        log.warn("Send failed ({} queued frames dropped): {}", dropped, ex.toString());
        // any IOException means the connection is most likely unrecoverable. Just close.
        close();
      }
    });
  }

  /**
   * Reads one newline-terminated line.
   * Returns {@code "_TIMEOUT_"} on a socket read timeout, {@code null} on clean EOF.
   */
  @Override
  public String receive() {
    lineBuffer.reset();
    try {
      int b;
      while ((b = input.read()) != -1) {
        if (b == '\n') {
          break;
        }
        if (b != '\r') {
          lineBuffer.write(b);
        }
        if (lineBuffer.size() > MAX_LINE_LENGTH) {
          throw new IOException("Line too long");
        }
      }
      if (b == -1 && lineBuffer.size() == 0) {
        return null;
      }
    } catch (SocketTimeoutException ex) {
      return "_TIMEOUT_";
    } catch (IOException ex) {
      if ("Connection reset".equals(ex.getMessage())) {
        log.info("[SocketConnection]: Connection reset.");
      } else {
        log.error("[SocketConnection]: Read error ({}): {}", getRemoteAddress(), ex.getMessage(),
            ex);
      }
      close();
    }
    return lineBuffer.toString(StandardCharsets.UTF_8);
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
      log.error("[SocketConnection]: Error closing socket ({}): {}",
          getRemoteAddress(), ex.getMessage(), ex);
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
