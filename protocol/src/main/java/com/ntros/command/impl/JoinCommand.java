package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ERROR;
import static com.ntros.model.world.protocol.CommandType.WELCOME;

import com.ntros.instance.InstanceRegistry;
import com.ntros.instance.WorldRegistry;
import com.ntros.instance.ins.Instance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.config.access.InstanceConfig;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.world.WorldConnectorHolder;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.CommandResult;
import com.ntros.model.world.protocol.JoinRequest;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to enable user to join a multiplayer world.
 */
@Slf4j
public class JoinCommand extends AbstractCommand {

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext sessionContext = session.getSessionContext();
    String playerName = resolvePlayer(message);
    WorldConnector world = resolveWorld(message);

    Instance instance = InstanceRegistry.get(world.worldName());
    if (instance == null) {
      return Optional.of(new ServerResponse(
          new Message(ERROR, List.of("WORLD_NOT_FOUND")),
          CommandResult.failed(playerName, null, "world not found")
      ));
    }

    // Visibility / capacity checks
    InstanceConfig cfg = instance.getConfig();
    if (cfg.visibility() == Visibility.PRIVATE) {
      var owner = WorldRegistry.ownerOf(instance.getWorldName()).orElse(null);
      if (owner != null && !owner.equals(sessionContext.getUserId())) {
        return Optional.of(new ServerResponse(
            new Message(ERROR, List.of("WORLD_PRIVATE")),
            CommandResult.failed(playerName, instance.getWorldName(), "private")
        ));
      }
    }
    if (cfg.maxPlayers() == 1 && instance.getActiveSessionsCount() >= 1) {
      return Optional.of(new ServerResponse(
          new Message(ERROR, List.of("WORLD_BUSY")),
          CommandResult.failed(playerName, instance.getWorldName(), "busy")
      ));
    }

    // Execute command
    CommandResult commandResult = world.add(new JoinRequest(playerName));

//    return Optional.of(handleServerResponse(result, ctx));

    if (commandResult.success()) {
      sessionContext.setEntityId(commandResult.playerName());
      sessionContext.setWorldId(commandResult.worldName());
      sessionContext.setJoinedAt(OffsetDateTime.now());
      sessionContext.setAuthenticated(true);

      instance.registerSession(session);

      log.info("[JOIN Command]: success. Sending WELCOME response to client: {}", sessionContext);
      return Optional.of(
          new ServerResponse(new Message(WELCOME, List.of(commandResult.playerName())),
              commandResult));
    }
    sessionContext.setAuthenticated(false);
    String err = String.format("%s %s\n", ERROR.name(), commandResult.reason());
    log.error("[JOIN Command]: failure. Sending ERROR response: {}", err);

    return Optional.of(new ServerResponse(new Message(ERROR, List.of(
        commandResult.reason())), commandResult));

  }

  protected String resolvePlayer(Message message) {
    String playerName = message.args().getFirst();
    if (playerName == null || playerName.isEmpty()) {
      logAndThrow("[JOIN Command]: no player name given.");
    }

    return playerName;
  }

  protected WorldConnector resolveWorld(Message message) {
    log.info("[JOIN COMMAND]: All worlds: {}",
        WorldConnectorHolder.getAllWorlds().stream().map(WorldConnector::worldName)
            .collect(Collectors.toList()));
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
//
//  private ServerResponse handleServerResponse(CommandResult commandResult,
//      SessionContext sessionContext) {
//    if (commandResult.success()) {
//      sessionContext.setEntityId(commandResult.playerName());
//      sessionContext.setWorldId(commandResult.worldName());
//      sessionContext.setJoinedAt(OffsetDateTime.now());
//      sessionContext.setAuthenticated(true);
//
//      log.info("[JOIN Command]: success. Sending WELCOME response to client: {}", sessionContext);
//      return new ServerResponse(new Message(WELCOME, List.of(commandResult.playerName())),
//          commandResult);
//    }
//    sessionContext.setAuthenticated(false);
//    String err = String.format("%s %s\n", ERROR.name(), commandResult.reason());
//    log.error("[JOIN Command]: failure. Sending ERROR response: {}", err);
//
//    return new ServerResponse(new Message(ERROR, List.of(
//        commandResult.reason())), commandResult);
//  }

}
