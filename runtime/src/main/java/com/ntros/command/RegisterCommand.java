package com.ntros.command;

import static com.ntros.protocol.Message.error;

import com.ntros.lifecycle.session.Session;
import com.ntros.persistence.PersistenceContext;
import com.ntros.protocol.Message;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegisterCommand extends AbstractCommand {


  @Override
  public Message execute(Message message, Session session) {
    var ctx = session.getSessionContext();
    // TODO: add persistence for clients
    if (ctx.isAuthenticated()) {
      String err = "Client already registered";
      log.error("{}: {}", err, ctx);
      return error(String.format("%s: %s", err, ctx));
    }
    // validate message arg
    List<String> args = message.args();
    if (args == null || args.isEmpty()) {
      return error("Invalid message - no args list");
    }
    String username = message.args().getFirst();
    // validate username
    if (username.isBlank()) {
      return error("Blank username given");
    }

    var record = PersistenceContext.players().upsert(username);
    // TODO: once persistence added: save player
    ctx.setAuthenticated(true);
    ctx.setUsername(username);

    return Message.registrationSuccess(ctx.getSessionId(), ctx.getUsername());
  }


}
