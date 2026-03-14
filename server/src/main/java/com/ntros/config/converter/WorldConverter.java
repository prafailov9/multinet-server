package com.ntros.config.converter;

import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.PlayerWorldConnector;
import com.ntros.model.world.connector.SimulationWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.d2.grid.fallingsand.FallingSandEngine;
import com.ntros.model.world.engine.d2.grid.gameoflife.GameOfLifeEngine;
import com.ntros.model.world.engine.d2.grid.GridWorldEngine;
import com.ntros.model.world.state.grid.ArenaGridState;
import com.ntros.model.world.state.grid.FallingSandState;
import com.ntros.model.world.state.grid.GameOfLifeState;
import com.ntros.persistence.model.WorldRecord;

/**
 * Converts a {@link WorldRecord} (database row) into a live {@link WorldConnector} ready to be
 * registered with the server.
 *
 * <p>Engine selection is driven by the {@code engine_type} column:
 * <ul>
 *   <li>{@code "GOL"} → {@link GameOfLifeEngine} + blank {@link GameOfLifeState}</li>
 *   <li>{@code "FALLING_SAND"} → {@link FallingSandEngine} + {@link FallingSandState}</li>
 *   <li>{@code "GRID"} (default) → {@link GridWorldEngine} + {@link ArenaGridState}</li>
 * </ul>
 *
 * <p>World capabilities are derived from the engine type via the typed factory methods on
 * {@link WorldCapabilities}, rather than being reconstructed from the individual DB columns.
 */
public class WorldConverter implements Converter<WorldRecord, WorldConnector> {

  /** Not used — world records are the source of truth, not the destination. */
  @Override
  public WorldRecord toFileObject(WorldConnector modelObject) {
    return null;
  }

  @Override
  public WorldConnector toModelObject(WorldRecord record) {
    return switch (record.engineType().toUpperCase()) {
      case "FALLING_SAND" -> new SimulationWorldConnector(
          new FallingSandState(record.name(), record.width(), record.height()),
          new FallingSandEngine(),
          WorldCapabilities.fallingSand());
      case "GOL" -> new SimulationWorldConnector(
          new GameOfLifeState(record.name(), record.width(), record.height()),
          new GameOfLifeEngine(),
          WorldCapabilities.gameOfLife());
      default -> new PlayerWorldConnector(
          new ArenaGridState(record.name(), record.width(), record.height()),
          new GridWorldEngine(),
          WorldCapabilities.arena());
    };
  }
}
