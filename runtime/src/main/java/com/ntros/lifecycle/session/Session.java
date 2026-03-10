package com.ntros.lifecycle.session;

import com.ntros.lifecycle.Lifecycle;
import com.ntros.lifecycle.Shutdownable;
import com.ntros.message.SessionContext;

/**
 * Represents an active connection between a client and the server.
 */
public interface Session extends Lifecycle, Shutdownable {

  SessionContext getSessionContext();


  /**
   * Sends server response to client
   *
   * @param serverResponse - the generated server response from
   */
  void response(String serverResponse);


  /**
   * ends session
   */
  void shutdown();
}
