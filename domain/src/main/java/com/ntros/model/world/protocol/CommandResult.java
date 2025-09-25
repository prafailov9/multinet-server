package com.ntros.model.world.protocol;

public record CommandResult(boolean success, String playerName, String worldName, String reason) {

}
