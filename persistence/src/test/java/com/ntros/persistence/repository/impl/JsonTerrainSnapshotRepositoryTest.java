package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.model.entity.movement.Position;
import com.ntros.model.world.protocol.TileType;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link JsonTerrainSnapshotRepository}.
 *
 * <p>{@link TempDir} gives each test an isolated, automatically-deleted directory, so no
 * snapshot files leak between tests or onto the developer's filesystem.
 */
class JsonTerrainSnapshotRepositoryTest {

  @TempDir
  Path tempDir;

  private TerrainSnapshotRepository repo;

  @BeforeEach
  void setUp() {
    repo = new JsonTerrainSnapshotRepository(tempDir);
  }

  // ── exists ────────────────────────────────────────────────────────────────

  @Test
  void exists_noSnapshotYet_returnsFalse() {
    assertThat(repo.exists("arena-x")).isFalse();
  }

  @Test
  void exists_afterSave_returnsTrue() {
    repo.save("arena-x", smallTerrain());
    assertThat(repo.exists("arena-x")).isTrue();
  }

  // ── load ──────────────────────────────────────────────────────────────────

  @Test
  void load_noSnapshotYet_returnsEmpty() {
    Optional<Map<Position, TileType>> result = repo.load("nonexistent");
    assertThat(result).isEmpty();
  }

  @Test
  void load_afterSave_returnsSameTerrain() {
    Map<Position, TileType> terrain = smallTerrain();
    repo.save("arena-x", terrain);

    Map<Position, TileType> loaded = repo.load("arena-x").orElseThrow();
    assertThat(loaded).containsExactlyInAnyOrderEntriesOf(terrain);
  }

  // ── save ──────────────────────────────────────────────────────────────────

  @Test
  void save_overwritesPreviousSnapshot() {
    Map<Position, TileType> v1 = Map.of(Position.of(0, 0), TileType.EMPTY);
    Map<Position, TileType> v2 = Map.of(Position.of(0, 0), TileType.WALL);

    repo.save("world", v1);
    repo.save("world", v2);

    Map<Position, TileType> loaded = repo.load("world").orElseThrow();
    assertThat(loaded.get(Position.of(0, 0))).isEqualTo(TileType.WALL);
  }

  @Test
  void save_multipleTileTypes_allPreserved() {
    Map<Position, TileType> terrain = new LinkedHashMap<>();
    terrain.put(Position.of(0, 0), TileType.EMPTY);
    terrain.put(Position.of(1, 0), TileType.WALL);
    terrain.put(Position.of(2, 0), TileType.WATER);
    terrain.put(Position.of(3, 0), TileType.TRAP);

    repo.save("mixed", terrain);
    Map<Position, TileType> loaded = repo.load("mixed").orElseThrow();

    assertThat(loaded.get(Position.of(0, 0))).isEqualTo(TileType.EMPTY);
    assertThat(loaded.get(Position.of(1, 0))).isEqualTo(TileType.WALL);
    assertThat(loaded.get(Position.of(2, 0))).isEqualTo(TileType.WATER);
    assertThat(loaded.get(Position.of(3, 0))).isEqualTo(TileType.TRAP);
  }

  // ── multi-world isolation ─────────────────────────────────────────────────

  @Test
  void save_differentWorlds_doNotInterfer() {
    repo.save("world-a", Map.of(Position.of(0, 0), TileType.WALL));
    repo.save("world-b", Map.of(Position.of(0, 0), TileType.WATER));

    assertThat(repo.load("world-a").orElseThrow().get(Position.of(0, 0)))
        .isEqualTo(TileType.WALL);
    assertThat(repo.load("world-b").orElseThrow().get(Position.of(0, 0)))
        .isEqualTo(TileType.WATER);
  }

  // ── world name sanitisation ───────────────────────────────────────────────

  @Test
  void save_worldNameWithSpecialChars_isSanitisedToSafeFilename() {
    // World names with slashes or spaces should not create subdirectories or fail
    repo.save("arena/1 test!", Map.of(Position.of(0, 0), TileType.EMPTY));
    assertThat(repo.exists("arena/1 test!")).isTrue();
    assertThat(repo.load("arena/1 test!")).isPresent();
  }

  // ── large terrain roundtrip ───────────────────────────────────────────────

  @Test
  void save_largeGridTerrain_roundtripsWithoutLoss() {
    Map<Position, TileType> terrain = new LinkedHashMap<>();
    TileType[] types = TileType.values();
    int idx = 0;
    for (int x = 0; x < 20; x++) {
      for (int y = 0; y < 20; y++) {
        terrain.put(Position.of(x, y), types[idx++ % types.length]);
      }
    }

    repo.save("big-world", terrain);
    Map<Position, TileType> loaded = repo.load("big-world").orElseThrow();

    assertThat(loaded).hasSize(400);
    assertThat(loaded).containsExactlyInAnyOrderEntriesOf(terrain);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void delete_existingSnapshot_removesFile() {
    repo.save("arena-x", smallTerrain());
    assertThat(repo.exists("arena-x")).isTrue();

    repo.delete("arena-x");

    assertThat(repo.exists("arena-x")).isFalse();
    assertThat(repo.load("arena-x")).isEmpty();
  }

  @Test
  void delete_nonExistentSnapshot_isNoOp() {
    // Must not throw
    repo.delete("ghost-world");
    assertThat(repo.exists("ghost-world")).isFalse();
  }

  private static Map<Position, TileType> smallTerrain() {
    Map<Position, TileType> m = new LinkedHashMap<>();
    m.put(Position.of(0, 0), TileType.EMPTY);
    m.put(Position.of(1, 0), TileType.WALL);
    m.put(Position.of(0, 1), TileType.TRAP);
    m.put(Position.of(1, 1), TileType.WATER);
    return m;
  }
}
