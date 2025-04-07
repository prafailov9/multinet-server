package com.ntros.model.world.state.dimension;

public abstract class BaseDimension implements Dimension {

    private final int width;
    private final int height;

    public BaseDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
