package com.ntros.command;

import static com.ntros.model.entity.config.access.Role.ORCHESTRATOR;
import static com.ntros.protocol.Message.ack;
import static com.ntros.protocol.Message.errorMsg;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.world.protocol.result.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateAction;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.protocol.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrchestratorCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    log.info("[OrchestratorCommand]: received orchestration message: {}", message);
    try {
      SessionContext ctx = session.getSessionContext();
      List<String> args = message.args();

      OrchestrateRequest req = parseRequest(args);
      String worldName = ctx.getWorldName();
      Instance instance = Instances.getInstance(worldName);
      if (instance == null) {
        log.error("[ORCHESTRATE] no instance for world '{}'", worldName);
        return errorMsg("World not found: " + worldName);
      }

      ctx.setRole(ORCHESTRATOR);
      PersistenceContext.clients().updateRole(ctx.getUsername(), ORCHESTRATOR.name());
      // submit on actor, session-vt proceeds
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


  // TODO: move parsing into diff abstraction
  // ── Parsing ────────────────────────────────────────────────────────────────

  private OrchestrateRequest parseRequest(List<String> args) {
    String sub = args.getFirst().toUpperCase();
    return switch (sub) {
      case "SEED" -> {
        List<Position> cells = parseCellPairs(args, 1);
        yield new OrchestrateRequest(OrchestrateAction.SEED, cells, 0f, null);
      }
      case "RANDOM" -> {
        float density = Float.parseFloat(args.get(1));
        yield OrchestrateRequest.randomSeed(density);
      }
      case "TOGGLE" -> {
        int x = Integer.parseInt(args.get(1));
        int y = Integer.parseInt(args.get(2));
        yield new OrchestrateRequest(OrchestrateAction.TOGGLE, List.of(Position.of(x, y)), 0f, null);
      }
      case "CLEAR" -> OrchestrateRequest.clear();
      case "PLACE" -> {
        String material = args.get(1).toUpperCase();
        int x = Integer.parseInt(args.get(2));
        int y = Integer.parseInt(args.get(3));
        yield OrchestrateRequest.place(material, x, y);
      }
      default -> throw new IllegalArgumentException(
          "Unknown ORCHESTRATE sub-command: " + sub + ". Valid: SEED, RANDOM, TOGGLE, CLEAR, PLACE");
    };
  }

  /**
   * Parses consecutive (x, y) integer pairs from {@code args} starting at {@code fromIndex}.
   *
   * @throws IllegalArgumentException if the tail has an odd number of tokens or bad integers
   */
  private List<Position> parseCellPairs(List<String> args, int fromIndex) {
    List<String> tail = args.subList(fromIndex, args.size());
    if (tail.size() % 2 != 0) {
      throw new IllegalArgumentException(
          "SEED expects an even number of x/y coordinates, got " + tail.size() + " token(s).");
    }
    List<Position> cells = new ArrayList<>(tail.size() / 2);
    for (int i = 0; i < tail.size(); i += 2) {
      int x = Integer.parseInt(tail.get(i));
      int y = Integer.parseInt(tail.get(i + 1));
      cells.add(Position.of(x, y));
    }
    return cells;
  }
}
