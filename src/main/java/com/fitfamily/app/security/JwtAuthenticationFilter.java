package com.fitfamily.app.security;

import com.fitfamily.app.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
		this.jwtUtil = jwtUtil;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		// Extract Authorization header
		final String authHeader = request.getHeader("Authorization");

		// Check if header exists and starts with "Bearer "
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract token (remove "Bearer " prefix)
		final String jwt = authHeader.substring(7);
		final String userEmail;

		try {
			// Extract email from token
			userEmail = jwtUtil.extractEmail(jwt);

			// If email exists and no authentication is set yet
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				
				// Validate token
				if (jwtUtil.validateToken(jwt, userEmail)) {
					
					// Extract role from token
					String role = jwtUtil.extractRole(jwt);
					
					// Create authentication token
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
							userEmail,
							null,
							Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
					);
					
					// Set authentication details
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					
					// Set authentication in SecurityContext
					SecurityContextHolder.getContext().setAuthentication(authToken);
				}
			}
		} catch (Exception e) {
			// Log and continue without authentication
			logger.error("JWT authentication failed: " + e.getMessage());
		}

		// Continue filter chain
		filterChain.doFilter(request, response);
	}

}

