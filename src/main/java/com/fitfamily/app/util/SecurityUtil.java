package com.fitfamily.app.util;

import com.fitfamily.app.exception.UserNotFoundException;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

	private final UserRepository userRepository;

	public SecurityUtil(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Gets the currently authenticated user from Spring Security context
	 * 
	 * @return Currently authenticated User entity
	 * @throws UserNotFoundException if user is not found in database
	 * @throws IllegalStateException if no authentication is present
	 */
	public User getCurrentUser() {
		// Get authentication from Security Context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalStateException("No authenticated user found");
		}

		// Extract email from principal (set by JwtAuthenticationFilter)
		String email = authentication.getName();

		if (email == null || email.equals("anonymousUser")) {
			throw new IllegalStateException("No authenticated user found");
		}

		// Fetch and return user from database
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
	}

	/**
	 * Gets the email of the currently authenticated user
	 * 
	 * @return Email of the authenticated user
	 * @throws IllegalStateException if no authentication is present
	 */
	public String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new IllegalStateException("No authenticated user found");
		}

		String email = authentication.getName();

		if (email == null || email.equals("anonymousUser")) {
			throw new IllegalStateException("No authenticated user found");
		}

		return email;
	}

}

