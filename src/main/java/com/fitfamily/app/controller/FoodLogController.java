package com.fitfamily.app.controller;

import com.fitfamily.app.dto.AddFoodLogRequest;
import com.fitfamily.app.model.FoodLog;
import com.fitfamily.app.model.User;
import com.fitfamily.app.service.FoodLogService;
import com.fitfamily.app.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

