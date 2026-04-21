package com.expensetracker.web;

import com.expensetracker.ExpenseTrackerApplication;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests hitting the full Spring Boot stack with a real PostgreSQL.
 * Testcontainers spins up a disposable database — no H2 impedance mismatch.
 */
@SpringBootTest(
    classes = ExpenseTrackerApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExpenseControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    private HttpHeaders headersWithKey() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        return headers;
    }

    private HttpHeaders headersWithKey(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        return headers;
    }

    private static final String VALID_BODY = """
            {"amount":"1250.50","category":"food","description":"Lunch","date":"2026-04-21"}
            """;

    // ── CREATE ──────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /api/expenses → 201 Created")
    void createExpense_returns201() {
        HttpEntity<String> request = new HttpEntity<>(VALID_BODY, headersWithKey());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/expenses", request, Map.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("1250.50", response.getBody().get("amount"));
        assertEquals("food", response.getBody().get("category"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/expenses (same key) → 200 OK (idempotent replay)")
    void createExpense_idempotentReplay_returns200() {
        String key = UUID.randomUUID().toString();

        // First request
        HttpEntity<String> request1 = new HttpEntity<>(VALID_BODY, headersWithKey(key));
        ResponseEntity<Map> response1 = restTemplate.postForEntity("/api/expenses", request1, Map.class);
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());

        // Same key, same body → idempotent replay
        HttpEntity<String> request2 = new HttpEntity<>(VALID_BODY, headersWithKey(key));
        ResponseEntity<Map> response2 = restTemplate.postForEntity("/api/expenses", request2, Map.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        // Same expense returned
        assertEquals(response1.getBody().get("id"), response2.getBody().get("id"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/expenses (same key, different body) → 409 Conflict")
    void createExpense_sameKeyDifferentBody_returns409() {
        String key = UUID.randomUUID().toString();

        // First request
        HttpEntity<String> request1 = new HttpEntity<>(VALID_BODY, headersWithKey(key));
        restTemplate.postForEntity("/api/expenses", request1, Map.class);

        // Same key, different amount
        String differentBody = """
                {"amount":"999.99","category":"transport","description":"Taxi","date":"2026-04-20"}
                """;
        HttpEntity<String> request2 = new HttpEntity<>(differentBody, headersWithKey(key));
        ResponseEntity<Map> response2 = restTemplate.postForEntity("/api/expenses", request2, Map.class);

        assertEquals(HttpStatus.CONFLICT, response2.getStatusCode());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/expenses without Idempotency-Key → 422")
    void createExpense_missingKey_returns422() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // No Idempotency-Key header

        HttpEntity<String> request = new HttpEntity<>(VALID_BODY, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expenses", request, Map.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/expenses with negative amount → 400")
    void createExpense_negativeAmount_returns400() {
        String body = """
                {"amount":"-50","category":"food","description":"Bad","date":"2026-04-21"}
                """;
        HttpEntity<String> request = new HttpEntity<>(body, headersWithKey());
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expenses", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/expenses with zero amount → 400")
    void createExpense_zeroAmount_returns400() {
        String body = """
                {"amount":"0.00","category":"food","description":"Free","date":"2026-04-21"}
                """;
        HttpEntity<String> request = new HttpEntity<>(body, headersWithKey());
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/expenses", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── LIST ────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /api/expenses → 200 with paginated response")
    void getExpenses_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/expenses", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("content"));
        assertNotNull(response.getBody().get("page"));
        assertNotNull(response.getBody().get("summary"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/expenses?category=food → filtered results")
    void getExpenses_filterByCategory() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/expenses?category=food", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── CATEGORIES ──────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("GET /api/expenses/categories → list of categories")
    void getCategories_returnsCategories() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/expenses/categories", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("categories"));
    }

    // ── HEALTH ──────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("GET /actuator/health → UP")
    void healthCheck() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/actuator/health", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
    }
}
