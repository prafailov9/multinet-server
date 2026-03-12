package com.ntros.command;

import static com.ntros.model.entity.config.access.Role.ORCHESTRATOR;
import static com.ntros.protocol.Message.ack;
import static com.ntros.protocol.Message.errorMsg;

import com.ntros.lifecycle.instance.Instance;
import com.ntros.lifecycle.instance.Instances;
import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.model.entity.config.access.Role;
import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.world.protocol.WorldResult;
import com.ntros.model.world.protocol.request.OrchestrateAction;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.protocol.Message;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@code ORCHESTRATE} commands for Game-of-Life (and similar) worlds.
 *
 * <p>Wire format (args after "ORCHESTRATE"):
 * <ul>
 *   <li>{@code SEED x1 y1 x2 y2 ...}   — make the listed cells alive (additive)</li>
 *   <li>{@code RANDOM <density>}         — random seed at the given density (0.0–1.0)</li>
 *   <li>{@code TOGGLE x y}              — flip the live/dead state of one cell</li>
 *   <li>{@code CLEAR}                   — kill all live cells</li>
 * </ul>
 */
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

  // ── Parsing ────────────────────────────────────────────────────────────────

  private OrchestrateRequest parseRequest(List<String> args) {
    String sub = args.getFirst().toUpperCase();
    return switch (sub) {
      case "SEED" -> {
        List<Position> cells = parseCellPairs(args, 1);
        yield new OrchestrateRequest(OrchestrateAction.SEED, cells, 0f);
      }
      case "RANDOM" -> {
        float density = Float.parseFloat(args.get(1));
        yield OrchestrateRequest.randomSeed(density);
      }
      case "TOGGLE" -> {
        int x = Integer.parseInt(args.get(1));
        int y = Integer.parseInt(args.get(2));
        yield new OrchestrateRequest(OrchestrateAction.TOGGLE, List.of(Position.of(x, y)), 0f);
      }
      case "CLEAR" -> OrchestrateRequest.clear();
      default -> throw new IllegalArgumentException(
          "Unknown ORCHESTRATE sub-command: " + sub + ". Valid: SEED, RANDOM, TOGGLE, CLEAR");
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
