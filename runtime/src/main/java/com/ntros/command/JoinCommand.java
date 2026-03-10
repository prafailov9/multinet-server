package com.ntros.command;

import static com.ntros.protocol.CommandType.ERROR;
import static com.ntros.protocol.CommandType.WELCOME;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.InstanceFactory;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.protocol.Message;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.protocol.response.ServerResponse;
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

  @Override
  public Optional<ServerResponse> execute(Message message, Session session) {
    SessionContext sessionContext = session.getSessionContext();
    String player = resolvePlayer(message);

    Instance instance = resolveInstance(message);
    if (instance == null) {
      log.error("instance does not exist. Full client message: {}", message);
      // TODO: change Command return types to CommandResult(vo package)
      return Optional.of(error("WORLD_NOT_FOUND", player, null));
    }

    Settings settings = instance.getSettings();
    if (settings.visibility() == Visibility.PRIVATE) {
      var owner = InstanceFactory.ownerOf(instance.getWorldName()).orElse(null);
      if (owner != null && !owner.equals(sessionContext.getUserId())) {
        return Optional.of(error("WORLD_PRIVATE", player, instance.getWorldName()));
      }
    }
    if (settings.maxPlayers() == 1 && instance.isRunning() && instance instanceof ServerInstance wi
        && wi.getActiveSessionsCount() >= 1) {
      return Optional.of(error("WORLD_BUSY", player, instance.getWorldName()));
    }

    CompletableFuture<WorldResult> fut = instance.joinAsync(new JoinRequest(player));
    instance.startIfNeededForJoin();

    WorldResult result;
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

    sessionContext.setEntityId(result.playerName());
    sessionContext.setWorldId(result.worldName());
    sessionContext.setJoinedAt(OffsetDateTime.now());
    sessionContext.setAuthenticated(true);

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
      log.error("no world name in JOIN message {}", message);
      throw new IllegalArgumentException("Message missing world-name argument");
    }
    return Instances.getInstance(worldName);
  }

  private ServerResponse error(String code, String player, String world) {
    return new ServerResponse(new Message(ERROR, List.of(code)),
        WorldResult.failed(player, world, code));
  }

}
