package com.ntros.session;

import com.ntros.message.ProtocolContext;

import java.util.Optional;

public interface ProtocolHandler {

    Optional<String> handle(String rawMessage, ProtocolContext context);


}
