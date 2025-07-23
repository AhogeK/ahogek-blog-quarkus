package com.ahogek;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * @author AhogeK
 * @since 2025-07-24 07:28:33
 */
@QuarkusTest
class StartWebSocketTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @TestHTTPResource("/start-websocket/test")
    URI uri;

    @Test
    void testWebSocket() throws DeploymentException, IOException, InterruptedException {
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri)) {
            Assertions.assertEquals("CONNECT", MESSAGES.poll(10, TimeUnit.SECONDS));
            session.getAsyncRemote().sendText("hello");
            String expectedReply = "Server received from test: hello";
            Assertions.assertEquals(expectedReply, MESSAGES.poll(10, TimeUnit.SECONDS));
        }

    }

    @ClientEndpoint
    static class Client {

        @OnOpen
        public void open(Session session) {
            MESSAGES.add("CONNECT");
        }

        @OnMessage
        public void message(String msg) {
            MESSAGES.add(msg);
        }
    }
}
