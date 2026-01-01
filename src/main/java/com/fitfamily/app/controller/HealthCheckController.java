package com.fitfamily.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple Health Check Controller
 * 
 * For detailed health checks with database status, use:
 * - /actuator/health (basic status)
 * - /actuator/health/readiness (readiness probe)
 * - /actuator/health/liveness (liveness probe)
 */
@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<Map<String, String>> healthCheck() {
		Map<String, String> response = new HashMap<>();
		response.put("status", "UP");
		response.put("application", "FitFamily Backend");
		response.put("message", "Application is running");
		return ResponseEntity.ok(response);
	}

}

