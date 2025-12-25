package com.fitfamily.app.service;

import com.fitfamily.app.dto.FoodPortionResponse;
import com.fitfamily.app.dto.FoodResponse;
import com.fitfamily.app.model.Food;
import com.fitfamily.app.model.FoodPortion;
import com.fitfamily.app.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodService {

	private final FoodRepository foodRepository;

	public FoodService(FoodRepository foodRepository) {
		this.foodRepository = foodRepository;
	}

	public List<FoodResponse> searchFoods(String query) {
		List<Food> foods = foodRepository.findByNameContainingIgnoreCase(query);
		
		return foods.stream()
				.map(this::mapToFoodResponse)
				.collect(Collectors.toList());
	}

	private FoodResponse mapToFoodResponse(Food food) {
		List<FoodPortionResponse> portions = food.getFoodPortions().stream()
				.map(this::mapToFoodPortionResponse)
				.collect(Collectors.toList());
		
		return new FoodResponse(food.getId(), food.getName(), portions);
	}

	private FoodPortionResponse mapToFoodPortionResponse(FoodPortion foodPortion) {
		return new FoodPortionResponse(foodPortion.getId(), foodPortion.getLabel());
	}

}

