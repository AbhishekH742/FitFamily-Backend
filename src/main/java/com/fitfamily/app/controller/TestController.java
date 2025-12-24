package com.fitfamily.app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

	@GetMapping("/secured")
	public ResponseEntity<String> securedEndpoint() {
		// Get authenticated user's email from SecurityContext
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String userEmail = authentication.getName();
		String userRole = authentication.getAuthorities().toString();
		
		return ResponseEntity.ok(
			"JWT authentication successful! Authenticated user: " + userEmail + " with role: " + userRole
		);
	}

}

