package com.fitfamily.app.controller;

import com.fitfamily.app.dto.AddFoodLogRequest;
import com.fitfamily.app.model.FoodLog;
import com.fitfamily.app.model.User;
import com.fitfamily.app.service.FoodLogService;
import com.fitfamily.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/food-logs")
public class FoodLogController {

	private final FoodLogService foodLogService;
	private final SecurityUtil securityUtil;

	public FoodLogController(FoodLogService foodLogService, SecurityUtil securityUtil) {
		this.foodLogService = foodLogService;
		this.securityUtil = securityUtil;
	}

	/**
	 * Add a food log entry
	 * 
	 * POST /food-logs
	 * 
	 * @param request AddFoodLogRequest containing foodId, portionId, and mealType
	 * @return Success response with food log details
	 */
	@PostMapping
	public ResponseEntity<FoodLogResponse> addFoodLog(@Valid @RequestBody AddFoodLogRequest request) {
		User currentUser = securityUtil.getCurrentUser();
		FoodLog foodLog = foodLogService.addFoodLog(request, currentUser);
		
		FoodLogResponse response = new FoodLogResponse(
			foodLog.getId().toString(),
			foodLog.getFood().getName(),
			foodLog.getPortion().getLabel(),
			foodLog.getMealType().toString(),
			foodLog.getCalories(),
			foodLog.getProtein(),
			foodLog.getCarbs(),
			foodLog.getFat(),
			"Food logged successfully!"
		);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Delete a food log entry
	 * 
	 * DELETE /food-logs/{id}
	 * 
	 * @param id The UUID of the food log to delete
	 * @return 204 No Content on successful deletion
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFoodLog(@PathVariable UUID id) {
		User currentUser = securityUtil.getCurrentUser();
		foodLogService.deleteFoodLog(id, currentUser);
		
		return ResponseEntity.noContent().build();
	}

	// Response DTO
	record FoodLogResponse(
		String id,
		String foodName,
		String portion,
		String mealType,
		double calories,
		double protein,
		double carbs,
		double fat,
		String message
	) {}

}

