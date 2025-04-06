package com.ntros.model.world;

import java.util.List;

public class Message {

    private CommandType command;
    private List<String> args;

    public Message() {

    }

    public Message(CommandType command, List<String> args) {
        this.command = command;
        this.args = args;
    }

    public CommandType getCommand() {
        return command;
    }


    public List<String> getArgs() {
        return args;
    }

    @Override
    public String toString() {
        return "Message{" +
                "command=" + command +
                ", args=" + args +
                '}';
    }
}
