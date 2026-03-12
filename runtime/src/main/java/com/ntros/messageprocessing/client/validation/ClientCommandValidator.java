package com.ntros.messageprocessing.client.validation;

import com.ntros.lifecycle.session.Session;
import com.ntros.lifecycle.session.SessionContext;
import com.ntros.messageprocessing.client.validation.exception.MessageValidationException;
import com.ntros.protocol.Message;
import java.util.List;
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

  /**
   * validates the values sent free and cell(one-step) movement are floats - whole numbers are
   * allowed too.
   * Prevents invalid float values: Infinity, Nan
   * Prevents no movement, or too large movement.
   */
  private void onMove(Message message, Session session) {
    SessionContext ctx = session.getSessionContext();
    check(() -> !ctx.isAuthenticated(),
        "[Validator.Move]: Client: %s must be authenticated",
        ctx.toString());
    check(() -> message.args().size() != 5,
        "[Validator.Move]: Invalid argument count: %s",
        message.toWireFormat());

    try {
      List<String> args = message.args();
      float dx = Float.parseFloat(args.get(0));
      float dy = Float.parseFloat(args.get(1));
      float dz = Float.parseFloat(args.get(2));
      float dw = Float.parseFloat(args.get(3));

      check(() ->
              Float.isNaN(dx) || Float.isNaN(dy) || Float.isNaN(dz) || Float.isNaN(dw) ||
                  Float.isInfinite(dx) || Float.isInfinite(dy) || Float.isInfinite(dz)
                  || Float.isInfinite(dw),
          "[Validator.Move]: Invalid float values: %s",
          message.toWireFormat());

      check(() -> Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1 || Math.abs(dw) > 1,
          "[Validator.Move]: Movement too large: %s",
          message.toWireFormat());

      check(() -> dx == 0f && dy == 0f && dz == 0f && dw == 0f,
          "[Validator.Move]: Zero movement not allowed: %s",
          message.toWireFormat());

    } catch (NumberFormatException e) {
      throw new MessageValidationException(
          "[Validator.Move]: Movement values must be floats"
      );
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
