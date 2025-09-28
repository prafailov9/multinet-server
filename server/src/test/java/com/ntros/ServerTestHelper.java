package com.ntros;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.server.Server;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

  public static void stopServerWhen(List<Instance> instances, Server server, ExecutorService exec)
      throws IOException {
    server.stop();

    // run marker tasks to clean up the actor thread
    List<CompletableFuture<Void>> drains = instances.stream()
        .map(Instance::drain)
        .toList();

    // wait until all actor tasks are done for all instances
    await()
        .ignoreExceptions()
        .until(() -> drains.stream().allMatch(CompletableFuture::isDone));

    // assert world empty
    await()
        .pollDelay(Duration.ofMillis(100))
        .pollInterval(Duration.ofMillis(50))
        .atMost(Duration.ofSeconds(5))
        .ignoreExceptions()
        .untilAsserted(() -> {
          int sessions = instances.stream().mapToInt(Instance::getActiveSessionsCount).sum();
          int entities = instances.stream().mapToInt(Instance::getEntityCount).sum();
          assertEquals(0, sessions, "sessions not drained");
          assertEquals(0, entities, "entities not cleared");
        });

    exec.shutdownNow();
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
