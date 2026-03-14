package com.ntros.command.orchestrate;

import com.ntros.model.entity.movement.grid.Position;
import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses ORCHESTRATE arguments for Wildfire worlds.
 *
 * <p>Supported sub-commands:
 * <ul>
 *   <li>{@code RANDOM density}          — seed forest at the given density (0.0–1.0)</li>
 *   <li>{@code PLACE material x y}      — place a cell type at (x, y)</li>
 *   <li>{@code SEED x1 y1 [x2 y2 ...]} — plant TREE at one or more positions</li>
 *   <li>{@code WIND direction speed}    — set wind direction and speed (0.0–1.0)</li>
 *   <li>{@code CLEAR}                   — reset the grid and calm the wind</li>
 * </ul>
 */
public class WildfireOrchestrateParser implements OrchestrateParser {

  @Override
  public OrchestrateRequest parse(List<String> args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException("ORCHESTRATE requires a sub-command.");
    }
    String sub = args.getFirst().toUpperCase();
    return switch (sub) {
      case "RANDOM" -> {
        float density = Float.parseFloat(args.get(1));
        yield OrchestrateRequest.randomSeed(density);
      }
      case "PLACE" -> {
        String material = args.get(1).toUpperCase();
        int x = Integer.parseInt(args.get(2));
        int y = Integer.parseInt(args.get(3));
        yield OrchestrateRequest.place(material, x, y);
      }
      case "SEED" -> {
        // SEED x1 y1 x2 y2 ... — pairs of coordinates
        List<Position> cells = new ArrayList<>();
        for (int i = 1; i + 1 < args.size(); i += 2) {
          int x = Integer.parseInt(args.get(i));
          int y = Integer.parseInt(args.get(i + 1));
          cells.add(Position.of(x, y));
        }
        if (cells.isEmpty()) {
          throw new IllegalArgumentException("SEED requires at least one x y pair.");
        }
        yield OrchestrateRequest.seed(cells);
      }
      case "WIND" -> {
        String direction = args.get(1).toUpperCase();
        float speed = Float.parseFloat(args.get(2));
        yield OrchestrateRequest.setWind(direction, speed);
      }
      case "CLEAR" -> OrchestrateRequest.clear();
      default -> throw new IllegalArgumentException(
          "Unknown WILDFIRE ORCHESTRATE sub-command: " + sub
              + ". Valid: RANDOM, PLACE, SEED, WIND, CLEAR");
    };
  }
}
