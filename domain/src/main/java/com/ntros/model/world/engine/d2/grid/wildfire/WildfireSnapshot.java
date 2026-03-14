package com.ntros.model.world.engine.d2.grid.wildfire;

public record WildfireSnapshot(
    WildfireCell[] cells,   // flat, row-major
    int width,
    int height,
    float windDx,
    float windDy,
    float windSpeed
) {

}
