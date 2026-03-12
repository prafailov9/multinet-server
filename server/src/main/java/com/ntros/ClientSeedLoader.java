package com.ntros;

import com.ntros.persistence.db.PersistenceContext;
import com.ntros.persistence.model.ClientRecord;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ClientSeedLoader {

  // no pass allowed for now
  // sessionId means nothing yet but make sure it doesnt conflict with other existing sessions or the IdSequenceGenerator
  static final List<ClientRecord> DEFAULT_CLIENTS = List.of(
      clients("admin", "123", Integer.MAX_VALUE),
      clients("admin-x", "222", Integer.MAX_VALUE - 1),
      clients("root", "", Integer.MAX_VALUE - 2)
  );

  private ClientSeedLoader() {
  }

  public static void seedIfEmpty() {
    List<ClientRecord> existing = PersistenceContext.clients().findAll();
    if (!existing.isEmpty()) {
      log.info("[ClientSeedLoader] {} client(s) already in DB — skipping seed.", existing.size());
      return;
    }
    for (ClientRecord record : DEFAULT_CLIENTS) {
      PersistenceContext.clients().registerIfAbsent(record);
    }
    log.info("[ClientSeedLoader] Seeded {} default client(s) into DB.", DEFAULT_CLIENTS.size());
  }

  private static ClientRecord clients(String username, String password, long sessionId) {
    return ClientRecord.newClient(username, password, sessionId);
  }

}
