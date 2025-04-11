package com.ntros.model.entity.movement;

import java.util.Objects;

public class Vector2D implements Vector {
    public static final Vector2D ZERO = new Vector2D(0, 0);
    private final float x;
    private final float y;

    private Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float getX() {
        return x;
    }
    @Override
    public float getY() {
        return y;
    }
    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    public Vector2D scale(float factor) {
        return new Vector2D(this.x * factor, this.y * factor);
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public Vector2D normalize() {
        float mag = magnitude();
        if (mag == 0) return ZERO;
        return new Vector2D(x / mag, y / mag);
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Vector2D vector2D)) return false;
        return Float.compare(x, vector2D.x) == 0 && Float.compare(y, vector2D.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Vector2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
