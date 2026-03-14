package com.ntros.persistence.repository.impl;

import static com.ntros.model.entity.config.access.SystemRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.ntros.model.entity.config.access.SystemRole;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.persistence.model.SystemRoleRecord;
import com.ntros.persistence.repository.ClientRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteClientRepositoryTest extends AbstractRepositoryTest {

  private static final ClientRecord DEFAULT_CLIENT = new ClientRecord(1L, 1,
      new SystemRoleRecord(1L,
          USER.name()), List.of(), "bob", "bob123"
      , Instant.now(), Instant.now());

  private ClientRepository repo;

  @BeforeEach
  void setUp() {
    repo = new SqliteClientRepository();
  }

  @AfterEach
  void tearDown() {
    repo = null;
  }


  @Test
  public void insert_newClient_success() {
    var saved = repo.insert(DEFAULT_CLIENT).orElse(null);
    asserts(DEFAULT_CLIENT, saved);
  }

  @Test
  public void insert_multipleNewClients_success() {
    int n = 5;
    List<ClientRecord> clients = new ArrayList<>();
    // act
    for (int i = 0; i < n; i++) {
      clients.add(repo.insert(ClientRecord.newClient("cl-" + i, "pass" + i, i + 1)).orElse(null));
    }

    // assert
    for (int i = 0; i < n; i++) {
      ClientRecord expected = new ClientRecord(i + 1, i + 1, new SystemRoleRecord(1L,
          USER.name()), List.of(), "cl-" + i, "pass" + i,
          Instant.now(),
          Instant.now());
      asserts(expected, clients.get(i));
    }
  }

  @Test
  public void insert_existingClient_returnSuccess() {
    var saved = repo.insert(DEFAULT_CLIENT).orElse(null);
    asserts(DEFAULT_CLIENT, saved);

    saved = repo.insert(DEFAULT_CLIENT).orElse(null);
    asserts(DEFAULT_CLIENT, saved);

  }

  @Test
  public void findById_exist_success() {
    var saved = repo.insert(DEFAULT_CLIENT).orElse(null);
    assertNotNull(saved);

    var exist = repo.findById(saved.clientId()).orElse(null);
    asserts(DEFAULT_CLIENT, exist);
  }

  @Test
  public void findById_notExist_returnsEmpty() {
    // findById() returns Optional.empty() when the record does not exist
    assertThat(repo.findById(DEFAULT_CLIENT.clientId())).isEmpty();
  }


  private void asserts(ClientRecord expected, ClientRecord actual) {
    assertNotNull(actual);
    assertEquals(expected.clientId(), actual.clientId());
    assertEquals(expected.username(), actual.username());
    assertEquals(expected.password(), actual.password());
    assertNotNull(actual.createdAt());
    assertNotNull(actual.updatedAt());
  }

}