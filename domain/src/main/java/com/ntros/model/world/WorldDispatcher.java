package com.ntros.model.world;

import com.ntros.model.world.context.GridWorldContext;
import com.ntros.model.world.context.WorldContext;
import com.ntros.model.world.engine.GridWorldEngine;
import com.ntros.model.world.state.GridWorldState;
import com.ntros.model.world.state.WorldState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class WorldDispatcher {

    private static final WorldContext DEFAULT_WORLD = new GridWorldContext(new GridWorldState("world-1", 10, 10), new GridWorldEngine());
    private static final Map<String, WorldContext> WORLDS = new HashMap<>();

    static {
        // default available world
        WORLDS.put("world-1", DEFAULT_WORLD);
    }

    private WorldDispatcher() {
    }

    public static void register(String id, WorldContext context) {
        WORLDS.put(id, context);
    }

    public static WorldContext getWorld(String id) {
        return WORLDS.get(id);
    }
    public static WorldContext getDefaultWorld() {
        return DEFAULT_WORLD;
    }

    public static Collection<WorldContext> getAllWorlds() {
        return WORLDS.values();
    }

    public static void clear() {
        WORLDS.clear();
    }

}
