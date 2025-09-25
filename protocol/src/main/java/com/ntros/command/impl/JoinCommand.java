package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ERROR;
import static com.ntros.model.world.protocol.CommandType.WELCOME;

import com.ntros.message.ClientProfile;
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
    ClientProfile ctx = session.getProtocolContext();
    String playerName = resolvePlayer(message);
    WorldConnector world = resolveWorld(message);

    // Decide policy/visibility yourself (as before) — omitted for brevity.

    // enqueue the add(); choose ACK immediately or wait:
    // Option A (immediate): world.add(...) enqueues and returns success ACK
    CommandResult res = world.add(new JoinRequest(playerName));

    // set context, register session, etc. (as you do now)
    ctx.setEntityId(res.playerName());
    ctx.setWorldId(res.worldName());
    ctx.setAuthenticated(true);
    return Optional.of(handleServerResponse(res, ctx));
//    return Optional.of(new ServerResponse(new Message(WELCOME, List.of(res.playerName())), res));
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

  private ServerResponse handleServerResponse(CommandResult commandResult,
      ClientProfile clientProfile) {
    if (commandResult.success()) {
      clientProfile.setEntityId(commandResult.playerName());
      clientProfile.setWorldId(commandResult.worldName());
      clientProfile.setJoinedAt(OffsetDateTime.now());
      clientProfile.setAuthenticated(true);
      log.info("[JOIN Command]: success. Sending WELCOME response to client: {}", clientProfile);
      return new ServerResponse(new Message(WELCOME, List.of(commandResult.playerName())),
          commandResult);
    }
    clientProfile.setAuthenticated(false);
    String err = String.format("%s %s\n", ERROR.name(), commandResult.reason());
    log.error("[JOIN Command]: failure. Sending ERROR response: {}", err);

    return new ServerResponse(new Message(ERROR, List.of(
        commandResult.reason())), commandResult);
  }

}
