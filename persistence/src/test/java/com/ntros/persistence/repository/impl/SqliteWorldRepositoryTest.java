package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.persistence.model.WorldRecord;
import com.ntros.persistence.repository.WorldRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqliteWorldRepository} using an in-memory SQLite database.
 */
class SqliteWorldRepositoryTest extends AbstractRepositoryTest {

  private WorldRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqliteWorldRepository();
  }

  @AfterEach
  void tearDown() {
    repo = null;
  }

  // ── registerIfAbsent ──────────────────────────────────────────────────────

  @Test
  void registerIfAbsent_newWorld_isStoredAndReturned() {
    WorldRecord record = world("arena-x", "GRID", 5, 5, 0,
        true, false, false, true);
    WorldRecord stored = repo.registerIfAbsent(record);

    assertThat(stored.name()).isEqualTo("arena-x");
    assertThat(stored.engineType()).isEqualTo("GRID");
    assertThat(stored.width()).isEqualTo(5);
    assertThat(stored.height()).isEqualTo(5);
    assertThat(stored.depth()).isZero();
    assertThat(stored.multiplayer()).isTrue();
    assertThat(stored.deterministic()).isTrue();
  }

  @Test
  void registerIfAbsent_duplicateName_isIdempotentReturnsExisting() {
    WorldRecord first  = world("arena-x", "GRID", 5,   5,   0,   true,  false, false, true);
    WorldRecord second = world("arena-x", "GOL",  100, 50,  100, false, true,  true,  false);

    repo.registerIfAbsent(first);
    WorldRecord returned = repo.registerIfAbsent(second);

    // Must return the FIRST record — engine_type must not be overwritten
    assertThat(returned.engineType()).isEqualTo("GRID");
    assertThat(repo.findAll()).hasSize(1);
  }

  // ── findByName ────────────────────────────────────────────────────────────

  @Test
  void findByName_unknownWorld_returnsEmpty() {
    assertThat(repo.findByName("missing")).isEmpty();
  }

  @Test
  void findByName_knownWorld_returnsRecord() {
    repo.registerIfAbsent(world("open-1", "GRID", 200, 50, 0,
        false, true, false, true));

    var found = repo.findByName("open-1");
    assertThat(found).isPresent();
    assertThat(found.get().width()).isEqualTo(200);
    assertThat(found.get().orchestrated()).isTrue();
  }

  // ── findAll ───────────────────────────────────────────────────────────────

  @Test
  void findAll_emptyDatabase_returnsEmptyList() {
    assertThat(repo.findAll()).isEmpty();
  }

  @Test
  void findAll_multipleWorlds_returnsAllOrderedByCreatedAt() throws InterruptedException {
    repo.registerIfAbsent(world("world-a", "GRID", 10, 10, 0,
        true, false, false, true));
    Thread.sleep(5);
    repo.registerIfAbsent(world("world-b", "GOL",  50, 20, 0,
        false, true, false, true));

    List<WorldRecord> all = repo.findAll();

    assertThat(all).hasSize(2);
    assertThat(all.get(0).name()).isEqualTo("world-a");
    assertThat(all.get(1).name()).isEqualTo("world-b");
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private static WorldRecord world(String name, String engineType,
      int width, int height, int depth,
      boolean multiplayer, boolean orchestrated,
      boolean hasAi, boolean deterministic) {
    return new WorldRecord(name, engineType, width, height, depth,
        multiplayer, orchestrated, hasAi, deterministic, Instant.now());
  }
}
