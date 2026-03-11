package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.db.TestDbHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base class for SQLite repository integration tests.
 *
 * <p>Opens a fresh in-memory database before each test and closes it afterwards, giving every
 * test method a clean, fully-isolated environment without leaving any files on disk.
 *
 * <h3>Usage</h3>
 * <pre>
 * class SqlitePlayerRepositoryTest extends AbstractRepositoryTest {
 *
 *   private PlayerRepository repo;
 *
 *   &#64;BeforeEach
 *   void setUp() {
 *     repo = new SqlitePlayerRepository();   // DB already initialised by abstract class
 *   }
 *
 *   // tests …
 * }
 * </pre>
 *
 * <p>JUnit 5 calls the parent {@code @BeforeEach} before the child's, so the database is always
 * ready when {@code setUp()} runs. Symmetrically, the parent {@code @AfterEach} runs after the
 * child's, so any repository references can be nulled out before the connection is closed.
 */
abstract class AbstractRepositoryTest {

  @BeforeEach
  void createDb() {
    TestDbHelper.createDb();
  }

  /**
   * Closes the in-memory database.
   */
  @AfterEach
  void dropDb() {
    TestDbHelper.dropDatabase();
    ConnectionProvider.close();
  }
}
