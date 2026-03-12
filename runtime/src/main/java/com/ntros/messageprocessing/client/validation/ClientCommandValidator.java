package com.ntros.messageprocessing.client.validation;

import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.messageprocessing.client.validation.exception.MessageValidationException;
import com.ntros.protocol.Message;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientCommandValidator implements CommandValidator {

  @Override
  public void validate(Message message, Session session) {
    switch (message.commandType()) {
      case REG -> onRegister(message, session);
      case AUTHENTICATE -> onAuthenticate(message, session);
      case JOIN -> onJoin(message, session);
      case DISCONNECT -> onDisconnect(message, session);
      case MOVE -> onMove(message, session);
      case CREATE -> onCreate(message, session);
      case ORCHESTRATE -> onOrchestrate(message, session);
      default -> log.info("Received message: {}. Passing it downstream", message);
    }
  }


  private void onRegister(Message message, Session session) {
    // session must not be authenticated, arg list must not have more than 3 params
    SessionContext ctx = session.getSessionContext();
    check(ctx::isAuthenticated, "[Validator.Register]: Client: %s already authenticated",
        ctx.toString());

    check(() -> message.args().size() > 3,
        "[Validator.Register]: Too many arguments for REG command: %s",
        message.toWireFormat()
    );
  }

  private void onAuthenticate(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(ctx::isAuthenticated, "[Validator.Auth]: Client: %s already authenticated",
        ctx.toString());
    check(() -> message.args().size() > 3,
        "[Validator.Auth]: Too many arguments for AUTH command: %s",
        message.toWireFormat()
    );

  }

  private void onJoin(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(), "[Validator.Join]: Client: %s must be authenticated",
        ctx.toString());
  }

  private void onDisconnect(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(), "[Validator.Disconnect]: Client: %s must be authenticated",
        ctx.toString());

  }

  private void onMove(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(),
        "[Validator.Move]: Client: %s must be authenticated", ctx.toString());

    // MOVE dx dy dz dw player
    check(() -> message.args().size() != 5,
        "[Validator.Move]: Invalid argument count: %s", message.toWireFormat());

    try {
      int dx = Integer.parseInt(message.args().get(0));
      int dy = Integer.parseInt(message.args().get(1));
      int dz = Integer.parseInt(message.args().get(2));
      int dw = Integer.parseInt(message.args().get(3));

      // prevent teleport cheats
      check(() -> Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1 || Math.abs(dw) > 1,
          "[Validator.Move]: Movement too large: %s",
          message.toWireFormat());

      // prevent zero movement
      check(() -> dx == 0 && dy == 0 && dz == 0 && dw == 0,
          "[Validator.Move]: Zero movement not allowed: %s",
          message.toWireFormat());

    } catch (NumberFormatException e) {
      throw new MessageValidationException("[Validator.Move]: Movement values must be integers");
    }
  }

  private void onCreate(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(), "[Validator.Create]: Client: %s must be authenticated",
        ctx.toString());
  }

  private void onOrchestrate(Message message, Session session) {
    // must be auth, at least 2 arguments in message
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(), "[Validator.Orchestrate]: Client: %s must be authenticated",
        ctx.toString());

    check(() -> message.args().size() < 2,
        "[Validator.Orchestrate]: Client: %s must be authenticated",
        ctx.toString());
  }

  private void check(Supplier<Boolean> consumer, String err, String errArg) {
    if (consumer.get()) {
      log.error(err);
      throw new MessageValidationException(String.format(err, errArg));
    }
  }

}
