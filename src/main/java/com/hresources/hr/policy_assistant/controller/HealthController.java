package com.hresources.hr.policy_assistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes simple liveness endpoints for operational checks.
 */
@RestController
@Tag(name = "Health", description = "Infrastructure and liveness endpoints.")
public class HealthController {

    /**
     * Returns a basic application health payload.
     *
     * @return status response indicating the application is running
     */
    @GetMapping("/api/health")
    @Operation(summary = "Health check", description = "Simple endpoint used to verify that the application is running.")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
