package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;

import com.ntros.event.listener.SessionManager;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.ins.WorldInstance;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerTestHelper {

  public static void startServer(Server server, ExecutorService serverExecutor, int port) {
    // running the server on its own thread.
    serverExecutor.submit(() -> {
      try {
        server.start();
      } catch (IOException e) {
        System.out.println("Could not start the server: " + e.getMessage());
      }
    });

    // waiting for the port on the caller thread
    waitForPort(port, 10_000);
  }

  public static void stopServerWhen(Instance instance, Server server,
      ExecutorService serverExecutor) throws IOException {
    server.stop();
    await().pollDelay(Duration.ofMillis(100)) // Give some room for shutdown event handling
        .pollInterval(Duration.ofMillis(50)).atMost(Duration.ofSeconds(5))
        .conditionEvaluationListener(condition -> {
          int sessions = instance.getActiveSessionsCount();

          int entities = instance.getEntityCount();
          log.info("[Awaitility Poll Single] Sessions: {}, Entities: {}", sessions, entities);
        }).until(() -> {
          int sessions = instance.getActiveSessionsCount();
          int entities = instance.getEntityCount();
          return sessions == 0 && entities == 0;
        });

    serverExecutor.shutdownNow();
  }

  public static void stopServerWhen(List<Instance> instances, Server server,
      ExecutorService serverExecutor) throws IOException {
    server.stop();
    await().pollDelay(Duration.ofMillis(100)) // Give some room for shutdown event handling
        .pollInterval(Duration.ofMillis(50)).atMost(Duration.ofSeconds(5))
        .conditionEvaluationListener(condition -> {
          int sessions = instances.stream().mapToInt(Instance::getActiveSessionsCount).sum();
          int entities = instances.stream().mapToInt(Instance::getEntityCount).sum();
          log.info("[Awaitility Poll multiple] Sessions: {}, Entities: {}", sessions, entities);
        }).until(() -> {
          int sessions = instances.stream().mapToInt(Instance::getActiveSessionsCount).sum();
          int entities = instances.stream().mapToInt(Instance::getEntityCount).sum();
          return sessions == 0 && entities == 0;
        });

    serverExecutor.shutdownNow();
  }

  public static void waitForPort(int port, long timeoutMillis) {
    await().atMost(timeoutMillis, MILLISECONDS).pollInterval(50, MILLISECONDS)
        .ignoreExceptions() // ignore ConnectException until port is open
        .until(() -> {
          try (Socket ignored = new Socket("localhost", port)) {
            return true;
          }
        });
  }
}
