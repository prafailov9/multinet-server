package com.ntros.connection;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SocketConnectionTest {

  private ServerSocket serverSocket;
  private Socket clientSocket;
  private SocketConnection connection;

  @BeforeEach
  public void setup() throws Exception {
    serverSocket = new ServerSocket(0);
    int port = serverSocket.getLocalPort();

    ExecutorService acceptor = Executors.newSingleThreadExecutor();
    Future<Socket> accepted = acceptor.submit(serverSocket::accept);

    clientSocket = new Socket("localhost", port);
    Socket serverSide = accepted.get();

    connection = new SocketConnection(serverSide);
  }

  @AfterEach
  void cleanup() throws Exception {
    if (connection != null) {
      connection.close();
    }
    if (clientSocket != null) {
      clientSocket.close();
    }
    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  // --------------------------------------------------
  // RECEIVE TESTS
  // --------------------------------------------------

  @Test
  void receive_singleLine_returnsMessage() throws Exception {

    OutputStream out = clientSocket.getOutputStream();
    out.write("HELLO\n".getBytes(StandardCharsets.UTF_8));
    out.flush();

    String msg = connection.receive();

    assertEquals("HELLO", msg);
  }

  @Test
  void receive_handlesCRLF() throws Exception {

    OutputStream out = clientSocket.getOutputStream();
    out.write("PING\r\n".getBytes(StandardCharsets.UTF_8));
    out.flush();

    String msg = connection.receive();

    assertEquals("PING", msg);
  }

  @Test
  void receive_multipleMessages_readsIndividually() throws Exception {

    OutputStream out = clientSocket.getOutputStream();
    out.write("A\nB\n".getBytes(StandardCharsets.UTF_8));
    out.flush();

    assertEquals("A", connection.receive());
    assertEquals("B", connection.receive());
  }

  @Test
  void receive_returnsNullOnEOF() throws Exception {

    clientSocket.close();

    String msg = connection.receive();

    assertNull(msg);
  }

  @Test
  void receive_returnsTimeoutToken() throws Exception {

    String msg = connection.receive();

    assertEquals("_TIMEOUT_", msg);
  }

  @Test
  void receive_largeLineWithinLimit() throws Exception {

    String payload = "X".repeat(8000) + "\n";

    OutputStream out = clientSocket.getOutputStream();
    out.write(payload.getBytes(StandardCharsets.UTF_8));
    out.flush();

    String msg = connection.receive();

    assertEquals("X".repeat(8000), msg);
  }

  // --------------------------------------------------
  // SEND TESTS
  // --------------------------------------------------

  @Test
  void send_singleMessage_writesToSocket() throws Exception {

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));

    connection.send("HELLO");

    String line = reader.readLine();

    assertEquals("HELLO", line);
  }

  @Test
  void send_multipleMessages_preservesOrder() throws Exception {

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));

    connection.send("A");
    connection.send("B");
    connection.send("C");

    assertEquals("A", reader.readLine());
    assertEquals("B", reader.readLine());
    assertEquals("C", reader.readLine());
  }

  @Test
  void send_concurrentSenders_preservesAllMessages() throws Exception {

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));

    int threads = 10;
    int messagesPerThread = 50;

    ExecutorService exec = Executors.newFixedThreadPool(threads);

    for (int t = 0; t < threads; t++) {
      int threadId = t;
      exec.submit(() -> {
        for (int i = 0; i < messagesPerThread; i++) {
          connection.send("T" + threadId + "-" + i);
        }
      });
    }

    exec.shutdown();
    exec.awaitTermination(5, TimeUnit.SECONDS);

    List<String> received = new ArrayList<>();

    for (int i = 0; i < threads * messagesPerThread; i++) {
      received.add(reader.readLine());
    }

    assertEquals(threads * messagesPerThread, received.size());
  }

  // --------------------------------------------------
  // BYTE RECEIVE TEST
  // --------------------------------------------------

  @Test
  void receiveBytesExactly_readsExactLength() throws Exception {

    OutputStream out = clientSocket.getOutputStream();
    out.write(new byte[]{1, 2, 3, 4});
    out.flush();

    byte[] data = connection.receiveBytesExactly(4);

    assertArrayEquals(new byte[]{1, 2, 3, 4}, data);
  }

  @Test
  void receiveBytesExactly_throwsOnEOF() throws Exception {

    clientSocket.close();

    assertThrows(IOException.class, () -> connection.receiveBytesExactly(5));
  }

  // --------------------------------------------------
  // CLOSE TESTS
  // --------------------------------------------------

  @Test
  void close_closesSocket() throws Exception {

    connection.close();

    assertFalse(connection.isOpen());
  }

  @Test
  void isOpen_trueWhileConnected() throws Exception {

    assertTrue(connection.isOpen());
  }

  @Test
  void send_highVolume_stress() throws Exception {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    int producers = 8;
    int messagesPerProducer = 2000;
    int totalMessages = producers * messagesPerProducer;

    ExecutorService producersPool = Executors.newFixedThreadPool(producers);

    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(producers);

    for (int p = 0; p < producers; p++) {
      int producerId = p;

      producersPool.submit(() -> {
        try {
          startGate.await();

          for (int i = 0; i < messagesPerProducer; i++) {
            connection.send("P" + producerId + "-" + i);
          }

        } catch (Exception ignored) {
        } finally {
          done.countDown();
        }
      });
    }

    // start all producers simultaneously
    startGate.countDown();

    done.await();
    producersPool.shutdown();

    clientSocket.setSoTimeout(2000);

    List<String> received = new ArrayList<>();
    try {
      while (true) {
        received.add(reader.readLine());
      }
    } catch (SocketTimeoutException ignored) {
    }
    assertEquals(totalMessages, received.size());

    // verify ordering per producer
    for (int p = 0; p < producers; p++) {
      int lastIndex = -1;

      for (String msg : received) {
        if (msg.startsWith("P" + p + "-")) {
          int idx = Integer.parseInt(msg.substring(msg.indexOf('-') + 1));
          assertTrue(idx > lastIndex, "Out-of-order message for producer " + p);
          lastIndex = idx;
        }
      }

      assertEquals(messagesPerProducer - 1, lastIndex);
    }
  }

}