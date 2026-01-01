package com.fitfamily.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfamily.app.dto.LoginRequest;
import com.fitfamily.app.dto.RegisterRequest;
import com.fitfamily.app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@AfterEach
	void cleanup() {
		// Clean up database after each test to ensure test isolation
		userRepository.deleteAll();
	}

	@Test
	void registerUser_success() throws Exception {
		// Arrange
		RegisterRequest request = new RegisterRequest();
		request.setName("John Doe");
		request.setEmail("john@example.com");
		request.setPassword("password123");

		// Act & Assert
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(content().string(containsString("User registered successfully")))
			.andExpect(content().string(containsString("john@example.com")));
	}

	@Test
	void registerUser_duplicateEmail_returnsBadRequest() throws Exception {
		// Arrange - Register first user
		RegisterRequest request1 = new RegisterRequest();
		request1.setName("John Doe");
		request1.setEmail("john@example.com");
		request1.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1)))
			.andExpect(status().isCreated());

		// Arrange - Try to register with same email
		RegisterRequest request2 = new RegisterRequest();
		request2.setName("Jane Doe");
		request2.setEmail("john@example.com"); // Same email
		request2.setPassword("password456");

		// Act & Assert
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("Duplicate Email"))
			.andExpect(jsonPath("$.message").value(containsString("Email is already registered")));
	}

	@Test
	void registerUser_invalidEmail_returnsBadRequest() throws Exception {
		// Arrange
		RegisterRequest request = new RegisterRequest();
		request.setName("John Doe");
		request.setEmail("invalid-email"); // Invalid email format
		request.setPassword("password123");

		// Act & Assert
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.email").exists())
			.andExpect(jsonPath("$.email").value(containsString("valid")));
	}

	@Test
	void registerUser_shortPassword_returnsBadRequest() throws Exception {
		// Arrange
		RegisterRequest request = new RegisterRequest();
		request.setName("John Doe");
		request.setEmail("john@example.com");
		request.setPassword("123"); // Too short (less than 6 characters)

		// Act & Assert
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.password").exists())
			.andExpect(jsonPath("$.password").value(containsString("at least 6 characters")));
	}

	@Test
	void loginUser_success_returnsJwt() throws Exception {
		// Arrange - First register a user
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setName("John Doe");
		registerRequest.setEmail("john@example.com");
		registerRequest.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest)))
			.andExpect(status().isCreated());

		// Arrange - Create login request
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("john@example.com");
		loginRequest.setPassword("password123");

		// Act & Assert
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").exists())
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.token").value(not(emptyString())))
			.andExpect(jsonPath("$.email").value("john@example.com"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.message").value("Login successful"))
			.andReturn();

		// Verify JWT token format (should have 3 parts separated by dots)
		String responseBody = result.getResponse().getContentAsString();
		String token = objectMapper.readTree(responseBody).get("token").asText();
		String[] tokenParts = token.split("\\.");
		org.junit.jupiter.api.Assertions.assertEquals(3, tokenParts.length, 
			"JWT token should have 3 parts (header.payload.signature)");
	}

	@Test
	void loginUser_wrongPassword_returnsUnauthorized() throws Exception {
		// Arrange - First register a user
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setName("John Doe");
		registerRequest.setEmail("john@example.com");
		registerRequest.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest)))
			.andExpect(status().isCreated());

		// Arrange - Create login request with wrong password
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("john@example.com");
		loginRequest.setPassword("wrongpassword");

		// Act & Assert
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("Authentication Failed"))
			.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void loginUser_userNotFound_returnsUnauthorized() throws Exception {
		// Arrange - No user registered
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("nonexistent@example.com");
		loginRequest.setPassword("password123");

		// Act & Assert
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error").value("Authentication Failed"))
			.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void loginUser_missingFields_returnsBadRequest() throws Exception {
		// Arrange - Login request with missing password
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("john@example.com");
		// No password set

		// Act & Assert
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isBadRequest());
	}

	@Test
	void registerAndLogin_fullFlow_success() throws Exception {
		// Step 1: Register
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setName("Jane Doe");
		registerRequest.setEmail("jane@example.com");
		registerRequest.setPassword("securePass123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest)))
			.andExpect(status().isCreated())
			.andExpect(content().string(containsString("jane@example.com")));

		// Step 2: Login
		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("jane@example.com");
		loginRequest.setPassword("securePass123");

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").exists())
			.andExpect(jsonPath("$.email").value("jane@example.com"))
			.andExpect(jsonPath("$.role").value("MEMBER"));
	}

}

