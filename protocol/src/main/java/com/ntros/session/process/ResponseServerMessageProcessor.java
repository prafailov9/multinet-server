package com.ntros.session.process;

import static com.ntros.event.SessionEvent.sessionStarted;

import com.ntros.event.bus.SessionEventBus;
import com.ntros.session.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseServerMessageProcessor implements ServerMessageProcessor {

    private static final String SERVER_RESPONSE_WELCOME = "WELCOME";
    private static final String SERVER_RESPONSE_ACK = "ACK";

    @Override
    public void processResponse(String serverResponse, Session session) {
        boolean isAuth = session.getProtocolContext().isAuthenticated();

        if (isAuth && serverResponse.startsWith(SERVER_RESPONSE_WELCOME)) {
            log.info("Sending start session event. Sharing world state with user: {}",
                    session.getProtocolContext().getSessionId());
            SessionEventBus.get()
                    .publish(sessionStarted(session, "Starting client session...", serverResponse));
        }

        if (isAuth && serverResponse.startsWith(SERVER_RESPONSE_ACK)) {
            // just log, server is already ticking the state.
            log.info("Response from server: {}", serverResponse);
        }
    }
}
