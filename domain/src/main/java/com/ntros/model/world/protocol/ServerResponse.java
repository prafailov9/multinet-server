package com.ntros.model.world.protocol;

import static com.ntros.model.world.protocol.WorldType.UNKNOWN;

import java.util.List;

public record ServerResponse(Message serverMessage, CommandResult commandResult) {

  public static ServerResponse ofError(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ERROR, List.of(reason)),
        new CommandResult(false, clientMessage.args().getFirst()
            , "no_world", reason, UNKNOWN));
  }

  public static ServerResponse ofSuccess(Message clientMessage, String reason) {
    return new ServerResponse(new Message(CommandType.ACK, List.of(reason)),
        new CommandResult(true, clientMessage.args().getFirst()
            , "no_world", reason, UNKNOWN));
  }


}
