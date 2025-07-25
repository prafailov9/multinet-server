package com.ntros.parser;

import com.ntros.model.world.Message;

public interface Parser {

  Message parse(String data);

}
