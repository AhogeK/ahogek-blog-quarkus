package com.ahogek.blog;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

/**
 * @author AhogeK
 * @since 2025-08-21 14:52:45
 */
@QuarkusTest
class VisitCountServiceTest {

    @Test
    void testRedisOperations() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON).when()
                .post("/api/visit/increment")
                .then()
                .statusCode(200)
                .body("count", org.hamcrest.Matchers.greaterThan(0));
    }
}
