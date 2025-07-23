package com.ahogek;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

@ServerEndpoint("/start-websocket/{name}")
@ApplicationScoped
public class StartWebSocket {

    private static final Logger LOGGER = Logger.getLogger(StartWebSocket.class);

    @OnOpen
    public void onOpen(Session session, @PathParam("name") String name) {
        LOGGER.infof("onOpen> %s", name);
    }

    @OnClose
    public void onClose(Session session, @PathParam("name") String name) {
        LOGGER.infof("onClose> %s", name);
    }

    @OnError
    public void onError(Session session, @PathParam("name") String name, Throwable throwable) {
        LOGGER.errorf(throwable, "onError> %s", name);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("name") String name, Session session) {
        LOGGER.infof("onMessage> %s: %s", name, message);

        String reply = "Server received from " + name + ": " + message;
        session.getAsyncRemote().sendText(reply);
    }
}
