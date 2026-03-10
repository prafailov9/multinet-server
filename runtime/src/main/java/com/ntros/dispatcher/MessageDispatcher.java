package com.ntros.dispatcher;

import com.ntros.command.utils.CommandRegistry;
import com.ntros.command.Command;
import com.ntros.protocol.Message;
import com.ntros.protocol.response.ServerResponse;
import com.ntros.lifecycle.session.Session;
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
    return CommandRegistry.get(commandName);

  }

}
