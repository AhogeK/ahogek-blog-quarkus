package com.ahogek.blog.socket;

import com.ahogek.blog.service.VisitCountService;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 网站访问统计WebSocket测试
 *
 * @author AhogeK
 * @since 2025-08-22 18:45:54
 */
@QuarkusTest
class VisitCountWebSocketTest {

    private static final Logger LOGGER = Logger.getLogger(VisitCountWebSocketTest.class);

    @TestHTTPResource("/websocket/visit-count")
    URI uri;

    @Inject
    VisitCountService visitCountService;

    private Vertx vertx;
    private WebSocketClient client;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        client = vertx.createWebSocketClient();

        // 重置计数器到已知状态
        // visitCountService.setCountAsync(0).await().indefinitely();
    }

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }

    @Test
    void testSingleConnection_ShouldReceiveMessage() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> receivedMessage = new AtomicReference<>();
        CountDownLatch connectionLatch = new CountDownLatch(1);

        client.connect(uri.getPort(), uri.getHost(), uri.getPath())
                .onSuccess(ws -> {
                    LOGGER.info("WebSocket连接成功");
                    connectionLatch.countDown();

                    ws.handler(buffer -> {
                        try {
                            String message = buffer.toString();
                            LOGGER.infof("收到消息: %s", message);
                            JsonObject json = new JsonObject(message);
                            receivedMessage.set(json);
                            messageLatch.countDown();
                        } catch (Exception e) {
                            LOGGER.error("解析消息失败", e);
                        }
                    });
                })
                .onFailure(throwable -> {
                    LOGGER.error("连接失败", throwable);
                    connectionLatch.countDown();
                });

        // 等待连接建立
        Assertions.assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "连接超时");

        // 等待收到消息
        Assertions.assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "消息接收超时");

        // 验证消息格式
        JsonObject json = receivedMessage.get();
        Assertions.assertNotNull(json, "应该收到消息");
        Assertions.assertTrue(json.containsKey("count"), "消息应包含count字段");
    }
}
