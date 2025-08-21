package com.ahogek.blog;

import com.ahogek.blog.socket.OnlineCounterSocket;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.websocket.*;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author AhogeK
 * @since 2025-07-25 09:36:55
 */
@QuarkusTest
class OnlineCounterSocketTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();
    private static final Logger LOGGER = Logger.getLogger(OnlineCounterSocketTest.class);

    @TestHTTPResource("/online")
    URI uri;

    @Test
    void testOnlineCounter() throws Exception {
        try (
                Session session1 = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri);
                Jsonb jsonb = JsonbBuilder.create()
        ) {
            // 第一个用户连接
            String message1 = MESSAGES.poll(10, TimeUnit.SECONDS);
            OnlineCounterSocket.UserCountMessage userCountMessage1 = jsonb.fromJson(message1, OnlineCounterSocket.UserCountMessage.class);
            Assertions.assertEquals(1, userCountMessage1.getCount());

            try (Session session2 = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
                // 第二个用户连接，会收到两条消息，一条来自session1，一条来自session2
                String message2_1 = MESSAGES.poll(10, TimeUnit.SECONDS);
                OnlineCounterSocket.UserCountMessage userCountMessage2_1 = jsonb.fromJson(message2_1, OnlineCounterSocket.UserCountMessage.class);
                Assertions.assertEquals(2, userCountMessage2_1.getCount());

                String message2_2 = MESSAGES.poll(10, TimeUnit.SECONDS);
                OnlineCounterSocket.UserCountMessage userCountMessage2_2 = jsonb.fromJson(message2_2, OnlineCounterSocket.UserCountMessage.class);
                Assertions.assertEquals(2, userCountMessage2_2.getCount());
            }

            // 第二个用户断开连接
            String message3 = MESSAGES.poll(10, TimeUnit.SECONDS);
            OnlineCounterSocket.UserCountMessage userCountMessage3 = jsonb.fromJson(message3, OnlineCounterSocket.UserCountMessage.class);
            Assertions.assertEquals(1, userCountMessage3.getCount());
        }
    }

    @ClientEndpoint
    public static class Client {
        @OnOpen
        public void open(Session session) {
            LOGGER.infof("Connecting to endpoint: %s", session.getRequestURI());
        }

        @OnMessage
        public void message(String msg) {
            MESSAGES.add(msg);
        }
    }
}
