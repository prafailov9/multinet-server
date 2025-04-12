package com.ntros.message;


import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtocolContext {

    private Long sessionId;
    private String playerId;
    private String worldId;
    private OffsetDateTime joinedAt;

    private final AtomicBoolean isAuthenticated = new AtomicBoolean(false);

    public ProtocolContext() {
    }

    public ProtocolContext(Long sessionId, String playerId, String worldId, OffsetDateTime joinedAt) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.worldId = worldId;
        this.joinedAt = joinedAt;
    }


    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAuthenticated() {
        return isAuthenticated.get();
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated.set(authenticated);
    }

    @Override
    public String toString() {
        return "ProtocolContext{" +
                "sessionId='" + sessionId + '\'' +
                ", worldId='" + worldId + '\'' +
                ", joinedAt=" + joinedAt +
                ", isAuthenticated=" + isAuthenticated.get() +
                '}';
    }
}
