package com.ntros.parser;

import com.ntros.protocol.CommandType;
import com.ntros.protocol.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageParser implements Parser {

  private static final String DELIMITER = "\\s+";

  /**
   * minimum word count for a given message
   * - "REG client_1" => valid
   * - "client-42" => invalid
   * - "REG c abc, 1118" => valid structure. Semantically it's wrong, REG command expects at least
   * two words, but semantics are not validated here
   */
  private static final int MIN_WORD_COUNT = 2;

  /**
   * Validates message structure and attempts to
   * parse ot a Client Command that the server can process.
   */
  @Override
  public Message parse(String rawMessage) {
    log.info("received data: {}", rawMessage);
    String[] words = rawMessage.split(DELIMITER);
    if (words.length < MIN_WORD_COUNT) {
      String err = "[Parser]: Invalid message - word count less than 2.";
      log.error(err);
      throw new MessageParsingException(err);
    }

    // the first word of the message is expected to be the command.
    // Attempts to convert the word to an existing server-side Client command, otherwise throw exception
    CommandType command = CommandType.valueOf(
        words[0]); // throws IllegalArgument if command doesn't exist

    List<String> args = new ArrayList<>(Arrays.asList(words).subList(1, words.length));

    if (args.isEmpty()) {
      String err = "[Parser]: No arguments received.";
      log.error(err);
      throw new MessageParsingException(err);
    }
    Message parsedMessage = new Message(command, args);
    log.info("[Parser]: message parsed: {}", parsedMessage);

    return parsedMessage;
  }

}
