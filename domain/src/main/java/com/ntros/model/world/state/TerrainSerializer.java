package com.ntros.model.world.state;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.ntros.model.entity.movement.vectors.Vector4;
import com.ntros.model.world.protocol.TileType;
import java.io.IOException;

/**
 * Serializes a {@link GridSnapshot} as:
 * <pre>{"type":"full","tiles":{"x,y":"TILETYPE",...},"entities":{"name":{"x":1,"y":2},...}}</pre>
 *
 * <p>The {@code "type":"full"} field lets the client distinguish a complete snapshot from an
 * incremental {@link com.ntros.model.world.engine.gameoflife.GridDiff} frame.
 * EMPTY tiles are skipped — only alive/wall/etc. tiles are emitted.
 */
public class TerrainSerializer extends JsonSerializer<GridSnapshot> {

  @Override
  public void serialize(GridSnapshot snapshot, JsonGenerator gen,
      SerializerProvider provider) throws IOException {
    gen.writeStartObject();
    gen.writeStringField("type", "full"); // distinguishes from GridDiff frames on the client

    // --- tiles ---
    gen.writeFieldName("tiles");
    gen.writeStartObject();
    for (var entry : snapshot.terrain().entrySet()) {
      if (entry.getValue() == TileType.EMPTY) {
        continue;
      }
      Vector4 pos = entry.getKey();
      gen.writeFieldName((int) pos.getX() + "," + (int) pos.getY());
      gen.writeString(entry.getValue().name());
    }
    gen.writeEndObject();

    // --- entities ---
    gen.writeFieldName("entities");
    gen.writeStartObject();
    for (var entry : snapshot.entities().entrySet()) {
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
