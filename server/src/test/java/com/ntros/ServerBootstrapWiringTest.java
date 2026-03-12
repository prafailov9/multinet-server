package com.ntros;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ntros.config.converter.WorldConverter;
import com.ntros.model.entity.config.WorldCapabilities;
import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.protocol.TileType;
import com.ntros.persistence.db.DatabaseBuilder;
import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.WorldRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration-level wiring test for server startup.
 *
 * <p>Validates that the entire world-loading chain — seed → DB → converter → connector —
 * produces live, correctly configured {@link WorldConnector} objects without touching the
 * filesystem DB or a real TCP socket.
 *
 * <p>Uses an in-memory SQLite database so each test method starts from a clean state.
 */
class ServerBootstrapWiringTest {

  private static Path terrainDir;

  @BeforeAll
  static void classSetUp() throws IOException {
    terrainDir = Files.createTempDirectory("wiring-test-terrain-");
  }

  @BeforeEach
  void setUp() {
    DatabaseBuilder.createDatabase(":memory:", terrainDir);
  }

  @AfterEach
  void tearDown() {
    DatabaseBuilder.dropDatabase();
  }

  // ── Seed ─────────────────────────────────────────────────────────────────

  @Nested
  class SeedDefaults {

    @Test
    void seedIfEmpty_insertsAllDefaultWorlds() {
      WorldSeedLoader.seedIfEmpty();

      List<WorldRecord> loaded = PersistenceContext.worlds().findAll();
      assertThat(loaded).hasSize(4);
      assertThat(loaded).extracting(WorldRecord::name)
          .containsExactlyInAnyOrder("arena-x-multi", "arena-y-multi", "gol-small", "gol-big");
    }

    @Test
    void seedIfEmpty_isIdempotent() {
      WorldSeedLoader.seedIfEmpty();
      WorldSeedLoader.seedIfEmpty(); // second call must not duplicate

      List<WorldRecord> loaded = PersistenceContext.worlds().findAll();
      assertThat(loaded).hasSize(4);
    }

    @Test
    void seedIfEmpty_doesNotThrow() {
      assertThatCode(WorldSeedLoader::seedIfEmpty).doesNotThrowAnyException();
    }
  }

  // ── World records ─────────────────────────────────────────────────────────

  @Nested
  class WorldRecordValues {

    @BeforeEach
    void seed() {
      WorldSeedLoader.seedIfEmpty();
    }

    @Test
    void arenaXMulti_hasCorrectDimensionsAndCapabilities() {
      WorldRecord record = PersistenceContext.worlds().findByName("arena-x-multi").orElseThrow();

      assertThat(record.engineType()).isEqualTo("GRID");
      assertThat(record.width()).isEqualTo(9);
      assertThat(record.height()).isEqualTo(12);
      assertThat(record.depth()).isEqualTo(0);
      assertThat(record.multiplayer()).isTrue();
      assertThat(record.orchestrated()).isTrue();
      assertThat(record.hasAi()).isFalse();
      assertThat(record.deterministic()).isTrue();
    }

    @Test
    void golSmall_hasCorrectDimensionsAndCapabilities() {
      WorldRecord record = PersistenceContext.worlds().findByName("gol-small").orElseThrow();

      assertThat(record.engineType()).isEqualTo("GOL");
      assertThat(record.width()).isEqualTo(18);
      assertThat(record.height()).isEqualTo(18);
      assertThat(record.multiplayer()).isTrue();
      assertThat(record.orchestrated()).isTrue();
    }

    @Test
    void golBig_isNotMultiplayerAndHasAI() {
      WorldRecord record = PersistenceContext.worlds().findByName("gol-big").orElseThrow();

      assertThat(record.engineType()).isEqualTo("GOL");
      assertThat(record.width()).isEqualTo(1024);
      assertThat(record.height()).isEqualTo(1024);
      assertThat(record.multiplayer()).isFalse();
      assertThat(record.hasAi()).isTrue();
    }
  }

  // ── WorldConverter ────────────────────────────────────────────────────────

  @Nested
  class ConverterBehaviour {

    private final WorldConverter converter = new WorldConverter();

    @Test
    void gridWorld_producesCorrectConnector() {
      WorldRecord record = worldRecord("arena-x-multi", "GRID", 9, 12,
          true, true, false, true);

      WorldConnector connector = converter.toModelObject(record);

      assertThat(connector.getWorldName()).isEqualTo("arena-x-multi");

      WorldCapabilities caps = connector.getCapabilities();
      assertThat(caps.supportsPlayers()).isTrue();
      assertThat(caps.supportsOrchestrator()).isTrue();
      assertThat(caps.hasAIEntities()).isFalse();
      assertThat(caps.isDeterministic()).isTrue();
    }

    @Test
    void golWorld_producesCorrectConnector() {
      WorldRecord record = worldRecord("gol-small", "GOL", 18, 18,
          true, true, false, true);

      WorldConnector connector = converter.toModelObject(record);

      assertThat(connector.getWorldName()).isEqualTo("gol-small");
      assertThat(connector.getCapabilities().supportsOrchestrator()).isTrue();
    }

    @Test
    void gridWorld_terrainContainsNonEmptyTiles() {
      WorldRecord record = worldRecord("arena-x-multi", "GRID", 9, 12,
          true, true, false, true);

      WorldConnector connector = converter.toModelObject(record);
      var terrain = ((GridWorldConnector) connector).getState().terrain();

      // A randomly generated grid world must have at least one non-EMPTY tile (wall/trap/water)
      Set<TileType> types = new java.util.HashSet<>(terrain.values());
      assertThat(types).isNotEqualTo(Set.of(TileType.EMPTY));
    }

    @Test
    void golWorld_terrainIsAllEmpty() {
      WorldRecord record = worldRecord("gol-small", "GOL", 18, 18,
          true, true, false, true);

      WorldConnector connector = converter.toModelObject(record);
      var terrain = ((GridWorldConnector) connector).getState().terrain();

      // GoL worlds start blank — every tile is EMPTY
      assertThat(terrain.values()).allMatch(t -> t == TileType.EMPTY);
      assertThat(terrain).hasSize(18 * 18);
    }

    @Test
    void golWorld_hasDimensionMatchingTileCount() {
      WorldRecord record = worldRecord("gol-big", "GOL", 1024, 1024,
          false, true, true, true);

      WorldConnector connector = converter.toModelObject(record);
      var terrain = ((GridWorldConnector) connector).getState().terrain();

      assertThat(terrain).hasSize(1024 * 1024);
    }
  }

  // ── Full startup chain ────────────────────────────────────────────────────

  @Nested
  class FullLoadChain {

    @Test
    void seedThenConvertAll_producesCorrectConnectors() {
      WorldSeedLoader.seedIfEmpty();

      WorldConverter converter = new WorldConverter();
      List<WorldConnector> connectors = PersistenceContext.worlds().findAll().stream()
          .map(converter::toModelObject)
          .toList();

      assertThat(connectors).hasSize(4);
      assertThat(connectors).extracting(WorldConnector::getWorldName)
          .containsExactlyInAnyOrder("arena-x-multi", "arena-y-multi", "gol-small", "gol-big");
    }

    @Test
    void allConnectors_haveExpectedCapabilities() {
      WorldSeedLoader.seedIfEmpty();
      WorldConverter converter = new WorldConverter();

      List<WorldConnector> connectors = PersistenceContext.worlds().findAll().stream()
          .map(converter::toModelObject)
          .toList();

      // All default worlds support orchestration
      assertThat(connectors)
          .allMatch(c -> c.getCapabilities().supportsOrchestrator(),
              "every world must support orchestration");

      // Only GoL worlds start with fully-empty terrain
      connectors.stream()
          .filter(c -> c.getWorldName().startsWith("gol"))
          .forEach(c -> {
            var terrain = ((GridWorldConnector) c).getState().terrain();
            assertThat(terrain.values()).allMatch(t -> t == TileType.EMPTY);
          });
    }

    @Test
    void noConnector_throwsDuringCreation() {
      WorldSeedLoader.seedIfEmpty();
      WorldConverter converter = new WorldConverter();

      assertThatCode(() ->
          PersistenceContext.worlds().findAll().stream()
              .map(converter::toModelObject)
              .toList()
      ).doesNotThrowAnyException();
    }
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private static WorldRecord worldRecord(String name, String engineType,
      int width, int height,
      boolean multiplayer, boolean orchestrated,
      boolean hasAi, boolean deterministic) {
    return new WorldRecord(name, engineType, width, height, 0,
        multiplayer, orchestrated, hasAi, deterministic,
        java.time.Instant.now());
  }
}
