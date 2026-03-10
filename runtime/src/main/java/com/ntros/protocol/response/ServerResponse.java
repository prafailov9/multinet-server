package com.ntros.protocol.response;

import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;

public record ServerResponse(Message serverMessage,
                             com.ntros.model.world.protocol.CommandResult commandResult) {

  public static ServerResponse ofError(Message clientMessage,
      com.ntros.model.world.protocol.CommandResult commandResult) {
    return new ServerResponse(clientMessage,
        commandResult);
  }

  public static ServerResponse ofError(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ERROR, List.of(reason)),
        new com.ntros.model.world.protocol.CommandResult(false, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new com.ntros.model.world.protocol.CommandResult(true, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message serverMessage,
      com.ntros.model.world.protocol.CommandResult commandResult) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(commandResult.reason())),
        commandResult);
  }


  public static ServerResponse ofSuccess(String playerName, String worldName, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new com.ntros.model.world.protocol.CommandResult(true, playerName
            , worldName, reason));
  }
}
