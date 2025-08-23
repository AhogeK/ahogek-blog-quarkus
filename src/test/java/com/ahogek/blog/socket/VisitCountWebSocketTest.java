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
import java.util.ArrayList;
import java.util.List;
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
    void testConnection_ShouldReceiveValidMessage() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<JsonObject> receivedMessage = new AtomicReference<>();

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

        // 验证连接成功
        Assertions.assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "连接超时");

        // 验证收到消息
        Assertions.assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "消息接收超时");

        // 验证消息格式
        JsonObject json = receivedMessage.get();
        Assertions.assertNotNull(json, "应该收到消息");
        Assertions.assertTrue(json.containsKey("count"), "消息应包含count字段");

        // 只验证是有效数字，不关心具体值
        Integer count = json.getInteger("count");
        Assertions.assertNotNull(count, "count不能为null");
        Assertions.assertTrue(count >= 0, "count应该是非负整数");
    }

    @Test
    void testMultipleConnections_AllShouldReceiveMessages() throws Exception {
        final int connectionCount = 3;
        CountDownLatch allMessagesLatch = new CountDownLatch(connectionCount);
        List<AtomicReference<JsonObject>> receivedMessages = new ArrayList<>();

        for (int i = 0; i < connectionCount; i++) {
            AtomicReference<JsonObject> receivedMessage = new AtomicReference<>();
            receivedMessages.add(receivedMessage);

            final int connectionIndex = i;

            client.connect(uri.getPort(), uri.getHost(), uri.getPath())
                    .onSuccess(ws -> {
                        LOGGER.infof("连接 %d 建立成功", connectionIndex + 1);

                        ws.handler(buffer -> {
                            try {
                                JsonObject json = new JsonObject(buffer.toString());
                                LOGGER.infof("连接 %d 收到消息: %s", connectionIndex + 1, json);

                                // 只记录第一条消息
                                if (receivedMessage.compareAndSet(null, json)) {
                                    allMessagesLatch.countDown();
                                }
                            } catch (Exception e) {
                                LOGGER.error("连接 " + (connectionIndex + 1) + " 解析消息失败", e);
                            }
                        });
                    })
                    .onFailure(throwable -> {
                        LOGGER.error("连接 " + (connectionIndex + 1) + " 失败", throwable);
                        allMessagesLatch.countDown(); // 即使失败也要countdown，避免无限等待
                    });

            // 连接间隔
            Thread.sleep(200);
        }

        // 等待所有连接都收到消息
        Assertions.assertTrue(allMessagesLatch.await(10, TimeUnit.SECONDS), "部分连接未收到消息");

        // 验证所有连接都收到了有效消息
        for (int i = 0; i < receivedMessages.size(); i++) {
            JsonObject json = receivedMessages.get(i).get();
            Assertions.assertNotNull(json, String.format("连接 %d 应该收到消息", i + 1));
            Assertions.assertTrue(json.containsKey("count"), "所有消息都应包含count字段");
        }
    }
}
