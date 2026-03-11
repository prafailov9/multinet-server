package com.ntros.persistence.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.model.PlayerRecord;
import com.ntros.persistence.repository.PlayerRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SqlitePlayerRepository} using an in-memory SQLite database.
 *
 * <p>Each test gets a fresh database via {@code ":memory:"}, so tests are fully isolated and
 * leave no files on disk.
 */
class SqlitePlayerRepositoryTest extends AbstractRepositoryTest {

  private PlayerRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqlitePlayerRepository();
  }

  @AfterEach
  void tearDown() {
    repo = null;
  }

  @Test
  void upsert_newPlayer_persistsWithZeroStats() {
    PlayerRecord record = repo.upsert("alice");

    assertThat(record.name()).isEqualTo("alice");
    assertThat(record.totalMoves()).isZero();
    assertThat(record.totalSessions()).isZero();
    assertThat(record.id()).isPositive();
  }

  @Test
  void upsert_existingPlayer_doesNotResetStats() {
    repo.upsert("bob");
    repo.incrementMoves("bob", 5);
    repo.recordSessionEnd("bob");

    // upsert again — stats must survive
    repo.upsert("bob");

    PlayerRecord loaded = repo.findByName("bob").orElseThrow();
    assertThat(loaded.totalMoves()).isEqualTo(5);
    assertThat(loaded.totalSessions()).isEqualTo(1);
  }

  @Test
  void upsert_existingPlayer_updatesLastSeen() throws InterruptedException {
    repo.upsert("carol");
    PlayerRecord first = repo.findByName("carol").orElseThrow();

    Thread.sleep(10); // ensure clock advances
    repo.upsert("carol");
    PlayerRecord second = repo.findByName("carol").orElseThrow();

    // lastSeen should be updated on the second upsert
    assertThat(second.lastSeen()).isAfterOrEqualTo(first.lastSeen());
  }

  // ── findByName ────────────────────────────────────────────────────────────

  @Test
  void findByName_unknownPlayer_returnsEmpty() {
    Optional<PlayerRecord> result = repo.findByName("nobody");
    assertThat(result).isEmpty();
  }

  @Test
  void findByName_knownPlayer_returnsRecord() {
    repo.upsert("dan");
    Optional<PlayerRecord> result = repo.findByName("dan");

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("dan");
  }

  // ── incrementMoves ────────────────────────────────────────────────────────

  @Test
  void incrementMoves_addsToExistingCount() {
    repo.upsert("eve");
    repo.incrementMoves("eve", 3);
    repo.incrementMoves("eve", 7);

    int total = repo.findByName("eve").orElseThrow().totalMoves();
    assertThat(total).isEqualTo(10);
  }

  @Test
  void incrementMoves_zeroDelta_isIgnored() {
    repo.upsert("frank");
    repo.incrementMoves("frank", 0);   // should be a no-op

    assertThat(repo.findByName("frank").orElseThrow().totalMoves()).isZero();
  }

  @Test
  void incrementMoves_negativeDelta_isIgnored() {
    repo.upsert("grace");
    repo.incrementMoves("grace", -5);  // should be a no-op

    assertThat(repo.findByName("grace").orElseThrow().totalMoves()).isZero();
  }

  @Test
  void incrementMoves_unknownPlayer_logsWarningDoesNotThrow() {
    // Should log a warning but not throw
    repo.incrementMoves("ghost", 3);
    // verify no row was created
    assertThat(repo.findByName("ghost")).isEmpty();
  }

  // ── recordSessionEnd ──────────────────────────────────────────────────────

  @Test
  void recordSessionEnd_incrementsSessionCount() {
    repo.upsert("hank");
    repo.recordSessionEnd("hank");
    repo.recordSessionEnd("hank");

    PlayerRecord loaded = repo.findByName("hank").orElseThrow();
    assertThat(loaded.totalSessions()).isEqualTo(2);
  }

  @Test
  void recordSessionEnd_setsLastSeen() {
    repo.upsert("iris");
    repo.recordSessionEnd("iris");

    assertThat(repo.findByName("iris").orElseThrow().lastSeen()).isNotNull();
  }

  @Test
  void recordSessionEnd_unknownPlayer_logsWarningDoesNotThrow() {
    repo.recordSessionEnd("phantom");
    assertThat(repo.findByName("phantom")).isEmpty();
  }

  // ── combined scenario ─────────────────────────────────────────────────────

  @Test
  void fullPlayerLifecycle_accumulatesStatsAcrossMultipleSessions() {
    // Session 1: 10 moves
    repo.upsert("jack");
    repo.incrementMoves("jack", 10);
    repo.recordSessionEnd("jack");

    // Session 2: 5 more moves
    repo.upsert("jack");
    repo.incrementMoves("jack", 5);
    repo.recordSessionEnd("jack");

    PlayerRecord final_ = repo.findByName("jack").orElseThrow();
    assertThat(final_.totalMoves()).isEqualTo(15);
    assertThat(final_.totalSessions()).isEqualTo(2);
    assertThat(final_.lastSeen()).isNotNull();
  }
}
