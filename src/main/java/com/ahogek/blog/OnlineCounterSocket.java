package com.ahogek.blog;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author AhogeK
 * @since 2025-07-24 07:55:23
 */
@ServerEndpoint("/online")
@ApplicationScoped
public class OnlineCounterSocket {

    private static final Logger LOGGER = Logger.getLogger(OnlineCounterSocket.class);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final Jsonb jsonb;

    @Inject
    public OnlineCounterSocket(Jsonb jsonb) {
        this.jsonb = jsonb;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        LOGGER.infof("onOpen> %s", session.getId());
        broadcastUserCount();
    }

    @OnClose
    @SuppressWarnings("resource")
    public void onClose(Session session) {
        // WebSocket Session 由容器管理，不需要手动关闭
        sessions.remove(session.getId());
        LOGGER.infof("onClose> %s", session.getId());
        broadcastUserCount();
    }

    @OnError
    @SuppressWarnings("resource")
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session.getId());
        LOGGER.errorf(throwable, "onError> %s", session.getId());
        broadcastUserCount();
    }

    private void broadcastUserCount() {
        UserCountMessage message = new UserCountMessage(sessions.size());
        String jsonMessage = jsonb.toJson(message);
        sessions.values().forEach(session -> session.getAsyncRemote().sendText(jsonMessage, result -> {
            if (result.getException() != null) {
                LOGGER.errorf("Failed to send message to %s: %s", session.getId(), result.getException());
            }
        }));
    }

    @RegisterForReflection
    public static class UserCountMessage {
        private int count; // 1. 将字段设为 private

        public UserCountMessage() {
        }

        public UserCountMessage(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
