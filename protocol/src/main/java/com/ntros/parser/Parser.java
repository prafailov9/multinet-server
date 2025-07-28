package com.ntros.parser;

import com.ntros.model.world.protocol.Message;

public interface Parser {

  Message parse(String data);

}
