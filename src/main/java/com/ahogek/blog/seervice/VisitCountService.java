package com.ahogek.blog.seervice;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * 访问统计服务
 *
 * @author AhogeK
 * @since 2025-08-21 14:27:49
 */
@ApplicationScoped
public class VisitCountService {

    private static final String VISIT_COUNT_KEY = "website:visit:count";

    private final RedisDataSource redisDataSource;

    public VisitCountService(RedisDataSource redisDataSource) {
        this.redisDataSource = redisDataSource;
    }

    public int incrementCount() {
        ValueCommands<String, String> valueCommands = redisDataSource.value(String.class);
        long newCount = valueCommands.incr(VISIT_COUNT_KEY);
        return (int) newCount;
    }
}
