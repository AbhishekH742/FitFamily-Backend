package com.fitfamily.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * 
 * Security Features:
 * - Prevents stack trace exposure in API responses
 * - Logs full exception details for debugging (server-side only)
 * - Environment-aware error messages (detailed in dev, minimal in prod)
 * - Assigns unique error IDs for tracking
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Value("${spring.profiles.active:dev}")
	private String activeProfile;

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			"Duplicate Email",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.UNAUTHORIZED.value(),
			"Authentication Failed",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(FamilyAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleFamilyAlreadyExists(FamilyAlreadyExistsException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.CONFLICT.value(),
			"Family Conflict",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(InvalidJoinCodeException.class)
	public ResponseEntity<ErrorResponse> handleInvalidJoinCode(InvalidJoinCodeException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"Invalid Join Code",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"User Not Found",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(FoodNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleFoodNotFound(FoodNotFoundException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"Food Not Found",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(FoodPortionNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleFoodPortionNotFound(FoodPortionNotFoundException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"Food Portion Not Found",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(InvalidFoodPortionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidFoodPortion(InvalidFoodPortionException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.BAD_REQUEST.value(),
			"Invalid Food Portion",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(FoodLogNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleFoodLogNotFound(FoodLogNotFoundException ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.NOT_FOUND.value(),
			"Food Log Not Found",
			ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
	}

	/**
	 * Handle authentication failures
	 * Security: Returns generic message, logs actual error server-side
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
		String errorId = UUID.randomUUID().toString();
		logger.error("Authentication failed [errorId={}]: {}", errorId, ex.getMessage(), ex);
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.UNAUTHORIZED.value(),
			"Authentication Failed",
			"Invalid credentials or authentication token"
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	/**
	 * Handle access denied (authorization failures)
	 * Security: Returns minimal information about why access was denied
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
		String errorId = UUID.randomUUID().toString();
		logger.warn("Access denied [errorId={}]: {}", errorId, ex.getMessage());
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.FORBIDDEN.value(),
			"Access Denied",
			"You do not have permission to access this resource"
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	/**
	 * Handle all unhandled exceptions
	 * Security: NEVER exposes stack traces or internal details to clients
	 * Logs full exception server-side with unique error ID for tracking
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		// Generate unique error ID for tracking
		String errorId = UUID.randomUUID().toString();
		
		// Log full exception details SERVER-SIDE ONLY (never sent to client)
		logger.error("Unhandled exception [errorId={}]: {}", errorId, ex.getMessage(), ex);
		
		// Determine error message based on environment
		String clientMessage = isProductionProfile() 
			? "An unexpected error occurred. Please contact support with error ID: " + errorId
			: "An unexpected error occurred: " + ex.getMessage() + " [errorId=" + errorId + "]";
		
		ErrorResponse error = new ErrorResponse(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"Internal Server Error",
			clientMessage
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
	
	/**
	 * Check if running in production profile
	 * @return true if production, false otherwise
	 */
	private boolean isProductionProfile() {
		return "prod".equalsIgnoreCase(activeProfile);
	}

}

