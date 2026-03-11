package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.ntros.persistence.db.ConnectionProvider;
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
class SqliteWorldRepositoryTest {

  private WorldRepository repo;

  @BeforeEach
  void setUp() {
    ConnectionProvider.initialize(":memory:");
    repo = new SqliteWorldRepository();
  }

  @AfterEach
  void tearDown() {
    ConnectionProvider.close();
  }

  // ── registerIfAbsent ──────────────────────────────────────────────────────

  @Test
  void registerIfAbsent_newWorld_isStoredAndReturned() {
    WorldRecord record = new WorldRecord("arena-x", "GRID", 5, 5, 0, Instant.now());
    WorldRecord stored = repo.registerIfAbsent(record);

    assertThat(stored.name()).isEqualTo("arena-x");
    assertThat(stored.type()).isEqualTo("GRID");
    assertThat(stored.width()).isEqualTo(5);
    assertThat(stored.height()).isEqualTo(5);
    assertThat(stored.depth()).isZero();
  }

  @Test
  void registerIfAbsent_duplicateName_isIdempotentReturnsExisting() {
    WorldRecord first = new WorldRecord("arena-x", "GRID", 5, 5, 0, Instant.now());
    WorldRecord second = new WorldRecord("arena-x", "OPEN", 100, 50, 100, Instant.now());

    repo.registerIfAbsent(first);
    WorldRecord returned = repo.registerIfAbsent(second);

    // Must return the FIRST record — type must not be overwritten
    assertThat(returned.type()).isEqualTo("GRID");
    assertThat(repo.findAll()).hasSize(1);
  }

  // ── findByName ────────────────────────────────────────────────────────────

  @Test
  void findByName_unknownWorld_returnsEmpty() {
    assertThat(repo.findByName("missing")).isEmpty();
  }

  @Test
  void findByName_knownWorld_returnsRecord() {
    repo.registerIfAbsent(new WorldRecord("open-1", "OPEN", 200, 50, 200, Instant.now()));

    var found = repo.findByName("open-1");
    assertThat(found).isPresent();
    assertThat(found.get().depth()).isEqualTo(200);
  }

  // ── findAll ───────────────────────────────────────────────────────────────

  @Test
  void findAll_emptyDatabase_returnsEmptyList() {
    assertThat(repo.findAll()).isEmpty();
  }

  @Test
  void findAll_multipleWorlds_returnsAllOrderedByCreatedAt() throws InterruptedException {
    repo.registerIfAbsent(new WorldRecord("world-a", "GRID", 10, 10, 0, Instant.now()));
    Thread.sleep(5);
    repo.registerIfAbsent(new WorldRecord("world-b", "OPEN", 50, 20, 50, Instant.now()));

    List<WorldRecord> all = repo.findAll();

    assertThat(all).hasSize(2);
    assertThat(all.get(0).name()).isEqualTo("world-a");
    assertThat(all.get(1).name()).isEqualTo("world-b");
  }
}
