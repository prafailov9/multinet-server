package com.ntros.command.orchestrate;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.world.protocol.request.OrchestrateAction;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses ORCHESTRATE arguments for Game-of-Life worlds.
 *
 * <p>Supported sub-commands:
 * <ul>
 *   <li>{@code SEED x0 y0 x1 y1 ...} — seed specific cells alive</li>
 *   <li>{@code RANDOM density} — randomly seed the grid at the given density (0.0–1.0)</li>
 *   <li>{@code TOGGLE x y} — flip the alive/dead state of a single cell</li>
 *   <li>{@code CLEAR} — reset all cells to dead</li>
 * </ul>
 */
public class GolOrchestrateParser implements OrchestrateParser {

  @Override
  public OrchestrateRequest parse(List<String> args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException("ORCHESTRATE requires a sub-command.");
    }
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
        yield new OrchestrateRequest(OrchestrateAction.TOGGLE,
            List.of(Position.of(x, y)), 0f, null);
      }
      case "CLEAR" -> OrchestrateRequest.clear();
      default -> throw new IllegalArgumentException(
          "Unknown GOL ORCHESTRATE sub-command: " + sub
              + ". Valid: SEED, RANDOM, TOGGLE, CLEAR");
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
