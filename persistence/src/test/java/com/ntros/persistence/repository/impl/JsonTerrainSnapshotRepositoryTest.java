package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.model.entity.movement.cell.Position;
import com.ntros.model.entity.movement.vectors.Vector4;
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

  @Test
  void exists_noSnapshotYet_returnsFalse() {
    assertThat(repo.exists("arena-x")).isFalse();
  }

  @Test
  void exists_afterSave_returnsTrue() {
    repo.save("arena-x", smallTerrain());
    assertThat(repo.exists("arena-x")).isTrue();
  }

  @Test
  void load_noSnapshotYet_returnsEmpty() {
    Optional<Map<Vector4, TileType>> result = repo.load("nonexistent");
    assertThat(result).isEmpty();
  }

  @Test
  void load_afterSave_returnsSameTerrain() {
    Map<Vector4, TileType> terrain = smallTerrain();
    repo.save("arena-x", terrain);

    Map<Vector4, TileType> loaded = repo.load("arena-x").orElseThrow();
    assertThat(loaded).containsExactlyInAnyOrderEntriesOf(terrain);
  }

  @Test
  void save_overwritesPreviousSnapshot() {
    Map<Vector4, TileType> v1 = Map.of(Vector4.of(0, 0, 0, 0), TileType.EMPTY);
    Map<Vector4, TileType> v2 = Map.of(Vector4.of(0, 0, 0, 0), TileType.WALL);

    repo.save("world", v1);
    repo.save("world", v2);

    Map<Vector4, TileType> loaded = repo.load("world").orElseThrow();
    assertThat(loaded.get(Vector4.of(0, 0, 0, 0))).isEqualTo(TileType.WALL);
  }

  @Test
  void save_multipleTileTypes_allPreserved() {
    Map<Vector4, TileType> terrain = new LinkedHashMap<>();
    terrain.put(Vector4.of(0, 0, 0, 0), TileType.EMPTY);
    terrain.put(Vector4.of(1, 0, 0, 0), TileType.WALL);
    terrain.put(Vector4.of(2, 0, 0, 0), TileType.WATER);
    terrain.put(Vector4.of(3, 0, 0, 0), TileType.TRAP);

    repo.save("mixed", terrain);
    Map<Vector4, TileType> loaded = repo.load("mixed").orElseThrow();

    assertThat(loaded.get(Vector4.of(0, 0, 0, 0))).isEqualTo(TileType.EMPTY);
    assertThat(loaded.get(Vector4.of(1, 0, 0, 0))).isEqualTo(TileType.WALL);
    assertThat(loaded.get(Vector4.of(2, 0, 0, 0))).isEqualTo(TileType.WATER);
    assertThat(loaded.get(Vector4.of(3, 0, 0, 0))).isEqualTo(TileType.TRAP);
  }

  @Test
  void save_differentWorlds_doNotInterfere() {
    repo.save("world-a", Map.of(Vector4.of(0, 0, 0, 0), TileType.WALL));
    repo.save("world-b", Map.of(Vector4.of(0, 0, 0, 0), TileType.WATER));

    assertThat(repo.load("world-a").orElseThrow().get(Vector4.of(0, 0, 0, 0))).isEqualTo(
        TileType.WALL);
    assertThat(repo.load("world-b").orElseThrow().get(Vector4.of(0, 0, 0, 0))).isEqualTo(
        TileType.WATER);
  }

  @Test
  void save_worldNameWithSpecialChars_isSanitisedToSafeFilename() {
    // World names with slashes or spaces should not create subdirectories or fail
    repo.save("arena/1 test!", Map.of(Vector4.of(0, 0, 0, 0), TileType.EMPTY));
    assertThat(repo.exists("arena/1 test!")).isTrue();
    assertThat(repo.load("arena/1 test!")).isPresent();
  }

  @Test
  void save_largeGridTerrain_roundTripsWithoutLoss() {
    Map<Vector4, TileType> terrain = new LinkedHashMap<>();
    TileType[] types = TileType.values();
    int idx = 0;
    for (int x = 0; x < 20; x++) {
      for (int y = 0; y < 20; y++) {
        terrain.put(Vector4.of(x, y, 0, 0), types[idx++ % types.length]);
      }
    }

    repo.save("big-world", terrain);
    Map<Vector4, TileType> loaded = repo.load("big-world").orElseThrow();

    assertThat(loaded).hasSize(400);
    assertThat(loaded).containsExactlyInAnyOrderEntriesOf(terrain);
  }

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

  private static Map<Vector4, TileType> smallTerrain() {
    Map<Vector4, TileType> m = new LinkedHashMap<>();
    m.put(Vector4.of(0, 0, 0, 0), TileType.EMPTY);
    m.put(Vector4.of(1, 0, 0, 0), TileType.WALL);
    m.put(Vector4.of(0, 1, 0, 0), TileType.TRAP);
    m.put(Vector4.of(1, 1, 0, 0), TileType.WATER);
    return m;
  }
}
