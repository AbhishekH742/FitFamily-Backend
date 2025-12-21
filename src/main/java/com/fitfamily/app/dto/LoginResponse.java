package com.fitfamily.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

	private String token;
	private String email;
	private String role;
	private String message;

	public LoginResponse(String token, String email, String role) {
		this.token = token;
		this.email = email;
		this.role = role;
		this.message = "Login successful";
	}

}

