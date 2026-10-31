package com.example.energier.server.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight, unauthenticated liveness endpoint used by the web client and by
 * infrastructure probes. It deliberately does not touch the database so it
 * answers quickly even when a downstream service is slow.
 *
 * <p>`/api/v1/health` is the path the web client uses so it goes through the
 * nginx `/api/` proxy in production. `/health` is kept for direct probes.</p>
 */
@RestController
public class HealthController {

    @GetMapping({"/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "status", "UP",
                        "service", "energir-server",
                        "timestamp", Instant.now().toString()
                ));
    }
}
