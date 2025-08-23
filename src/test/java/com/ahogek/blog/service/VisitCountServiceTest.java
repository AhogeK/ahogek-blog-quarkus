package com.ahogek.blog.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 网站访问统计服务测试
 *
 * @author AhogeK
 * @since 2025-08-22 18:37:08
 */
@QuarkusTest
class VisitCountServiceTest {

    private static final Logger LOGGER = Logger.getLogger(VisitCountServiceTest.class);

    @Inject
    VisitCountService visitCountService;

    @Test
    void getCurrentCountTest() {
        Assertions.assertDoesNotThrow(() -> {
            int currentCount = visitCountService.getCurrentCount();
            LOGGER.info("当前访问次数：" + currentCount);
        });
    }
}
