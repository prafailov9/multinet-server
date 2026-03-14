package com.ntros.config.converter;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.fallingsand.FallingSandEngine;
import com.ntros.model.world.engine.d2.grid.gameoflife.GameOfLifeEngine;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.state.grid.ArenaGridState;
import com.ntros.model.world.state.grid.FallingSandState;
import com.ntros.persistence.model.WorldRecord;

/**
 * Converts a {@link WorldRecord} (database row) into a live {@link WorldConnector} ready to be
 * registered with the server.
 *
 * <p>Engine selection is driven by the {@code engine_type} column:
 * <ul>
 *   <li>{@code "GOL"} → {@link GameOfLifeEngine} + blank (all-EMPTY) terrain</li>
 *   <li>{@code "GRID"} (default) → {@link GridWorldEngine} + randomly generated terrain</li>
 * </ul>
 */
public class WorldConverter implements Converter<WorldRecord, WorldConnector> {

  /** Not used — world records are the source of truth, not the destination. */
  @Override
  public WorldRecord toFileObject(WorldConnector modelObject) {
    return null;
  }

  @Override
  public WorldConnector toModelObject(WorldRecord record) {
    WorldCapabilities capabilities = new WorldCapabilities(
        record.multiplayer(),
        record.orchestrated(),
        record.hasAi(),
        record.deterministic()
    );

    return switch (record.engineType().toUpperCase()) {
      case "FALLING_SAND" -> new GridWorldConnector(
          new FallingSandState(record.name(), record.width(), record.height()),
          new FallingSandEngine(), capabilities);
      case "GOL" -> new GridWorldConnector(
          ArenaGridState.blank(record.name(), record.width(), record.height()),
          new GameOfLifeEngine(), capabilities);
      default -> new GridWorldConnector(
          new ArenaGridState(record.name(), record.width(), record.height()),
          new GridWorldEngine(), capabilities);
    };
  }
}
