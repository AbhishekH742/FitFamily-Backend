package com.fitfamily.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinFamilyRequest {

	@NotBlank(message = "Join code is required")
	@Pattern(regexp = "^FIT-[A-Z0-9]{4}$", message = "Invalid join code format. Expected format: FIT-XXXX")
	private String joinCode;

}

