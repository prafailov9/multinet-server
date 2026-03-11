package com.ntros.persistence.repository.impl;

import com.ntros.persistence.db.DatabaseBuilder;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


abstract class AbstractRepositoryTest {

  @BeforeEach
  void createDb() throws IOException {
    DatabaseBuilder.createDatabase(":memory:", Files.createTempDirectory("test-terrain-"));
  }

  @AfterEach
  void dropDb() {
    DatabaseBuilder.dropDatabase();

  }
}
