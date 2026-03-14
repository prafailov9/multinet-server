package com.ntros.command;

import static com.ntros.protocol.Message.ack;
import static com.ntros.protocol.Message.errorMsg;

import com.ntros.command.access.InstanceAccessController;
import com.ntros.command.orchestrate.FsOrchestrateParser;
import com.ntros.command.orchestrate.GolOrchestrateParser;
import com.ntros.command.orchestrate.OrchestrateParser;
import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.protocol.Message;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Processes ORCHESTRATE commands from connected clients.
 *
 * <p>Access is checked via {@link InstanceAccessController} before parsing the request.
 * Parsing is delegated to the world-type-specific {@link OrchestrateParser}.
 */
@Slf4j
public class OrchestratorCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    log.info("[OrchestratorCommand]: received orchestration message: {}", message);
    try {
      SessionContext ctx = session.getSessionContext();

      String worldName = ctx.getWorldName();
      Instance instance = Instances.getInstance(worldName);
      if (instance == null) {
        log.error("[ORCHESTRATE] no instance for world '{}'", worldName);
        return errorMsg("World not found: " + worldName);
      }

      // Access guard — capability + role check
      Optional<String> denied = InstanceAccessController.checkOrchestrate(ctx, instance);
      if (denied.isPresent()) {
        log.warn("[ORCHESTRATE] access denied for session {}: {}", ctx.getSessionId(),
            denied.get());
        return errorMsg(denied.get());
      }

      // Parse using the world-type-specific parser
      OrchestrateParser parser = resolveParser(instance.getWorldConnector().getWorldType());
      OrchestrateRequest req = parser.parse(message.args());

      WorldResult result = instance.orchestrateAsync(req).join();
      if (result.success()) {
        return ack(ctx.getSessionId());
      }
      return errorMsg(result.reason());

    } catch (IllegalArgumentException ex) {
      log.error("[ORCHESTRATE] parse error: {}", ex.getMessage());
      return errorMsg("ORCHESTRATE parse error: " + ex.getMessage());
    } catch (Exception ex) {
      log.error("[ORCHESTRATE] unexpected error: {}", ex.getMessage(), ex);
      return errorMsg("Internal error during ORCHESTRATE.");
    }
  }

  // ── Parser selection ────────────────────────────────────────────────────────

  private OrchestrateParser resolveParser(String worldType) {
    return switch (worldType.toUpperCase()) {
      case "GAME_OF_LIFE" -> new GolOrchestrateParser();
      case "FALLING_SAND" -> new FsOrchestrateParser();
      default -> throw new IllegalArgumentException(
          "No ORCHESTRATE parser registered for world type: " + worldType);
    };
  }
}
