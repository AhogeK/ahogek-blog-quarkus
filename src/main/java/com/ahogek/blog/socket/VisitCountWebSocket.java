package com.ahogek.blog.socket;

import com.ahogek.blog.service.VisitCountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 访问计数器WebSocket
 *
 * @author AhogeK
 * @since 2025-08-22 18:29:09
 */
@ApplicationScoped
@ServerEndpoint("/websocket/visit-count")
public class VisitCountWebSocket {
    private static final Logger LOGGER = Logger.getLogger(VisitCountWebSocket.class);
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private final VisitCountService visitCountService;

    public VisitCountWebSocket(VisitCountService visitCountService) {
        this.visitCountService = visitCountService;
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        LOGGER.infof("WebSocket连接建立: %s", session.getId());

        // 先增加访问计数，然后广播新的计数
        visitCountService.incrementCountAsync()
                .subscribe().with(
                        newCount -> {
                            LOGGER.infof("访问计数已增加到: %d", newCount);
                            broadcastCount(newCount);
                        },
                        failure -> {
                            LOGGER.error("增加访问计数失败，广播默认计数", failure);
                            // 即使增加失败，也尝试获取当前计数并广播
                            visitCountService.getCurrentCountAsync()
                                    .subscribe().with(
                                            this::broadcastCount,
                                            getCurrentFailure -> {
                                                LOGGER.error("获取当前计数也失败，广播默认值0", getCurrentFailure);
                                                broadcastCount(0);
                                            }
                                    );
                        }
                );
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        LOGGER.infof("WebSocket连接关闭: %s", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        LOGGER.errorf("WebSocket错误: %s - %s", session.getId(), throwable.getMessage());
    }

    /**
     * 向单个会话发送计数消息
     */
    private void sendCountToSession(Session session, int count) {
        if (session.isOpen()) {
            try {
                String message = String.format("{\"count\":%d}", count);
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                LOGGER.error("发送消息失败: " + session.getId(), e);
                sessions.remove(session);
            }
        } else {
            sessions.remove(session);
        }
    }

    /**
     * 广播计数更新到所有连接的会话
     */
    public void broadcastCount(int count) {
        Set<Session> sessionsToRemove = new HashSet<>();

        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    String message = String.format("{\"count\":%d}", count);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    LOGGER.error("广播消息失败: " + session.getId(), e);
                    sessionsToRemove.add(session);
                }
            } else {
                sessionsToRemove.add(session);
            }
        }

        // 清理无效会话
        sessions.removeAll(sessionsToRemove);
    }
}
