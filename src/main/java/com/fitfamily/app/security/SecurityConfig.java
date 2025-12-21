package com.fitfamily.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Disable CSRF (not needed for stateless REST APIs)
			.csrf(csrf -> csrf.disable())
			
			// Configure authorization rules
			.authorizeHttpRequests(auth -> auth
				// Permit public endpoints
				.requestMatchers("/auth/**", "/health", "/h2-console/**").permitAll()
				// Secure all other endpoints
				.anyRequest().authenticated()
			)
			
			// Stateless session management (no cookies)
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			
		// Disable frame options for H2 console
		.headers(headers -> headers
			.frameOptions(frameOptions -> frameOptions.disable())
		);

		// Add JWT filter before UsernamePasswordAuthenticationFilter
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}

