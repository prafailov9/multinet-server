package com.ntros.command;

import static com.ntros.protocol.Message.errorMsg;
import static com.ntros.protocol.Message.welcomeMsg;

import com.ntros.command.exception.JoinCmdException;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.JoinRequest;
import com.ntros.protocol.Message;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to enable user to join a world.
 */
@Slf4j
public class JoinCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    try {
      SessionContext ctx = session.getSessionContext();
      checkAuthenticated(ctx);

      String player = resolvePlayer(message);
      Instance instance = resolveInstance(message);
      if (instance == null) {
        log.error("instance does not exist. Full client message: {}", message);
        return errorMsg(String.format("WORLD_NOT_FOUND for player: %s", player), ctx.getUsername());
      }

//    Settings settings = instance.getSettings();
//    // is not owner
//    if (settings.visibility() == PRIVATE) {
//      var owner = InstanceFactory.ownerOf(instance.getWorldName()).orElse(null);
//      if (owner != null && !owner.equals(ctx.getUsername())) {
//        return errorMsg(
//            String.format("WORLD_PRIVATE. instance:%s, player: %s", instance.getWorldName(),
//                player));
//      }
//    }
//    // is world full
//    if (settings.maxPlayers() == 1 && instance.isRunning() && instance instanceof ServerInstance wi
//        && wi.getActiveSessionsCount() >= 1) {
//      return new Message(ERROR,
//          List.of(String.format("WORLD_BUSY. instance:%s, player: %s", instance.getWorldName(),
//              player)));
//    }

      // TODO: make Command abstraction fully async.
      // try join. blocks on actor
      WorldResult result = instance.joinAsync(new JoinRequest(player)).join();
      if (result.success()) {
        ctx.setEntityId(result.playerName());
        ctx.setWorldId(result.worldName());
        ctx.setAuthenticated(true);
        ctx.setJoinedAt(OffsetDateTime.now());
        return welcomeMsg(result.playerName());
      }
      return errorMsg(result.reason());
    } catch (JoinCmdException | IllegalArgumentException ex) {
      log.error("exception occurred during JoinCommand: {}", ex.getMessage(), ex);
      return errorMsg(ex.getMessage());
    }
  }

  private void checkAuthenticated(SessionContext sessionContext) {
    if (!sessionContext.isAuthenticated()) {
      throw new JoinCmdException("client not authenticated");
    }
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
      throw new JoinCmdException("Message missing world-name argument");
    }
    return Instances.getInstance(worldName);
  }

}
