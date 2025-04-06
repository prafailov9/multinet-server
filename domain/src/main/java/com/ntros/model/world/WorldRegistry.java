package com.ntros.model.world;

import java.util.Map;

public final class WorldRegistry {

    private static final String DEFAULT_WORLD_NAME = "world-1";
    private static final GridWorld DEFAULT_WORLD = new GridWorld(DEFAULT_WORLD_NAME, 10, 10);
    private static final Map<String, GridWorld> WORLD_MAP;

    static {
//        GRID_WORLD_IDS = List.of(IdSequenceGenerator.getInstance().getNextWorldId());
        WORLD_MAP = Map.of(DEFAULT_WORLD_NAME, DEFAULT_WORLD);
    }

    public static GridWorld getGridWorld(String worldName) {
        return (worldName == null || worldName.isEmpty()) ? DEFAULT_WORLD : WORLD_MAP.get(worldName);
    }

    public static GridWorld getRandomGridWorld() {
        return DEFAULT_WORLD;
    }

}
