package com.ntros.command.orchestrate;

import com.ntros.model.world.protocol.request.OrchestrateRequest;
import java.util.List;

/**
 * Strategy for parsing raw ORCHESTRATE message arguments into a typed {@link OrchestrateRequest}.
 *
 * <p>Each world type that supports orchestration provides its own implementation.
 * {@code OrchestratorCommand} selects the correct parser at runtime based on the world's type
 * string (e.g. {@code "GAME_OF_LIFE"}, {@code "FALLING_SAND"}).
 */
@FunctionalInterface
public interface OrchestrateParser {

  /**
   * Parses the raw argument list from the client message into an {@link OrchestrateRequest}.
   *
   * @param args the message arguments (first element is the sub-command, e.g. "SEED", "RANDOM")
   * @return a fully constructed request
   * @throws IllegalArgumentException if the arguments are malformed or the sub-command is unknown
   */
  OrchestrateRequest parse(List<String> args);
}
