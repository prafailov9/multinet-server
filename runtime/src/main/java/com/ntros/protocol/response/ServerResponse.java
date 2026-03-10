package com.ntros.protocol.response;

import com.ntros.model.world.protocol.WorldResult;
import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;

public record ServerResponse(Message serverMessage,
                             WorldResult worldResult) {

  public static ServerResponse ofError(Message clientMessage,
      WorldResult worldResult) {
    return new ServerResponse(clientMessage,
        worldResult);
  }

  public static ServerResponse ofError(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ERROR, List.of(reason)),
        new WorldResult(false, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new WorldResult(true, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message serverMessage,
      WorldResult worldResult) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(worldResult.reason())),
        worldResult);
  }


  public static ServerResponse ofSuccess(String playerName, String worldName, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new WorldResult(true, playerName
            , worldName, reason));
  }
}
