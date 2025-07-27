package com.ntros.model.world.protocol;

import com.ntros.model.world.WorldType;

public record ServerResponse(boolean success, String playerName, String worldName, String reason, WorldType worldType) {

}
