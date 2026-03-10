package com.ntros.protocol.response;

import com.ntros.model.world.protocol.ServerResult;
import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;
import java.util.List;

public record ServerResponse(Message serverMessage,
                             ServerResult serverResult) {

  public static ServerResponse ofError(Message clientMessage,
      ServerResult serverResult) {
    return new ServerResponse(clientMessage,
        serverResult);
  }

  public static ServerResponse ofError(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ERROR, List.of(reason)),
        new ServerResult(false, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new ServerResult(true, clientMessage.args().getFirst()
            , "no_world", reason));
  }

  public static ServerResponse ofSuccess(Message serverMessage,
      ServerResult serverResult) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(serverResult.reason())),
        serverResult);
  }


  public static ServerResponse ofSuccess(String playerName, String worldName, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new ServerResult(true, playerName
            , worldName, reason));
  }
}
