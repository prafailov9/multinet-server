package com.ntros.guard;

import com.ntros.instance.WorldRegistry;
import com.ntros.model.entity.config.access.Role;
import com.ntros.model.world.protocol.Message;
import com.ntros.session.Session;

public class CapabilityGuard implements CommandGuard {

  @Override
  public void check(Message msg, Session s) {
    switch (msg.commandType()) {
      case MOVE -> {
        String world = currentWorld(msg, s);
        var inst = WorldRegistry.get(world);
        if (inst == null) {
          throw new IllegalArgumentException("WORLD_NOT_FOUND");
        }
        if (!inst.getWorldConnector().getCapabilities().supportsPlayers()) {
          throw new IllegalStateException("WORLD_NO_PLAYERS");
        }
      }
      case ORCHESTRATE -> {
        var ctx = s.getSessionContext();
        if (ctx.getRole() != Role.ORCHESTRATOR) {
          throw new IllegalStateException("NOT_ORCHESTRATOR");
        }
        String world = currentWorld(msg, s);
        var inst = WorldRegistry.get(world);
        if (!inst.getWorldConnector().getCapabilities().supportsOrchestrator()) {
          throw new IllegalStateException("WORLD_NO_ORCHESTRATOR");
        }
      }
      default -> {
      }
    }
  }

  private String currentWorld(Message m, Session s) {
    return !m.args().isEmpty() ? m.args().getFirst() : s.getSessionContext().getWorldName();
  }
}
