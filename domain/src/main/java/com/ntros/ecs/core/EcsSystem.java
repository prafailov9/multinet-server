package com.ntros.ecs.core;

/**
 * A single simulation step operating on the {@link EcsWorld}.
 *
 * <p>Systems are registered with {@link EcsWorld#addSystem} and executed in registration order
 * by {@link EcsWorld#tick}. Each call receives the world and the elapsed time in seconds so
 * position / velocity integrations are frame-rate independent.
 *
 * <p>Being a {@code @FunctionalInterface}, systems can be written as lambdas or method
 * references — no boilerplate subclass required.
 */
@FunctionalInterface
public interface EcsSystem {

  void update(EcsWorld world, float dt);
}
