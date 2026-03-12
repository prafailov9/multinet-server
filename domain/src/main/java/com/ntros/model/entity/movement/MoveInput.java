package com.ntros.model.entity.movement;


import com.ntros.model.entity.movement.vectors.Vector4;

/**
 * Handles up to 4d worlds
 */
public record MoveInput(float dx, float dy, float dz, float dw) {

  public Vector4 toVector4() {
    return Vector4.of(dx, dy, dz, dw);
  }
}
