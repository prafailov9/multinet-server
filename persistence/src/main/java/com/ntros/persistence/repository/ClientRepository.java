package com.ntros.persistence.repository;

import com.ntros.persistence.model.ClientRecord;
import java.util.List;
import java.util.Optional;

public interface ClientRepository {

  Optional<ClientRecord> findByUsername(String username);

  Optional<ClientRecord> findById(long clientId);

  Optional<ClientRecord> insert(ClientRecord client);

  Optional<ClientRecord> remove(ClientRecord client);

  Optional<ClientRecord> removeById(long clientId);

  List<ClientRecord> findAll();

  ClientRecord registerIfAbsent(ClientRecord record);

}
