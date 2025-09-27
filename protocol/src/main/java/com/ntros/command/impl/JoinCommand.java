package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ERROR;
import static com.ntros.model.world.protocol.CommandType.WELCOME;

import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.WorldRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.ins.WorldInstance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to enable user to join a multiplayer world.
 */
@Slf4j
public class JoinCommand extends AbstractCommand {

  private static final int WORLD_NAME_INDEX = 1;

  /**
   * Expected Message: JOIN client_name world_name
   *
   * @param message
   * @param session
   * @return
   */
//  @Override
//  public Optional<ServerResponse> execute(Message message, Session session) {
//    SessionContext sessionContext = session.getSessionContext();
//    String playerName = resolvePlayer(message);
//    WorldConnector connector = resolveWorld(message);
//
//    InstanceConfig config = InstanceRegistry.getInstanceConfigForWorld(connector.getWorldName());
//    if (config == null) {
//      return Optional.of(new ServerResponse(
//          new Message(ERROR, List.of("WORLD_NOT_FOUND")),
//          CommandResult.failed(playerName, connector.getWorldName(), "world not found")
//      ));
//    }
//
//    Instance instance = InstanceRegistry.getInstance(connector.getWorldName());
//    if (instance == null) {
//      return Optional.of(new ServerResponse(
//          new Message(ERROR, List.of("WORLD_NOT_FOUND")),
//          CommandResult.failed(playerName, null, "world not found")
//      ));
//    }
//
//    // Visibility / capacity checks
//    InstanceConfig cfg = instance.getConfig();
//    if (cfg.visibility() == Visibility.PRIVATE) {
//      var owner = WorldRegistry.ownerOf(instance.getWorldName()).orElse(null);
//      if (owner != null && !owner.equals(sessionContext.getUserId())) {
//        return Optional.of(new ServerResponse(
//            new Message(ERROR, List.of("WORLD_PRIVATE")),
//            CommandResult.failed(playerName, instance.getWorldName(), "private")
//        ));
//      }
//    }
//    if (cfg.maxPlayers() == 1 && instance.getActiveSessionsCount() >= 1) {
//      return Optional.of(new ServerResponse(
//          new Message(ERROR, List.of("WORLD_BUSY")),
//          CommandResult.failed(playerName, instance.getWorldName(), "busy")
//      ));
//    }
//
//    // Execute command
//    // Enqueue join and wait briefly so it's atomic
//    CompletableFuture<CommandResult> fut = connector.joinPlayerAsynch(new JoinRequest(playerName));
//    CommandResult commandResult = null;
//
//    try {
//      commandResult = fut.get(750, TimeUnit.SECONDS);
//
//    } catch (Exception ex) {
//      log.error("Error occurred: {}", ex.getMessage(), ex);
//    }
//
//    if (commandResult == null) {
//      return Optional.of(new ServerResponse(
//          new Message(ERROR, List.of("Connector did not produce result")),
//          CommandResult.failed(playerName, instance.getWorldName(),
//              "Connector did not produce result")
//      ));
//    }
//
//    if (commandResult.success()) {
//      sessionContext.setEntityId(commandResult.playerName());
//      sessionContext.setWorldId(commandResult.worldName());
//      sessionContext.setJoinedAt(OffsetDateTime.now());
//      sessionContext.setAuthenticated(true);
//
////      instance.registerSession(session);
//
//      log.info("[JOIN Command]: success. Sending WELCOME response to client: {}", sessionContext);
//      return Optional.of(
//          new ServerResponse(new Message(WELCOME, List.of(commandResult.playerName())),
//              commandResult));
//    }
//    sessionContext.setAuthenticated(false);
//    String err = String.format("%s %s\n", ERROR.name(), commandResult.reason());
//    log.error("[JOIN Command]: failure. Sending ERROR response: {}", err);
//
//    return Optional.of(new ServerResponse(new Message(ERROR, List.of(
//        commandResult.reason())), commandResult));
//  }
  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    String player = resolvePlayer(message);

    // Resolve instance ONLY (no connector here)
    Instance instance = resolveInstance(message); // by worldName
    if (instance == null) {
      return Optional.of(error("WORLD_NOT_FOUND", player, null));
    }

    // Policy checks (visibility / capacity)
    InstanceConfig cfg = instance.getConfig();
    if (cfg.visibility() == Visibility.PRIVATE) {
      var owner = WorldRegistry.ownerOf(instance.getWorldName()).orElse(null);
      if (owner != null && !owner.equals(ctx.getUserId())) {
        return Optional.of(error("WORLD_PRIVATE", player, instance.getWorldName()));
      }
    }
    if (cfg.maxPlayers() == 1 && instance.isRunning() && instance instanceof WorldInstance wi
        && wi.getActiveSessionsCount() >= 1) {
      return Optional.of(error("WORLD_BUSY", player, instance.getWorldName()));
    }

    // Enqueue join and ensure ticking so the mailbox gets drained
    CompletableFuture<CommandResult> fut = instance.joinAsync(new JoinRequest(player));
    instance.startIfNeededForJoin(); // safe if already running

    // Wait a short time so JOIN is atomic from the client’s POV
    CommandResult result;
    try {
      result = fut.get(750, TimeUnit.MILLISECONDS);
    } catch (TimeoutException te) {
      return Optional.of(error("JOIN_TIMEOUT", player, instance.getWorldName()));
    } catch (Exception e) {
      return Optional.of(error("JOIN_FAILED", player, instance.getWorldName()));
    }

    if (!result.success()) {
      return Optional.of(error(result.reason(), player, instance.getWorldName()));
    }

    // Success: fill session context; DO NOT register here.
    ctx.setEntityId(result.playerName());
    ctx.setWorldId(result.worldName());
    ctx.setJoinedAt(OffsetDateTime.now());
    ctx.setAuthenticated(true);

    // Only return WELCOME. Registration happens after WELCOME is sent.
    return Optional.of(
        new ServerResponse(new Message(WELCOME, List.of(result.playerName())), result));
  }

  protected String resolvePlayer(Message message) {
    String playerName = message.args().getFirst();
    if (playerName == null || playerName.isEmpty()) {
      logAndThrow("[JOIN Command]: no player name given.");
    }

    return playerName;
  }

  protected WorldConnector resolveWorld(Message message) {
    log.info("[JOIN COMMAND]: Resolving world for message: {}", message);
    List<String> args = message.args();

    if (args.size() >= 2) {
      String worldName = args.get(1);

      WorldConnector world = WorldConnectorHolder.getWorld(worldName);
      if (world != null) {
        return world;
      }
      log.warn("[JOIN COMMAND]: Unknown world '{}', falling back to default", worldName);
    }

    return WorldConnectorHolder.getDefaultWorld();
  }

//  private Instance resolveWorldInstance(Message message) {
//    log.info("[JOIN COMMAND]: Resolving world instance for message: {}", message);
//    var args = message.args();
//    /// JOIN Message example with worldName: JOIN player_name world_name
//    /// JOIN without worldName: JOIN player_name -> will select default world
//
//    return args.stream()
//        .map(InstanceRegistry::getInstance)
//        .filter(Objects::nonNull).findFirst()
//        .orElseThrow(() -> new IllegalArgumentException(
//            String.format("No instance found for Message: %s", message)));
//  }

  private Instance resolveInstance(Message message) {
    String worldName = message.args().size() >= 2 ? message.args().get(1) : null;
    if (worldName == null) {
      worldName = WorldConnectorHolder.getDefaultWorld().getWorldName();
    }
    return InstanceRegistry.getInstance(worldName);
  }

  private ServerResponse error(String code, String player, String world) {
    return new ServerResponse(new Message(ERROR, List.of(code)),
        CommandResult.failed(player, world, code));
  }

}
