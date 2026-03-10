package com.ntros.parser;

import com.ntros.protocol.Message;

public interface Parser {

  Message parse(String data);

}
