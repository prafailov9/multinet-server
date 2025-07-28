package com.ntros.parser;

import com.ntros.model.world.protocol.CommandType;
import com.ntros.model.world.protocol.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageParser implements Parser {

  private static final String DELIMITER = "\\s+";
  private static final int MIN_WORD_COUNT = 2;

  @Override
  public Message parse(String data) {
    log.info("received data: {}", data);
    String[] words = data.split(DELIMITER);
    if (words.length < MIN_WORD_COUNT) {
      String err = "[Parser]: Invalid message - word count less than 2.";
      log.error(err);
      throw new MessageParsingException(err);
    }
    CommandType command = CommandType.valueOf(
        words[0]); // throws IllegalArgument if command doesnt exist
    List<String> args = new ArrayList<>(Arrays.asList(words).subList(1, words.length));

    if (args.isEmpty()) {
      String err = "[Parser]: No arguments received.";
      log.error(err);
      throw new MessageParsingException(err);
    }
    return new Message(command, args);
  }

  private void validateArgs(List<String> args) {
    args.forEach(arg -> {
      if (arg.length() < 2 && !Character.isLetter(arg.charAt(0))) {
        throw new MessageParsingException("Invalid argument: " + arg);
      }
    });
  }


}
