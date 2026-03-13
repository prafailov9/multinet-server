package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.d2.grid.CellType;
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
    Optional<Map<Vector4, CellType>> result = repo.load("nonexistent");
    assertThat(result).isEmpty();
  }

  @Test
  void load_afterSave_returnsSameTerrain() {
    Map<Vector4, CellType> terrain = smallTerrain();
    repo.save("arena-x", terrain);

    Map<Vector4, CellType> loaded = repo.load("arena-x").orElseThrow();
    assertThat(loaded).containsExactlyInAnyOrderEntriesOf(terrain);
  }

  @Test
  void save_overwritesPreviousSnapshot() {
    Map<Vector4, CellType> v1 = Map.of(Vector4.of(0, 0, 0, 0), CellType.EMPTY);
    Map<Vector4, CellType> v2 = Map.of(Vector4.of(0, 0, 0, 0), CellType.WALL);

    repo.save("world", v1);
    repo.save("world", v2);

    Map<Vector4, CellType> loaded = repo.load("world").orElseThrow();
    assertThat(loaded.get(Vector4.of(0, 0, 0, 0))).isEqualTo(CellType.WALL);
  }

  @Test
  void save_multipleTileTypes_allPreserved() {
    Map<Vector4, CellType> terrain = new LinkedHashMap<>();
    terrain.put(Vector4.of(0, 0, 0, 0), CellType.EMPTY);
    terrain.put(Vector4.of(1, 0, 0, 0), CellType.WALL);
    terrain.put(Vector4.of(2, 0, 0, 0), CellType.WATER);
    terrain.put(Vector4.of(3, 0, 0, 0), CellType.TRAP);

    repo.save("mixed", terrain);
    Map<Vector4, CellType> loaded = repo.load("mixed").orElseThrow();

    assertThat(loaded.get(Vector4.of(0, 0, 0, 0))).isEqualTo(CellType.EMPTY);
    assertThat(loaded.get(Vector4.of(1, 0, 0, 0))).isEqualTo(CellType.WALL);
    assertThat(loaded.get(Vector4.of(2, 0, 0, 0))).isEqualTo(CellType.WATER);
    assertThat(loaded.get(Vector4.of(3, 0, 0, 0))).isEqualTo(CellType.TRAP);
  }

  @Test
  void save_differentWorlds_doNotInterfere() {
    repo.save("world-a", Map.of(Vector4.of(0, 0, 0, 0), CellType.WALL));
    repo.save("world-b", Map.of(Vector4.of(0, 0, 0, 0), CellType.WATER));

    assertThat(repo.load("world-a").orElseThrow().get(Vector4.of(0, 0, 0, 0))).isEqualTo(
        CellType.WALL);
    assertThat(repo.load("world-b").orElseThrow().get(Vector4.of(0, 0, 0, 0))).isEqualTo(
        CellType.WATER);
  }

  @Test
  void save_worldNameWithSpecialChars_isSanitisedToSafeFilename() {
    // World names with slashes or spaces should not create subdirectories or fail
    repo.save("arena/1 test!", Map.of(Vector4.of(0, 0, 0, 0), CellType.EMPTY));
    assertThat(repo.exists("arena/1 test!")).isTrue();
    assertThat(repo.load("arena/1 test!")).isPresent();
  }

  @Test
  void save_largeGridTerrain_roundTripsWithoutLoss() {
    Map<Vector4, CellType> terrain = new LinkedHashMap<>();
    CellType[] types = CellType.values();
    int idx = 0;
    for (int x = 0; x < 20; x++) {
      for (int y = 0; y < 20; y++) {
        terrain.put(Vector4.of(x, y, 0, 0), types[idx++ % types.length]);
      }
    }

    repo.save("big-world", terrain);
    Map<Vector4, CellType> loaded = repo.load("big-world").orElseThrow();

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

  private static Map<Vector4, CellType> smallTerrain() {
    Map<Vector4, CellType> m = new LinkedHashMap<>();
    m.put(Vector4.of(0, 0, 0, 0), CellType.EMPTY);
    m.put(Vector4.of(1, 0, 0, 0), CellType.WALL);
    m.put(Vector4.of(0, 1, 0, 0), CellType.TRAP);
    m.put(Vector4.of(1, 1, 0, 0), CellType.WATER);
    return m;
  }
}
