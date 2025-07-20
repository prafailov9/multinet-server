package com.ntros.model.world;

import com.ntros.model.world.connector.GridWorldConnector;
import com.ntros.model.world.connector.WorldConnector;
import com.ntros.model.world.engine.solid.GridWorldEngine;
import com.ntros.model.world.state.solid.GridWorldState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class WorldDispatcher {

    private static final WorldConnector DEFAULT_WORLD = new GridWorldConnector(new GridWorldState("world-1", 10, 10), new GridWorldEngine());
    private static final Map<String, WorldConnector> WORLDS = new HashMap<>();

    static {
        // default available world
        WORLDS.put("world-1", DEFAULT_WORLD);
    }

    private WorldDispatcher() {
    }

    public static void register(String id, WorldConnector context) {
        WORLDS.put(id, context);
    }
    public static void remove(String id) {
        WORLDS.remove(id);
    }

    public static WorldConnector getWorld(String id) {
        return WORLDS.get(id);
    }
    public static WorldConnector getDefaultWorld() {
        return DEFAULT_WORLD;
    }

    public static Collection<WorldConnector> getAllWorlds() {
        return WORLDS.values();
    }

    public static void clear() {
        WORLDS.clear();
    }

}
