package com.ntros.model.world.protocol;

public enum CommandType {
  ///  CLIENT COMMANDS ///
  // AUTHs client with the server
  AUTHENTICATE,
  // JOIN a multiplayer world
  JOIN,
  // DISCONNECT from current connected world
  DISCONNECT,
  // MOVE ACTION in current connected world
  MOVE,
  // CREATE world
  CREATE,
  // CLEAR world state
  CLEAR,
  // START ticking the world
  START,
  ORCHESTRATE,

  ///  SERVER COMMANDS ///
 // Auth success
  AUTH_SUCCESS,
  // Server broadcasts it's current world STATE
  STATE,
  // Server sends WELCOME message in response to client's success JOIN command
  WELCOME,
  // in response to any failed CLIENT command
  ERROR,
  // in response to any client ACTION
  ACK
}
