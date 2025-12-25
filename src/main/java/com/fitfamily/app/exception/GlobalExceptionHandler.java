package com.fitfamily.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		ErrorResponse error = new ErrorResponse(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"Internal Server Error",
			"An unexpected error occurred"
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

}

