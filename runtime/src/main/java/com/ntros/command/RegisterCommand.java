package com.ntros.command;

import static com.ntros.protocol.Message.errorMsg;
import static com.ntros.protocol.Message.registrationSuccess;

import com.ntros.command.exception.RegisterCmdException;
import com.ntros.lifecycle.session.Session;
import com.ntros.message.SessionContext;
import com.ntros.persistence.PersistenceContext;
import com.ntros.persistence.model.ClientRecord;
import com.ntros.protocol.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterCommand extends AbstractCommand {


  @Override
  public Message execute(Message message, Session session) {
    try {
      var ctx = session.getSessionContext();
      checkAuth(ctx);

      // build record
      ClientRecord record;
      record = buildClient(message, ctx);

      // try to persist + update ctx
      var saved = saveClient(record);
      ctx.setAuthenticated(true);
      ctx.setUsername(saved.username());

      return registrationSuccess(ctx.getSessionId(), ctx.getUsername());
    } catch (RegisterCmdException ex) {
      log.error("Error occurred during Registration. {}", ex.getMessage(), ex);
      return errorMsg(ex.getMessage());
    }
  }

  private ClientRecord saveClient(ClientRecord clientRecord) {
    var saved = PersistenceContext.clients().insert(clientRecord);
    if (saved.isEmpty()) {
      throw new RegisterCmdException(
          String.format("Could not persist client record: %s", clientRecord));
    }
    return saved.get();
  }

  private void checkAuth(SessionContext sessionContext) {
    if (sessionContext.isAuthenticated()) {
      String err = "Client already registered";
      log.error("{}: {}", err, sessionContext);
      throw new RegisterCmdException(String.format("%s: %s", err, sessionContext));
    }
  }

  private ClientRecord buildClient(Message message, SessionContext ctx) {
    // validate message arg
    List<String> args = message.args();
    if (args == null || args.isEmpty()) {
      throw new RegisterCmdException("Invalid message - no args list");
    }

    String username = args.getFirst();
    // validate username
    if (username.isBlank()) {
      throw new RegisterCmdException("Blank username given");
    }
    String password = args.get(1);
    return ClientRecord.newClient(username, password, ctx.getSessionId());

  }


}
