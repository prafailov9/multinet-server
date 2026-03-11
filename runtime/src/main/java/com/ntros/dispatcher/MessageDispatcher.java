package com.ntros.dispatcher;

import com.ntros.command.Command;
import com.ntros.command.utils.CommandRegistry;
import com.ntros.lifecycle.session.Session;
import com.ntros.protocol.Message;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageDispatcher implements Dispatcher {


  @Override
  public Optional<Message> dispatch(Message message, Session session) {
    log.info("Received parsed message: {}", message);
    Command command = getCommand(message.commandType().name());
    return Optional.of(command.execute(message, session));
  }


  private Command getCommand(String commandName) {
    return CommandRegistry.get(commandName);

  }

}
