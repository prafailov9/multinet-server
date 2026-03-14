package com.ntros.command;

import static com.ntros.protocol.Message.errorMsg;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Returns the current world snapshot for the client's joined world.
 *
 * <p>The client sends {@code STATE} to request an immediate snapshot without waiting for
 * the next scheduled broadcast.  The response is a {@code STATE worldName snapshotJson}
 * message serialised via the world's own snapshot serialiser.
 *
 * <p><b>Threading note:</b> the snapshot is read from the world state outside the actor
 * thread, so it may not be perfectly synchronised with the current tick.  For authoritative
 * state the broadcast stream (pushed by the server at the configured rate) is preferred.
 */
@Slf4j
public class StateCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    String worldName = ctx.getWorldName();

    if (worldName == null || worldName.isBlank()) {
      return errorMsg("Not joined to any world.");
    }

    Instance instance = Instances.getInstance(worldName);
    if (instance == null) {
      log.error("[STATE] no instance for world '{}'", worldName);
      return errorMsg("World not found: " + worldName);
    }

    try {
      String snapshotJson = instance.getWorldConnector().snapshot(true);
      return new Message(CommandType.STATE, List.of(worldName, snapshotJson));
    } catch (Exception ex) {
      log.error("[STATE] snapshot failed for world '{}': {}", worldName, ex.getMessage(), ex);
      return errorMsg("Failed to retrieve state for world: " + worldName);
    }
  }
}
