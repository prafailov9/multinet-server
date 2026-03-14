package com.ntros.command.orchestrate;

import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.util.List;

/**
 * Parses ORCHESTRATE arguments for Falling Sand worlds.
 *
 * <p>Supported sub-commands:
 * <ul>
 *   <li>{@code PLACE material x y} — place a material tile at (x, y)</li>
 *   <li>{@code RANDOM density} — randomly seed the grid with sand at the given density</li>
 *   <li>{@code CLEAR} — reset all tiles to air</li>
 * </ul>
 */
public class FsOrchestrateParser implements OrchestrateParser {

  @Override
  public OrchestrateRequest parse(List<String> args) {
    if (args.isEmpty()) {
      throw new IllegalArgumentException("ORCHESTRATE requires a sub-command.");
    }
    String sub = args.getFirst().toUpperCase();
    return switch (sub) {
      case "PLACE" -> {
        String material = args.get(1).toUpperCase();
        int x = Integer.parseInt(args.get(2));
        int y = Integer.parseInt(args.get(3));
        yield OrchestrateRequest.place(material, x, y);
      }
      case "RANDOM" -> {
        float density = Float.parseFloat(args.get(1));
        yield OrchestrateRequest.randomSeed(density);
      }
      case "CLEAR" -> OrchestrateRequest.clear();
      default -> throw new IllegalArgumentException(
          "Unknown FALLING_SAND ORCHESTRATE sub-command: " + sub
              + ". Valid: PLACE, RANDOM, CLEAR");
    };
  }
}
