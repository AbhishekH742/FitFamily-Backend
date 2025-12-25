package com.fitfamily.app.controller;

import com.fitfamily.app.dto.FoodResponse;
import com.fitfamily.app.service.FoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/foods")
public class FoodController {

	private final FoodService foodService;

	public FoodController(FoodService foodService) {
		this.foodService = foodService;
	}

	/**
	 * Search for foods by name
	 * 
	 * GET /foods/search?query=chicken
	 * 
	 * @param query Search query string
	 * @return List of matching foods with portions
	 */
	@GetMapping("/search")
	public ResponseEntity<List<FoodResponse>> searchFoods(@RequestParam String query) {
		List<FoodResponse> foods = foodService.searchFoods(query);
		return ResponseEntity.ok(foods);
	}

}

