package com.fitfamily.app.service;

import com.fitfamily.app.dto.AddFoodLogRequest;
import com.fitfamily.app.exception.FoodNotFoundException;
import com.fitfamily.app.exception.FoodPortionNotFoundException;
import com.fitfamily.app.exception.InvalidFoodPortionException;
import com.fitfamily.app.model.*;
import com.fitfamily.app.repository.FoodLogRepository;
import com.fitfamily.app.repository.FoodPortionRepository;
import com.fitfamily.app.repository.FoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodLogServiceTest {

	@Mock
	private FoodRepository foodRepository;

	@Mock
	private FoodPortionRepository foodPortionRepository;

	@Mock
	private FoodLogRepository foodLogRepository;

	@InjectMocks
	private FoodLogService foodLogService;

	private AddFoodLogRequest request;
	private User user;
	private Food food;
	private FoodPortion portion;
	private FoodLog foodLog;

	@BeforeEach
	void setUp() {
		// Setup user
		user = new User();
		user.setId(UUID.randomUUID());
		user.setName("John Doe");
		user.setEmail("john@example.com");
		user.setRole(Role.MEMBER);
		user.setFamily(null);

		// Setup food with fixed test data
		food = new Food();
		food.setId(UUID.randomUUID());
		food.setName("Chicken Breast");
		food.setCaloriesPer100g(100.0);
		food.setProteinPer100g(20.0);
		food.setCarbsPer100g(30.0);
		food.setFatPer100g(10.0);

		// Setup portion with fixed test data (150g)
		portion = new FoodPortion();
		portion.setId(UUID.randomUUID());
		portion.setLabel("1 piece (150g)");
		portion.setGrams(150.0);
		portion.setFood(food);

		// Setup request
		request = new AddFoodLogRequest();
		request.setFoodId(food.getId());
		request.setPortionId(portion.getId());
		request.setMealType(MealType.LUNCH);

		// Setup food log
		foodLog = new FoodLog();
		foodLog.setId(UUID.randomUUID());
		foodLog.setUser(user);
		foodLog.setFamily(user.getFamily());
		foodLog.setFood(food);
		foodLog.setPortion(portion);
		foodLog.setMealType(MealType.LUNCH);
		foodLog.setDate(LocalDate.now());
	}

	@Test
	void addFoodLog_success_calculatesMacrosCorrectly() {
		// Arrange
		when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
		when(foodPortionRepository.findById(portion.getId())).thenReturn(Optional.of(portion));
		when(foodLogRepository.save(any(FoodLog.class))).thenReturn(foodLog);

		// Act
		FoodLog result = foodLogService.addFoodLog(request, user);

		// Assert - Verify result is not null
		assertNotNull(result);

		// Capture the FoodLog that was saved to verify calculated values
		ArgumentCaptor<FoodLog> foodLogCaptor = ArgumentCaptor.forClass(FoodLog.class);
		verify(foodLogRepository).save(foodLogCaptor.capture());
		FoodLog savedFoodLog = foodLogCaptor.getValue();

		// Assert calculated values
		// Formula: (portion.grams / 100) * food.per100g
		// Expected: (150 / 100) * 100 = 150 calories
		assertEquals(150.0, savedFoodLog.getCalories(), 0.01, "Calories should be 150");
		assertEquals(30.0, savedFoodLog.getProtein(), 0.01, "Protein should be 30");
		assertEquals(45.0, savedFoodLog.getCarbs(), 0.01, "Carbs should be 45");
		assertEquals(15.0, savedFoodLog.getFat(), 0.01, "Fat should be 15");

		// Assert other fields
		assertEquals(user, savedFoodLog.getUser());
		assertEquals(food, savedFoodLog.getFood());
		assertEquals(portion, savedFoodLog.getPortion());
		assertEquals(MealType.LUNCH, savedFoodLog.getMealType());
		assertEquals(LocalDate.now(), savedFoodLog.getDate());

		// Verify interactions
		verify(foodRepository, times(1)).findById(food.getId());
		verify(foodPortionRepository, times(1)).findById(portion.getId());
		verify(foodLogRepository, times(1)).save(any(FoodLog.class));
	}

	@Test
	void addFoodLog_portionDoesNotBelongToFood_throwsException() {
		// Arrange - Create a different food
		Food differentFood = new Food();
		differentFood.setId(UUID.randomUUID());
		differentFood.setName("Rice");

		// Portion belongs to 'differentFood', not the requested 'food'
		portion.setFood(differentFood);

		when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
		when(foodPortionRepository.findById(portion.getId())).thenReturn(Optional.of(portion));

		// Act & Assert
		InvalidFoodPortionException exception = assertThrows(
			InvalidFoodPortionException.class,
			() -> foodLogService.addFoodLog(request, user)
		);

		assertEquals("The selected portion does not belong to the selected food", exception.getMessage());

		// Verify repository interactions
		verify(foodRepository, times(1)).findById(food.getId());
		verify(foodPortionRepository, times(1)).findById(portion.getId());
		verify(foodLogRepository, never()).save(any(FoodLog.class));
	}

	@Test
	void addFoodLog_foodNotFound_throwsException() {
		// Arrange
		when(foodRepository.findById(food.getId())).thenReturn(Optional.empty());

		// Act & Assert
		FoodNotFoundException exception = assertThrows(
			FoodNotFoundException.class,
			() -> foodLogService.addFoodLog(request, user)
		);

		assertEquals("Food not found with ID: " + food.getId(), exception.getMessage());

		// Verify interactions
		verify(foodRepository, times(1)).findById(food.getId());
		verify(foodPortionRepository, never()).findById(any(UUID.class));
		verify(foodLogRepository, never()).save(any(FoodLog.class));
	}

	@Test
	void addFoodLog_portionNotFound_throwsException() {
		// Arrange
		when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
		when(foodPortionRepository.findById(portion.getId())).thenReturn(Optional.empty());

		// Act & Assert
		FoodPortionNotFoundException exception = assertThrows(
			FoodPortionNotFoundException.class,
			() -> foodLogService.addFoodLog(request, user)
		);

		assertEquals("Food portion not found with ID: " + portion.getId(), exception.getMessage());

		// Verify interactions
		verify(foodRepository, times(1)).findById(food.getId());
		verify(foodPortionRepository, times(1)).findById(portion.getId());
		verify(foodLogRepository, never()).save(any(FoodLog.class));
	}

	@Test
	void addFoodLog_withFamily_assignsFamilyCorrectly() {
		// Arrange - User with family
		Family family = new Family();
		family.setId(UUID.randomUUID());
		family.setName("Doe Family");
		user.setFamily(family);

		when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
		when(foodPortionRepository.findById(portion.getId())).thenReturn(Optional.of(portion));
		when(foodLogRepository.save(any(FoodLog.class))).thenReturn(foodLog);

		// Act
		foodLogService.addFoodLog(request, user);

		// Assert
		ArgumentCaptor<FoodLog> foodLogCaptor = ArgumentCaptor.forClass(FoodLog.class);
		verify(foodLogRepository).save(foodLogCaptor.capture());
		FoodLog savedFoodLog = foodLogCaptor.getValue();

		assertEquals(family, savedFoodLog.getFamily(), "Family should be assigned to food log");
	}

	@Test
	void addFoodLog_differentPortionSize_calculatesCorrectly() {
		// Arrange - Test with 100g portion
		portion.setGrams(100.0);
		portion.setLabel("100g");

		when(foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
		when(foodPortionRepository.findById(portion.getId())).thenReturn(Optional.of(portion));
		when(foodLogRepository.save(any(FoodLog.class))).thenReturn(foodLog);

		// Act
		foodLogService.addFoodLog(request, user);

		// Assert
		ArgumentCaptor<FoodLog> foodLogCaptor = ArgumentCaptor.forClass(FoodLog.class);
		verify(foodLogRepository).save(foodLogCaptor.capture());
		FoodLog savedFoodLog = foodLogCaptor.getValue();

		// Expected: (100 / 100) * 100 = 100 calories
		assertEquals(100.0, savedFoodLog.getCalories(), 0.01);
		assertEquals(20.0, savedFoodLog.getProtein(), 0.01);
		assertEquals(30.0, savedFoodLog.getCarbs(), 0.01);
		assertEquals(10.0, savedFoodLog.getFat(), 0.01);
	}

}

