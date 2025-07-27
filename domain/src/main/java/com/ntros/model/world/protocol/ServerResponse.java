package com.ntros.model.world.protocol;

public record ServerResponse(boolean success, String playerName, String worldName, String reason) {

}
