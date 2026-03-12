package com.ntros.broadcast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractBroadcaster implements Broadcaster {

  private final ExecutorService broadcastExecutor = Executors.newSingleThreadExecutor();


}
