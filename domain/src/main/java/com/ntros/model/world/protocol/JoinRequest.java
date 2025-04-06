package com.ntros.model.world.protocol;

public class JoinRequest {

    private String playerName;

    public JoinRequest(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }

}
