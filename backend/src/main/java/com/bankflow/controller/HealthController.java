package com.bankflow.controller;

import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight readiness probe for the frontend. Actuator covers the operational
 * side; this exists so the React app can show a real connection banner and so
 * the database credentials can be verified without opening a SQL client.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final Environment environment;

    @Value("${info.app.version}")
    private String version;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", "BankFlow");
        body.put("version", version);
        body.put("profiles", environment.getActiveProfiles());
        body.put("timestamp", Instant.now().toString());
        body.put("database", describeDatabase());
        body.put("status", "UP");
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> describeDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            db.put("reachable", true);
            db.put("product", connection.getMetaData().getDatabaseProductName());
            db.put("version", connection.getMetaData().getDatabaseProductVersion());
            db.put("schema", connection.getCatalog());
        } catch (Exception e) {
            log.warn("Database probe failed: {}", e.getMessage());
            db.put("reachable", false);
            db.put("error", e.getMessage());
        }
        return db;
    }
}
