package com.ntros.command.impl;

import static com.ntros.model.world.protocol.CommandType.ERROR;
import static com.ntros.model.world.protocol.CommandType.WELCOME;

import com.ntros.instance.Instances;
import com.ntros.instance.InstanceFactory;
import com.ntros.instance.ins.Instance;
import com.ntros.instance.ins.ServerInstance;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.world.Connectors;
import com.ntros.model.world.protocol.response.CommandResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.response.ServerResponse;
import com.ntros.session.Session;
import java.time.OffsetDateTime;
import java.util.List;
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

  /**
   * Expected Message: JOIN client_name world_name
   *
   * @param message
   * @param session
   * @return
   */
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
    Settings cfg = instance.getSettings();
    if (cfg.visibility() == Visibility.PRIVATE) {
      var owner = InstanceFactory.ownerOf(instance.getWorldName()).orElse(null);
      if (owner != null && !owner.equals(ctx.getUserId())) {
        return Optional.of(error("WORLD_PRIVATE", player, instance.getWorldName()));
      }
    }
    if (cfg.maxPlayers() == 1 && instance.isRunning() && instance instanceof ServerInstance wi
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

  private Instance resolveInstance(Message message) {
    String worldName = message.args().size() >= 2 ? message.args().get(1) : null;
    if (worldName == null) {
      worldName = Connectors.getDefaultWorld().getWorldName();
    }
    return Instances.getInstance(worldName);
  }

  private ServerResponse error(String code, String player, String world) {
    return new ServerResponse(new Message(ERROR, List.of(code)),
        CommandResult.failed(player, world, code));
  }

}
