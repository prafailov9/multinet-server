package com.ntros.dispatcher;

import com.ntros.command.CommandRegistry;
import com.ntros.command.impl.Command;
import com.ntros.command.impl.ErrorCommand;
import com.ntros.model.world.protocol.Message;
import com.ntros.model.world.protocol.ServerResponse;
import com.ntros.session.Session;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageDispatcher implements Dispatcher {


  @Override
  public Optional<ServerResponse> dispatch(Message message, Session session) {
    log.info("Received parsed message: {}", message);
    Command command = getCommand(message.commandType().name());

    return command.execute(message, session);
  }


  private Command getCommand(String commandName) {
    Command command = CommandRegistry.get(commandName);
    return (command == null) ? new ErrorCommand() : command;

  }

}
