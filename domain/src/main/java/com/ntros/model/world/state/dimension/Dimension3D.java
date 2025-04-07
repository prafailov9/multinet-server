package com.ntros.model.world.state.dimension;

public class Dimension3D extends BaseDimension {

    private final int depth;

    public Dimension3D(int width, int height, int depth) {
        super(width, height);
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

}
