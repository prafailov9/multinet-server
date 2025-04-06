package com.ntros.model.entity;

public interface Solid {
    default boolean isSolid() {
        return true;
    }
}
