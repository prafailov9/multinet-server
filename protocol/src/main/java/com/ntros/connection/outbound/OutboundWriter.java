package com.ntros.connection.outbound;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class OutboundWriter implements Closeable {

  private final OutputStream out;
  private final int capacity;
  private final BlockingQueue<Outbound> queue;
  private final ExecutorService writerExec;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Charset charset = StandardCharsets.UTF_8;

  public OutboundWriter(OutputStream out, int capacity, String nameTag) {
    this.out = Objects.requireNonNull(out, "out");
    this.capacity = capacity;
    this.queue = new LinkedBlockingQueue<>(capacity);
    this.writerExec = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "outbound-" + nameTag);
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * Enqueue a one-line message (terminator added by writer).
   */
  public void sendLine(String text) {
    offerOrFail(new Outbound.Line(text));
    startIfNeeded();
  }

  /**
   * Enqueue a length-framed message: header + '\n' + raw body + '\n'.
   */
  public void sendFrame(String header, byte[] body) {
    offerOrFail(new Outbound.Frame(header, body));
    startIfNeeded();
  }

  private void offerOrFail(Outbound m) {
    if (!queue.offer(m)) {
      throw new RuntimeException("Backpressure: client not reading data (queue " + capacity + ")");
    }
  }

  private void startIfNeeded() {
    if (!running.compareAndSet(false, true)) {
      return;
    }
    writerExec.execute(this::drainLoop);
  }

  private void drainLoop() {
    try {
      label:
      for (; ; ) {
        Outbound m = queue.poll();
        switch (m) {
          case null:
            break label;
          case Outbound.Line(String text):
            out.write((text + "\n").getBytes(charset));
            break;
          case Outbound.Frame(String header, byte[] body):
            out.write((header + "\n").getBytes(charset)); // header line
            out.write(body);                              // raw payload
            out.write('\n');                           // trailing newline

            break;
          default:
            break;
        }

      }
      out.flush();
    } catch (IOException ex) {
      log.error("OutboundWriter flush failed: {}", ex.toString());
      // The socket likely died; nothing more to do here
    } finally {
      running.set(false);
      // If new items arrived after we stopped, loop again.
      if (!queue.isEmpty()) {
        startIfNeeded();
      }
    }
  }

  /**
   * Best-effort drain (optional).
   */
  public void flushNow() {
    startIfNeeded();
  }

  @Override
  public void close() {
    writerExec.shutdownNow();
    queue.clear();
  }
}
