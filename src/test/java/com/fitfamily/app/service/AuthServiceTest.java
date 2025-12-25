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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthService authService;

	private RegisterRequest registerRequest;
	private LoginRequest loginRequest;
	private User user;

	@BeforeEach
	void setUp() {
		// Setup register request
		registerRequest = new RegisterRequest();
		registerRequest.setName("John Doe");
		registerRequest.setEmail("john@example.com");
		registerRequest.setPassword("password123");

		// Setup login request
		loginRequest = new LoginRequest();
		loginRequest.setEmail("john@example.com");
		loginRequest.setPassword("password123");

		// Setup user
		user = new User();
		user.setName("John Doe");
		user.setEmail("john@example.com");
		user.setPassword("hashedPassword");
		user.setRole(Role.MEMBER);
	}

	@Test
	void register_successful() {
		// Arrange
		when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
		when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashedPassword");
		when(userRepository.save(any(User.class))).thenReturn(user);

		// Act
		User result = authService.register(registerRequest);

		// Assert
		assertNotNull(result);
		assertEquals("John Doe", result.getName());
		assertEquals("john@example.com", result.getEmail());
		assertEquals("hashedPassword", result.getPassword());
		assertEquals(Role.MEMBER, result.getRole());

		// Verify interactions
		verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
		verify(passwordEncoder, times(1)).encode(registerRequest.getPassword());
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void register_duplicateEmail_throwsException() {
		// Arrange
		when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(user));

		// Act & Assert
		EmailAlreadyExistsException exception = assertThrows(
			EmailAlreadyExistsException.class,
			() -> authService.register(registerRequest)
		);

		assertEquals("Email is already registered: john@example.com", exception.getMessage());

		// Verify interactions
		verify(userRepository, times(1)).findByEmail(registerRequest.getEmail());
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void login_successful() {
		// Arrange
		String expectedToken = "jwt.token.here";
		when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
		when(jwtUtil.generateToken(user)).thenReturn(expectedToken);

		// Act
		LoginResponse result = authService.login(loginRequest);

		// Assert
		assertNotNull(result);
		assertEquals(expectedToken, result.getToken());
		assertEquals("john@example.com", result.getEmail());
		assertEquals("MEMBER", result.getRole());
		assertEquals("Login successful", result.getMessage());

		// Verify interactions
		verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
		verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), user.getPassword());
		verify(jwtUtil, times(1)).generateToken(user);
	}

	@Test
	void login_wrongPassword_throwsException() {
		// Arrange
		when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

		// Act & Assert
		InvalidCredentialsException exception = assertThrows(
			InvalidCredentialsException.class,
			() -> authService.login(loginRequest)
		);

		assertEquals("Invalid email or password", exception.getMessage());

		// Verify interactions
		verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
		verify(passwordEncoder, times(1)).matches(loginRequest.getPassword(), user.getPassword());
		verify(jwtUtil, never()).generateToken(any(User.class));
	}

	@Test
	void login_userNotFound_throwsException() {
		// Arrange
		when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

		// Act & Assert
		InvalidCredentialsException exception = assertThrows(
			InvalidCredentialsException.class,
			() -> authService.login(loginRequest)
		);

		assertEquals("Invalid email or password", exception.getMessage());

		// Verify interactions
		verify(userRepository, times(1)).findByEmail(loginRequest.getEmail());
		verify(passwordEncoder, never()).matches(anyString(), anyString());
		verify(jwtUtil, never()).generateToken(any(User.class));
	}

}

