package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.ConnectionProvider;
import com.ntros.persistence.db.DatabaseBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


abstract class AbstractRepositoryTest {

  @BeforeEach
  void createDb() {
    DatabaseBuilder.createDb();
  }

  @AfterEach
  void dropDb() {
    DatabaseBuilder.dropDatabase();
    ConnectionProvider.close();
  }
}
