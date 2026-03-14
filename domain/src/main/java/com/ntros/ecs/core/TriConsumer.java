package com.ntros.ecs.core;

/**
 * Functional interface for a three-argument consumer, used in ECS cross-component queries.
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> {

  void accept(A a, B b, C c);
}
