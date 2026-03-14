package com.ntros.command;

import static com.ntros.protocol.Message.errorMsg;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles an {@code ERROR} message received from the client.
 *
 * <p>Under normal operation the server initiates ERROR messages, not clients.
 * This handler exists to close the protocol loop gracefully if a client does echo an error
 * back (e.g. a proxy or test harness).  It simply logs and returns the same error text.
 */
@Slf4j
public class ErrorCommand extends AbstractCommand {

  @Override
  public Message execute(Message message, Session session) {
    String reason = message.args().isEmpty() ? "Unknown error" : message.args().getFirst();
    log.warn("[ERROR] received error from session {}: {}",
        session.getSessionContext().getSessionId(), reason);
    return errorMsg(reason);
  }
}
