package com.ntros.command;

import static com.ntros.protocol.CommandType.ERROR;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.InstanceFactory;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.instance.ServerInstance;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.model.entity.config.access.Settings;
import com.ntros.model.entity.config.access.Visibility;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.protocol.Message;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to enable user to join a multiplayer world.
 */
@Slf4j
public class JoinCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    SessionContext sessionContext = session.getSessionContext();
    String player = resolvePlayer(message);

    Instance instance = resolveInstance(message);
    if (instance == null) {
      log.error("instance does not exist. Full client message: {}", message);
      return new Message(ERROR, List.of(String.format("WORLD_NOT_FOUND for player: %s", player)));
    }

    Settings settings = instance.getSettings();
    // is not owner
    if (settings.visibility() == Visibility.PRIVATE) {
      var owner = InstanceFactory.ownerOf(instance.getWorldName()).orElse(null);
      if (owner != null && !owner.equals(sessionContext.getUserId())) {
        return new Message(ERROR,
            List.of(String.format("WORLD_PRIVATE. instance:%s, player: %s", instance.getWorldName(),
                player)));
      }
    }
    // is world full
    if (settings.maxPlayers() == 1 && instance.isRunning() && instance instanceof ServerInstance wi
        && wi.getActiveSessionsCount() >= 1) {
      return new Message(ERROR,
          List.of(String.format("WORLD_BUSY. instance:%s, player: %s", instance.getWorldName(),
              player)));
    }

    // TODO: make Command abstraction fully async.
    // try join
    WorldResult result = instance.joinAsync(new JoinRequest(player)).join();
    if (result.success()) {
      sessionContext.setEntityId(result.playerName());
      sessionContext.setWorldId(result.worldName());
      sessionContext.setAuthenticated(true);
      sessionContext.setJoinedAt(OffsetDateTime.now());
      return Message.welcome(result.playerName());
    }
    return new Message(ERROR, List.of(result.reason()));
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

}
