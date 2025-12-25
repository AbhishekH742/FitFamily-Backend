package com.fitfamily.app.service;

import com.fitfamily.app.dto.DailyMacroSummary;
import com.fitfamily.app.dto.FamilyMemberDashboardResponse;
import com.fitfamily.app.dto.FoodLogResponse;
import com.fitfamily.app.dto.UserDailyDashboardResponse;
import com.fitfamily.app.model.*;
import com.fitfamily.app.repository.FoodLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private FoodLogRepository foodLogRepository;

	@InjectMocks
	private DashboardService dashboardService;

	private User user;
	private Family family;
	private Food food1;
	private Food food2;
	private FoodPortion portion1;
	private FoodPortion portion2;
	private LocalDate testDate;

	@BeforeEach
	void setUp() {
		testDate = LocalDate.of(2025, 12, 25);

		// Setup family
		family = new Family();
		family.setId(UUID.randomUUID());
		family.setName("Doe Family");

		// Setup user
		user = new User();
		user.setId(UUID.randomUUID());
		user.setName("John Doe");
		user.setEmail("john@example.com");
		user.setFamily(family);

		// Setup foods
		food1 = new Food();
		food1.setId(UUID.randomUUID());
		food1.setName("Rice");

		food2 = new Food();
		food2.setId(UUID.randomUUID());
		food2.setName("Chicken");

		// Setup portions
		portion1 = new FoodPortion();
		portion1.setId(UUID.randomUUID());
		portion1.setLabel("1 cup");
		portion1.setFood(food1);

		portion2 = new FoodPortion();
		portion2.setId(UUID.randomUUID());
		portion2.setLabel("100g");
		portion2.setFood(food2);
	}

	@Test
	void getUserDailyDashboard_success_calculatesTotalsCorrectly() {
		// Arrange
		FoodLog log1 = createFoodLog(user, food1, portion1, 200.0, 5.0, 40.0, 2.0, MealType.BREAKFAST);
		FoodLog log2 = createFoodLog(user, food2, portion2, 165.0, 31.0, 0.0, 3.6, MealType.LUNCH);
		FoodLog log3 = createFoodLog(user, food1, portion1, 200.0, 5.0, 40.0, 2.0, MealType.DINNER);

		List<FoodLog> foodLogs = Arrays.asList(log1, log2, log3);
		when(foodLogRepository.findByUserAndDate(user, testDate)).thenReturn(foodLogs);

		// Act
		UserDailyDashboardResponse result = dashboardService.getUserDailyDashboard(user, testDate);

		// Assert
		assertNotNull(result);
		assertEquals(testDate, result.getDate());

		// Verify totals
		DailyMacroSummary summary = result.getSummary();
		assertEquals(565.0, summary.getCalories(), 0.01); // 200 + 165 + 200
		assertEquals(41.0, summary.getProtein(), 0.01);   // 5 + 31 + 5
		assertEquals(80.0, summary.getCarbs(), 0.01);     // 40 + 0 + 40
		assertEquals(7.6, summary.getFat(), 0.01);        // 2 + 3.6 + 2

		// Verify food logs
		List<FoodLogResponse> foodLogResponses = result.getFoodLogs();
		assertEquals(3, foodLogResponses.size());

		// Verify first food log
		assertEquals("Rice", foodLogResponses.get(0).getFoodName());
		assertEquals("1 cup", foodLogResponses.get(0).getPortionLabel());
		assertEquals(200.0, foodLogResponses.get(0).getCalories(), 0.01);
		assertEquals(MealType.BREAKFAST, foodLogResponses.get(0).getMealType());

		// Verify interactions
		verify(foodLogRepository, times(1)).findByUserAndDate(user, testDate);
	}

	@Test
	void getUserDailyDashboard_noLogs_returnsZeroSummary() {
		// Arrange
		when(foodLogRepository.findByUserAndDate(user, testDate)).thenReturn(new ArrayList<>());

		// Act
		UserDailyDashboardResponse result = dashboardService.getUserDailyDashboard(user, testDate);

		// Assert
		assertNotNull(result);
		assertEquals(testDate, result.getDate());

		// Verify all totals are zero
		DailyMacroSummary summary = result.getSummary();
		assertEquals(0.0, summary.getCalories(), 0.01);
		assertEquals(0.0, summary.getProtein(), 0.01);
		assertEquals(0.0, summary.getCarbs(), 0.01);
		assertEquals(0.0, summary.getFat(), 0.01);

		// Verify food logs list is empty
		assertTrue(result.getFoodLogs().isEmpty());
	}

	@Test
	void getFamilyDailyDashboard_success_groupsByUser() {
		// Arrange
		User user2 = new User();
		user2.setId(UUID.randomUUID());
		user2.setName("Jane Doe");
		user2.setEmail("jane@example.com");
		user2.setFamily(family);

		// User 1 logs
		FoodLog user1Log1 = createFoodLog(user, food1, portion1, 200.0, 5.0, 40.0, 2.0, MealType.BREAKFAST);
		FoodLog user1Log2 = createFoodLog(user, food2, portion2, 165.0, 31.0, 0.0, 3.6, MealType.LUNCH);

		// User 2 logs
		FoodLog user2Log1 = createFoodLog(user2, food1, portion1, 200.0, 5.0, 40.0, 2.0, MealType.BREAKFAST);

		List<FoodLog> allFamilyLogs = Arrays.asList(user1Log1, user1Log2, user2Log1);
		when(foodLogRepository.findByFamilyAndDate(family, testDate)).thenReturn(allFamilyLogs);

		// Act
		List<FamilyMemberDashboardResponse> result = dashboardService.getFamilyDailyDashboard(user, testDate);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.size()); // Two family members

		// Find John's dashboard
		FamilyMemberDashboardResponse johnDashboard = result.stream()
			.filter(d -> d.getUserName().equals("John Doe"))
			.findFirst()
			.orElse(null);

		assertNotNull(johnDashboard);
		assertEquals(365.0, johnDashboard.getDashboard().getSummary().getCalories(), 0.01); // 200 + 165
		assertEquals(2, johnDashboard.getDashboard().getFoodLogs().size());

		// Find Jane's dashboard
		FamilyMemberDashboardResponse janeDashboard = result.stream()
			.filter(d -> d.getUserName().equals("Jane Doe"))
			.findFirst()
			.orElse(null);

		assertNotNull(janeDashboard);
		assertEquals(200.0, janeDashboard.getDashboard().getSummary().getCalories(), 0.01);
		assertEquals(1, janeDashboard.getDashboard().getFoodLogs().size());

		// Verify interactions
		verify(foodLogRepository, times(1)).findByFamilyAndDate(family, testDate);
	}

	@Test
	void getFamilyDailyDashboard_userNotInFamily_returnsEmptyList() {
		// Arrange
		user.setFamily(null); // User not in any family

		// Act
		List<FamilyMemberDashboardResponse> result = dashboardService.getFamilyDailyDashboard(user, testDate);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());

		// Verify no repository call
		verify(foodLogRepository, never()).findByFamilyAndDate(any(), any());
	}

	@Test
	void getFamilyDailyDashboard_noLogsForDate_returnsEmptyList() {
		// Arrange
		when(foodLogRepository.findByFamilyAndDate(family, testDate)).thenReturn(new ArrayList<>());

		// Act
		List<FamilyMemberDashboardResponse> result = dashboardService.getFamilyDailyDashboard(user, testDate);

		// Assert
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getUserDailyDashboard_singleLog_calculatesCorrectly() {
		// Arrange
		FoodLog log = createFoodLog(user, food1, portion1, 100.5, 10.2, 20.3, 5.1, MealType.SNACK);
		when(foodLogRepository.findByUserAndDate(user, testDate)).thenReturn(Arrays.asList(log));

		// Act
		UserDailyDashboardResponse result = dashboardService.getUserDailyDashboard(user, testDate);

		// Assert
		DailyMacroSummary summary = result.getSummary();
		assertEquals(100.5, summary.getCalories(), 0.01);
		assertEquals(10.2, summary.getProtein(), 0.01);
		assertEquals(20.3, summary.getCarbs(), 0.01);
		assertEquals(5.1, summary.getFat(), 0.01);
	}

	// Helper method to create FoodLog
	private FoodLog createFoodLog(User user, Food food, FoodPortion portion,
	                               double calories, double protein, double carbs, double fat,
	                               MealType mealType) {
		FoodLog log = new FoodLog();
		log.setId(UUID.randomUUID());
		log.setUser(user);
		log.setFamily(user.getFamily());
		log.setFood(food);
		log.setPortion(portion);
		log.setCalories(calories);
		log.setProtein(protein);
		log.setCarbs(carbs);
		log.setFat(fat);
		log.setMealType(mealType);
		log.setDate(testDate);
		return log;
	}

}

