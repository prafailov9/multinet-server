package com.ntros.command;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.model.entity.config.access.InstanceVisibility;
import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Responds to a client {@code WELCOME} request with a list of publicly joinable worlds.
 *
 * <p>The response is a {@code WELCOME world1 world2 ...} message.  Only worlds whose
 * {@link InstanceVisibility} is {@code PUBLIC} or {@code JOINABLE} are included — PRIVATE
 * instances are hidden from the listing.
 *
 * <p>This command is typically sent by the client immediately after AUTH to discover
 * available worlds before issuing a JOIN.
 */
@Slf4j
public class WelcomeCommand extends AbstractCommand {

  private static final String SERVER_VERSION = "multinet-1.0";

  @Override
  public Message execute(Message message, Session session) {
    List<String> visibleWorlds = Instances.getAll().stream()
        .filter(i -> isVisible(i))
        .map(Instance::getWorldName)
        .sorted()
        .toList();

    // Response: WELCOME <serverVersion> <world1> <world2> ...
    List<String> args = new java.util.ArrayList<>();
    args.add(SERVER_VERSION);
    args.addAll(visibleWorlds);

    log.info("[WELCOME] returning {} visible world(s) to session {}",
        visibleWorlds.size(), session.getSessionContext().getSessionId());
    return new Message(CommandType.WELCOME, args);
  }

  private boolean isVisible(Instance instance) {
    InstanceVisibility vis = instance.getSettings().instanceVisibility();
    return vis == InstanceVisibility.PUBLIC || vis == InstanceVisibility.JOINABLE;
  }
}
