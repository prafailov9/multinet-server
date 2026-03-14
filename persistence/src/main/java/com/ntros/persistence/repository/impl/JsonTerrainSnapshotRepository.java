package com.ntros.persistence.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.state.grid.CellType;
import com.ntros.persistence.repository.TerrainSnapshotRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Saves and loads grid-world terrain as a pretty-printed JSON file under the configured snapshot
 * directory.
 *
 * <h3>File format</h3>
 * <pre>
 * {
 *   "worldName": "arena-x",
 *   "terrain": {
 *     "0,0": "EMPTY",
 *     "0,1": "WALL",
 *     ...
 *   }
 * }
 * </pre>
 *
 * <p>The key {@code "x,y"} matches the same format used by the grid snapshot broadcaster so
 * clients can cross-reference offline data with live state messages.
 */
@Slf4j
public class JsonTerrainSnapshotRepository implements TerrainSnapshotRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private final Path snapshotDir;

  /**
   * @param snapshotDir directory where terrain JSON files are stored
   *                    (created automatically if it does not exist)
   */
  public JsonTerrainSnapshotRepository(Path snapshotDir) {
    this.snapshotDir = snapshotDir;
    try {
      Files.createDirectories(snapshotDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create snapshot directory: " + snapshotDir, e);
    }
  }

  // ── save ──────────────────────────────────────────────────────────────────

  @Override
  public void save(String worldName, Map<Vector4, CellType> terrain) {
    Path file = fileFor(worldName);
    try {
      // Convert Position keys to "x,y" strings for JSON serialisation
      Map<String, String> serialized = new LinkedHashMap<>(terrain.size());
      terrain.forEach((pos, tile) ->
          serialized.put(pos.getX() + "," + pos.getY(), tile.name()));

      SnapshotFile payload = new SnapshotFile(worldName, serialized);
      MAPPER.writeValue(file.toFile(), payload);
      log.info("[TerrainSnapshotRepository] Saved terrain for '{}' → {} tiles → {}.",
          worldName, terrain.size(), file);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save terrain snapshot for: " + worldName, e);
    }
  }

  // ── load ──────────────────────────────────────────────────────────────────

  @Override
  public Optional<Map<Vector4, CellType>> load(String worldName) {
    Path file = fileFor(worldName);
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    try {
      SnapshotFile payload = MAPPER.readValue(file.toFile(), SnapshotFile.class);
      Map<Vector4, CellType> terrain = new LinkedHashMap<>(payload.terrain().size());
      payload.terrain().forEach((key, tileName) -> {
        String[] parts = key.split(",");
        Vector4 pos = Vector4.of(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), 0f, 0f);
        CellType tile = CellType.valueOf(tileName);
        terrain.put(pos, tile);
      });
      log.info("[TerrainSnapshotRepository] Loaded terrain for '{}' ← {} tiles.", worldName,
          terrain.size());
      return Optional.of(terrain);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load terrain snapshot for: " + worldName, e);
    }
  }

  // ── exists ────────────────────────────────────────────────────────────────

  @Override
  public boolean exists(String worldName) {
    return Files.exists(fileFor(worldName));
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Override
  public void delete(String worldName) {
    try {
      boolean deleted = Files.deleteIfExists(fileFor(worldName));
      if (deleted) {
        log.info("[TerrainSnapshotRepository] Deleted snapshot for '{}'.", worldName);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete terrain snapshot for: " + worldName, e);
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private Path fileFor(String worldName) {
    // Sanitise world name so it is safe to use as a filename
    String safe = worldName.replaceAll("[^a-zA-Z0-9._-]", "_");
    return snapshotDir.resolve(safe + ".terrain.json");
  }

  // ── Wire format POJO ──────────────────────────────────────────────────────

  /**
   * Jackson-serialisable wrapper so the JSON file is self-describing.
   *
   * @param worldName name of the world this terrain belongs to
   * @param terrain   {@code "x,y" → "TILETYPE"} map
   */
  record SnapshotFile(String worldName, Map<String, String> terrain) {

    // No-arg constructor required by Jackson for deserialization
    @SuppressWarnings("unused")
    SnapshotFile() {
      this(null, null);
    }
  }
}
