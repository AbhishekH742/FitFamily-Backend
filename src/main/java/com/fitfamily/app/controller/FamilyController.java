package com.fitfamily.app.controller;

import com.fitfamily.app.dto.CreateFamilyRequest;
import com.fitfamily.app.dto.JoinFamilyRequest;
import com.fitfamily.app.model.Family;
import com.fitfamily.app.model.User;
import com.fitfamily.app.service.FamilyService;
import com.fitfamily.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/families")
public class FamilyController {

	private final FamilyService familyService;
	private final SecurityUtil securityUtil;

	public FamilyController(FamilyService familyService, SecurityUtil securityUtil) {
		this.familyService = familyService;
		this.securityUtil = securityUtil;
	}

	/**
	 * Create a new family
	 * 
	 * POST /families
	 * 
	 * @param request CreateFamilyRequest containing family name
	 * @return Created family with join code
	 */
	@PostMapping
	public ResponseEntity<FamilyResponse> createFamily(@Valid @RequestBody CreateFamilyRequest request) {
		User currentUser = securityUtil.getCurrentUser();
		Family family = familyService.createFamily(request.getName(), currentUser);
		
		FamilyResponse response = new FamilyResponse(
			family.getId().toString(),
			family.getName(),
			family.getJoinCode(),
			"Family created successfully! Share the join code with your family members."
		);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Join an existing family using join code
	 * 
	 * POST /families/join
	 * 
	 * @param request JoinFamilyRequest containing join code
	 * @return Success message with family details
	 */
	@PostMapping("/join")
	public ResponseEntity<JoinFamilyResponse> joinFamily(@Valid @RequestBody JoinFamilyRequest request) {
		User currentUser = securityUtil.getCurrentUser();
		User updatedUser = familyService.joinFamily(request.getJoinCode(), currentUser);
		
		JoinFamilyResponse response = new JoinFamilyResponse(
			updatedUser.getFamily().getId().toString(),
			updatedUser.getFamily().getName(),
			updatedUser.getRole().toString(),
			"Successfully joined the family!"
		);
		
		return ResponseEntity.ok(response);
	}

	/**
	 * Get current user's family details
	 * 
	 * GET /families/me
	 * 
	 * @return Current user's family or 404 if not in a family
	 */
	@GetMapping("/me")
	public ResponseEntity<MyFamilyResponse> getMyFamily() {
		User currentUser = securityUtil.getCurrentUser();
		
		if (currentUser.getFamily() == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		
		MyFamilyResponse response = new MyFamilyResponse(
			currentUser.getFamily().getId().toString(),
			currentUser.getFamily().getName(),
			currentUser.getFamily().getJoinCode(),
			currentUser.getRole().toString()
		);
		
		return ResponseEntity.ok(response);
	}

	// Response DTOs
	record FamilyResponse(String id, String name, String joinCode, String message) {}
	record JoinFamilyResponse(String familyId, String familyName, String role, String message) {}
	record MyFamilyResponse(String id, String name, String joinCode, String myRole) {}

}

