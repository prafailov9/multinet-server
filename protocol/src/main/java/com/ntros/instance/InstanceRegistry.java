package com.ntros.instance;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceRegistry {

  private static final Map<String, Instance> INSTANCES = new ConcurrentHashMap<>();

  public static void register(Instance instance) {
    INSTANCES.put(instance.worldName(), instance);
  }

  public static Collection<Instance> getAll() {
    return INSTANCES.values();
  }

  public static void clear() {
    INSTANCES.clear();
  }
}
