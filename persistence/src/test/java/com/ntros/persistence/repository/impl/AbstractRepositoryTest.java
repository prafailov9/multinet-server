package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.db.TestDbHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


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
