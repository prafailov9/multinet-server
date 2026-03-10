package com.ntros.command.vo;

import com.ntros.protocol.response.ServerResponse;

public record Response(ServerResponse payload) implements CommandResult {

}
