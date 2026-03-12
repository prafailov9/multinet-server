package com.ntros.messageprocessing.client.validation;

import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;

/**
 * Validate a parsed Client Message
 */
public interface CommandValidator {

  void validate(Message message, Session session);

}
