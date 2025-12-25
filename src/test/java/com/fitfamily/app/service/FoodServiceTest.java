package com.fitfamily.app.service;

import com.fitfamily.app.dto.FoodPortionResponse;
import com.fitfamily.app.dto.FoodResponse;
import com.fitfamily.app.model.Food;
import com.fitfamily.app.model.FoodPortion;
import com.fitfamily.app.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

	@Mock
	private FoodRepository foodRepository;

	@InjectMocks
	private FoodService foodService;

	private Food food1;
	private Food food2;
	private FoodPortion portion1;
	private FoodPortion portion2;
	private FoodPortion portion3;

	@BeforeEach
	void setUp() {
		// Setup food 1
		food1 = new Food();
		food1.setId(UUID.randomUUID());
		food1.setName("Chicken Breast");
		food1.setCaloriesPer100g(165.0);
		food1.setProteinPer100g(31.0);
		food1.setCarbsPer100g(0.0);
		food1.setFatPer100g(3.6);

		// Setup portions for food 1
		portion1 = new FoodPortion();
		portion1.setId(UUID.randomUUID());
		portion1.setLabel("100g");
		portion1.setGrams(100.0);
		portion1.setFood(food1);

		portion2 = new FoodPortion();
		portion2.setId(UUID.randomUUID());
		portion2.setLabel("1 piece (150g)");
		portion2.setGrams(150.0);
		portion2.setFood(food1);

		food1.setFoodPortions(Arrays.asList(portion1, portion2));

		// Setup food 2
		food2 = new Food();
		food2.setId(UUID.randomUUID());
		food2.setName("Rice");
		food2.setCaloriesPer100g(130.0);
		food2.setProteinPer100g(2.7);
		food2.setCarbsPer100g(28.2);
		food2.setFatPer100g(0.3);

		// Setup portions for food 2
		portion3 = new FoodPortion();
		portion3.setId(UUID.randomUUID());
		portion3.setLabel("1 cup (cooked)");
		portion3.setGrams(158.0);
		portion3.setFood(food2);

		food2.setFoodPortions(Arrays.asList(portion3));
	}

	@Test
	void searchFoods_success_returnsMatchingFoods() {
		// Arrange
		String query = "chicken";
		when(foodRepository.findByNameContainingIgnoreCase(query)).thenReturn(Arrays.asList(food1));

		// Act
		List<FoodResponse> result = foodService.searchFoods(query);

		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());

		// Verify first food
		FoodResponse foodResponse = result.get(0);
		assertEquals(food1.getId(), foodResponse.getId());
		assertEquals("Chicken Breast", foodResponse.getName());

		// Verify portions
		assertEquals(2, foodResponse.getPortions().size());

		FoodPortionResponse portionResponse1 = foodResponse.getPortions().get(0);
		assertEquals(portion1.getId(), portionResponse1.getId());
		assertEquals("100g", portionResponse1.getLabel());

		FoodPortionResponse portionResponse2 = foodResponse.getPortions().get(1);
		assertEquals(portion2.getId(), portionResponse2.getId());
		assertEquals("1 piece (150g)", portionResponse2.getLabel());

		// Verify interactions
		verify(foodRepository, times(1)).findByNameContainingIgnoreCase(query);
	}

	@Test
	void searchFoods_multipleResults_returnsAll() {
		// Arrange
		String query = "rice";
		when(foodRepository.findByNameContainingIgnoreCase(query)).thenReturn(Arrays.asList(food2));

		// Act
		List<FoodResponse> result = foodService.searchFoods(query);

		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());

		FoodResponse foodResponse = result.get(0);
		assertEquals("Rice", foodResponse.getName());
		assertEquals(1, foodResponse.getPortions().size());
		assertEquals("1 cup (cooked)", foodResponse.getPortions().get(0).getLabel());
	}

	@Test
	void searchFoods_noResults_returnsEmptyList() {
		// Arrange
		String query = "pizza";
		when(foodRepository.findByNameContainingIgnoreCase(query)).thenReturn(new ArrayList<>());

		// Act
		List<FoodResponse> result = foodService.searchFoods(query);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(foodRepository, times(1)).findByNameContainingIgnoreCase(query);
	}

	@Test
	void searchFoods_caseInsensitive_works() {
		// Arrange
		String query = "CHICKEN"; // uppercase query
		when(foodRepository.findByNameContainingIgnoreCase(query)).thenReturn(Arrays.asList(food1));

		// Act
		List<FoodResponse> result = foodService.searchFoods(query);

		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Chicken Breast", result.get(0).getName());

		verify(foodRepository, times(1)).findByNameContainingIgnoreCase(query);
	}

	@Test
	void searchFoods_foodWithNoPortions_returnsEmptyPortionsList() {
		// Arrange
		Food foodWithNoPortions = new Food();
		foodWithNoPortions.setId(UUID.randomUUID());
		foodWithNoPortions.setName("New Food");
		foodWithNoPortions.setFoodPortions(new ArrayList<>());

		when(foodRepository.findByNameContainingIgnoreCase("new")).thenReturn(Arrays.asList(foodWithNoPortions));

		// Act
		List<FoodResponse> result = foodService.searchFoods("new");

		// Assert
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("New Food", result.get(0).getName());
		assertTrue(result.get(0).getPortions().isEmpty());
	}

	@Test
	void searchFoods_multipleMatchingFoods_returnsAllMapped() {
		// Arrange
		when(foodRepository.findByNameContainingIgnoreCase("food"))
			.thenReturn(Arrays.asList(food1, food2));

		// Act
		List<FoodResponse> result = foodService.searchFoods("food");

		// Assert
		assertEquals(2, result.size());
		assertEquals("Chicken Breast", result.get(0).getName());
		assertEquals("Rice", result.get(1).getName());
	}

}

