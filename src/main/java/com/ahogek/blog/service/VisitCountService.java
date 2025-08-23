package com.ahogek.blog.service;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * 访问统计服务
 *
 * @author AhogeK
 * @since 2025-08-21 14:27:49
 */
@ApplicationScoped
public class VisitCountService {

    private static final Logger LOGGER = Logger.getLogger(VisitCountService.class);
    private static final String VISIT_COUNT_KEY = "website:visit:count";

    private final ReactiveValueCommands<String, String> valueCommands;

    public VisitCountService(ReactiveRedisDataSource reactiveRedisDataSource) {
        this.valueCommands = reactiveRedisDataSource.value(String.class);
    }

    /**
     * 异步获取当前访问计数
     */
    public Uni<Integer> getCurrentCountAsync() {
        return valueCommands.get(VISIT_COUNT_KEY)
                .onItem().transform(count -> {
                    if (count != null) {
                        try {
                            return Integer.parseInt(count);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("访问计数格式错误，重置为0: " + count);
                            return 0;
                        }
                    }
                    return 0;
                })
                .onFailure().invoke(throwable ->
                        LOGGER.error("获取访问计数失败", throwable))
                .onFailure().recoverWithItem(0); // 失败时返回默认值
    }

    /**
     * 异步增加访问计数
     */
    public Uni<Integer> incrementCountAsync() {
        return valueCommands.incr(VISIT_COUNT_KEY)
                .onItem().transform(Long::intValue)
                .onFailure().invoke(throwable ->
                        LOGGER.error("增加访问计数失败", throwable))
                .onFailure().recoverWithItem(1); // 失败时返回默认值
    }

    /**
     * 异步设置访问计数
     */
    public Uni<Void> setCountAsync(int count) {
        return valueCommands.set(VISIT_COUNT_KEY, String.valueOf(count))
                .onFailure().invoke(throwable ->
                        LOGGER.error("设置访问计数失败", throwable));
    }

    // 保留阻塞版本供某些场景使用（如测试、同步场景）
    public int getCurrentCount() {
        return getCurrentCountAsync().await().indefinitely();
    }

    public int incrementCount() {
        return incrementCountAsync().await().indefinitely();
    }
}
