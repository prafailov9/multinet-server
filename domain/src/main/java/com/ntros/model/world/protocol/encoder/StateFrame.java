package com.ntros.model.world.protocol.encoder;

// carries only metadata + the already-shaped domain snapshot
public record StateFrame(
    int proto,      // protocol version
    String inst,    // instance/world name
    long seq,       // monotonically increasing sequence
    Object data     // domain-specific snapshot (GridSnapshot, TrafficSnapshot, etc.)
) {

}