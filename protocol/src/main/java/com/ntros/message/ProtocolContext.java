package com.ntros.message;


import java.time.OffsetDateTime;

public class ProtocolContext {

    private String sessionId;
    private String worldId;
    private OffsetDateTime joinedAt;

    private boolean isAuthenticated;

    public ProtocolContext() {
    }

    public ProtocolContext(String sessionId, String worldId, OffsetDateTime joinedAt, boolean isAuthenticated) {
        this.sessionId = sessionId;
        this.worldId = worldId;
        this.joinedAt = joinedAt;
        this.isAuthenticated = isAuthenticated;
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(OffsetDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    @Override
    public String toString() {
        return "ProtocolContext{" +
                "sessionId='" + sessionId + '\'' +
                ", worldId='" + worldId + '\'' +
                ", joinedAt=" + joinedAt +
                ", isAuthenticated=" + isAuthenticated +
                '}';
    }
}
