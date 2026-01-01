package com.fitfamily.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration for FitFamily Backend
 * 
 * Security Features:
 * - Stateless JWT authentication (no server-side sessions)
 * - CSRF protection disabled (not needed for stateless APIs)
 * - CORS enabled for frontend communication
 * - Security headers enabled (XSS, Content Type, Frame Options)
 * - Environment-aware H2 console access (dev only)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	@Value("${cors.allowed-origins:http://localhost:3000,http://localhost}")
	private String[] allowedOrigins;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Enable CORS (uses corsConfigurationSource bean)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			
			// Disable CSRF (not needed for stateless REST APIs with JWT)
			.csrf(csrf -> csrf.disable())
			
			// Configure authorization rules
			.authorizeHttpRequests(auth -> {
				// Public endpoints - always accessible
				auth.requestMatchers(
					"/auth/**", 
					"/health", 
					"/actuator/health",
					"/actuator/health/**",
					"/actuator/info"
				).permitAll();
				
				// H2 Console - only in development
				if (isDevelopmentProfile()) {
					auth.requestMatchers("/h2-console/**").permitAll();
				}
				
				// All other endpoints require authentication
				auth.anyRequest().authenticated();
			})
			
			// CRITICAL: Stateless session management
			// No server-side sessions, no cookies - JWT only
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			
			// Security headers configuration
			.headers(headers -> headers
				// XSS Protection (deprecated but still useful for older browsers)
				.xssProtection(xss -> xss.disable())  // Modern browsers use CSP instead
				
				// Content Type Options - prevent MIME sniffing
				.contentTypeOptions(contentType -> {})  // Enabled by default
				
				// Frame Options - clickjacking protection
				.frameOptions(frameOptions -> {
					if (isDevelopmentProfile()) {
						// Allow frames for H2 console in development
						frameOptions.disable();
					} else {
						// Deny frames in production (clickjacking protection)
						frameOptions.deny();
					}
				})
				
				// HTTP Strict Transport Security (HSTS) - commented out by default
				// Enable if using HTTPS in production
				// .httpStrictTransportSecurity(hsts -> hsts
				//     .includeSubDomains(true)
				//     .maxAgeInSeconds(31536000)
				// )
			);

		// Add JWT authentication filter
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	
	/**
	 * CORS Configuration
	 * Allows frontend applications to communicate with the backend
	 * 
	 * Security considerations:
	 * - Specific origins only (not wildcard *)
	 * - Explicit HTTP methods
	 * - Authorization header allowed (for JWT tokens)
	 * - Credentials not allowed by default (more secure)
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		
		// Allowed origins (frontend URLs)
		configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
		
		// Allowed HTTP methods
		configuration.setAllowedMethods(Arrays.asList(
			"GET", 
			"POST", 
			"PUT", 
			"DELETE", 
			"OPTIONS"
		));
		
		// Allowed headers (including Authorization for JWT)
		configuration.setAllowedHeaders(Arrays.asList(
			"Authorization",
			"Content-Type",
			"Accept"
		));
		
		// Expose these headers to the frontend
		configuration.setExposedHeaders(Arrays.asList(
			"Authorization"
		));
		
		// Allow credentials (cookies, authorization headers)
		// Set to false for better security unless you need cookies
		configuration.setAllowCredentials(false);
		
		// How long browsers can cache CORS preflight responses (1 hour)
		configuration.setMaxAge(3600L);
		
		// Apply CORS configuration to all endpoints
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		
		return source;
	}
	
	/**
	 * Check if running in development profile
	 * @return true if development, false otherwise
	 */
	private boolean isDevelopmentProfile() {
		return "dev".equalsIgnoreCase(activeProfile);
	}

}

