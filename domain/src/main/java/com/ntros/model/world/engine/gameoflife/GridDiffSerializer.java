package com.ntros.model.world.engine.gameoflife;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ntros.model.entity.movement.vectors.Vector4;
import java.io.IOException;

/**
 * Serializes a {@link GridDiff} as:
 * <pre>
 * {
 *   "type":     "diff",
 *   "born":     [[x,y], …],
 *   "died":     [[x,y], …],
 *   "entities": {"name": {"x": N, "y": N}, …}
 * }
 * </pre>
 *
 * <p>Cell positions are emitted as compact 2-element integer arrays instead of objects to
 * minimise payload size — a diff can contain tens of thousands of entries per tick.
 */
public class GridDiffSerializer extends JsonSerializer<GridDiff> {

  @Override
  public void serialize(GridDiff diff, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "diff");

    // ── born cells: [[x,y], …] ─────────────────────────────────────────────
    gen.writeFieldName("born");
    gen.writeStartArray();
    for (Vector4 pos : diff.getBornCells()) {
      gen.writeStartArray();
      gen.writeNumber((int) pos.getX());
      gen.writeNumber((int) pos.getY());
      gen.writeEndArray();
    }
    gen.writeEndArray();

    // ── died cells: [[x,y], …] ─────────────────────────────────────────────
    gen.writeFieldName("died");
    gen.writeStartArray();
    for (Vector4 pos : diff.getDiedCells()) {
      gen.writeStartArray();
      gen.writeNumber((int) pos.getX());
      gen.writeNumber((int) pos.getY());
      gen.writeEndArray();
    }
    gen.writeEndArray();

    // ── entities ───────────────────────────────────────────────────────────
    gen.writeFieldName("entities");
    gen.writeStartObject();
    for (var entry : diff.getEntities().entrySet()) {
      gen.writeFieldName(entry.getKey());
      gen.writeStartObject();
      gen.writeNumberField("x", entry.getValue().x());
      gen.writeNumberField("y", entry.getValue().y());
      gen.writeEndObject();
    }
    gen.writeEndObject();

    gen.writeEndObject();
  }
}
