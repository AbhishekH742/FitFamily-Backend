package com.fitfamily.app.service;

import com.fitfamily.app.dto.LoginRequest;
import com.fitfamily.app.dto.LoginResponse;
import com.fitfamily.app.dto.RegisterRequest;
import com.fitfamily.app.exception.EmailAlreadyExistsException;
import com.fitfamily.app.exception.InvalidCredentialsException;
import com.fitfamily.app.model.Role;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.UserRepository;
import com.fitfamily.app.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	public User register(RegisterRequest request) {
		// Check if email already exists
		if (userRepository.findByEmail(request.getEmail()).isPresent()) {
			throw new EmailAlreadyExistsException("Email is already registered: " + request.getEmail());
		}

		// Create new user
		User user = new User();
		user.setName(request.getName());
		user.setEmail(request.getEmail());
		
		// Hash password
		String hashedPassword = passwordEncoder.encode(request.getPassword());
		user.setPassword(hashedPassword);
		
		// Assign default role
		user.setRole(Role.MEMBER);

		// Save and return user
		return userRepository.save(user);
	}

	public LoginResponse login(LoginRequest request) {
		// Find user by email
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		// Verify password
		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		// Generate JWT token
		String token = jwtUtil.generateToken(user);

		// Return login response with token
		return new LoginResponse(token, user.getEmail(), user.getRole().toString());
	}

}

