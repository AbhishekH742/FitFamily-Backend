package com.fitfamily.app.service;

import com.fitfamily.app.dto.AddFoodLogRequest;
import com.fitfamily.app.exception.FoodLogNotFoundException;
import com.fitfamily.app.exception.FoodNotFoundException;
import com.fitfamily.app.exception.FoodPortionNotFoundException;
import com.fitfamily.app.exception.InvalidFoodPortionException;
import com.fitfamily.app.model.Food;
import com.fitfamily.app.model.FoodLog;
import com.fitfamily.app.model.FoodPortion;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.FoodLogRepository;
import com.fitfamily.app.repository.FoodPortionRepository;
import com.fitfamily.app.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class FoodLogService {

	private final FoodRepository foodRepository;
	private final FoodPortionRepository foodPortionRepository;
	private final FoodLogRepository foodLogRepository;

	public FoodLogService(FoodRepository foodRepository, FoodPortionRepository foodPortionRepository, FoodLogRepository foodLogRepository) {
		this.foodRepository = foodRepository;
		this.foodPortionRepository = foodPortionRepository;
		this.foodLogRepository = foodLogRepository;
	}

	public FoodLog addFoodLog(AddFoodLogRequest request, User currentUser) {
		// Fetch Food by foodId
		Food food = foodRepository.findById(request.getFoodId())
				.orElseThrow(() -> new FoodNotFoundException("Food not found with ID: " + request.getFoodId()));

		// Fetch FoodPortion by portionId
		FoodPortion portion = foodPortionRepository.findById(request.getPortionId())
				.orElseThrow(() -> new FoodPortionNotFoundException("Food portion not found with ID: " + request.getPortionId()));

		// Validate portion belongs to food
		if (!portion.getFood().getId().equals(food.getId())) {
			throw new InvalidFoodPortionException("The selected portion does not belong to the selected food");
		}

		// Calculate calories and macros based on portion size
		double portionMultiplier = portion.getGrams() / 100.0;
		double calculatedCalories = food.getCaloriesPer100g() * portionMultiplier;
		double calculatedProtein = food.getProteinPer100g() * portionMultiplier;
		double calculatedCarbs = food.getCarbsPer100g() * portionMultiplier;
		double calculatedFat = food.getFatPer100g() * portionMultiplier;

		// Create FoodLog
		FoodLog foodLog = new FoodLog();
		foodLog.setUser(currentUser);
		foodLog.setFamily(currentUser.getFamily());
		foodLog.setFood(food);
		foodLog.setPortion(portion);
		foodLog.setCalories(calculatedCalories);
		foodLog.setProtein(calculatedProtein);
		foodLog.setCarbs(calculatedCarbs);
		foodLog.setFat(calculatedFat);
		foodLog.setMealType(request.getMealType());
		foodLog.setDate(LocalDate.now());

		// Save and return FoodLog
		return foodLogRepository.save(foodLog);
	}

	/**
	 * Delete a food log entry
	 * 
	 * @param foodLogId ID of the food log to delete
	 * @param currentUser The current authenticated user
	 * @throws FoodLogNotFoundException if food log not found or user is not the owner
	 */
	public void deleteFoodLog(UUID foodLogId, User currentUser) {
		// Find food log by ID and user (ensures only owner can delete)
		FoodLog foodLog = foodLogRepository.findByIdAndUser(foodLogId, currentUser)
				.orElseThrow(() -> new FoodLogNotFoundException("Food log not found with ID: " + foodLogId + " or you do not have permission to delete it"));

		// Delete the food log
		foodLogRepository.delete(foodLog);
	}

}

