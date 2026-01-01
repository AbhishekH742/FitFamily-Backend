package com.fitfamily.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfamily.app.dto.*;
import com.fitfamily.app.model.MealType;
import com.fitfamily.app.repository.FoodLogRepository;
import com.fitfamily.app.repository.FoodPortionRepository;
import com.fitfamily.app.repository.FoodRepository;
import com.fitfamily.app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FoodRepository foodRepository;

	@Autowired
	private FoodPortionRepository foodPortionRepository;

	@Autowired
	private FoodLogRepository foodLogRepository;

	private String user1Token; // John - Family ADMIN
	private String user2Token; // Jane - Family MEMBER
	private UUID riceId;
	private UUID riceCupPortionId;
	private UUID chickenId;
	private UUID chicken100gPortionId;
	private String familyJoinCode;

	@BeforeEach
	void setUp() throws Exception {
		// Get seeded food IDs from database
		var riceFood = foodRepository.findByNameContainingIgnoreCase("Rice").stream().findFirst().orElseThrow();
		riceId = riceFood.getId();
		riceCupPortionId = foodPortionRepository.findByFood(riceFood).stream()
			.filter(p -> p.getLabel().contains("cup"))
			.findFirst()
			.orElseThrow()
			.getId();

		var chickenFood = foodRepository.findByNameContainingIgnoreCase("Chicken").stream().findFirst().orElseThrow();
		chickenId = chickenFood.getId();
		chicken100gPortionId = foodPortionRepository.findByFood(chickenFood).stream()
			.filter(p -> p.getLabel().equals("100g"))
			.findFirst()
			.orElseThrow()
			.getId();

		// Register and login user 1 (John)
		RegisterRequest user1Register = new RegisterRequest();
		user1Register.setName("John Doe");
		user1Register.setEmail("john@example.com");
		user1Register.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user1Register)))
			.andExpect(status().isCreated());

		LoginRequest user1Login = new LoginRequest();
		user1Login.setEmail("john@example.com");
		user1Login.setPassword("password123");

		MvcResult user1Result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user1Login)))
			.andExpect(status().isOk())
			.andReturn();

		String user1Response = user1Result.getResponse().getContentAsString();
		user1Token = objectMapper.readTree(user1Response).get("token").asText();

		// User 1 creates a family
		CreateFamilyRequest familyRequest = new CreateFamilyRequest();
		familyRequest.setName("Doe Family");

		MvcResult familyResult = mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(familyRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		String familyResponse = familyResult.getResponse().getContentAsString();
		familyJoinCode = objectMapper.readTree(familyResponse).get("joinCode").asText();

		// Register and login user 2 (Jane)
		RegisterRequest user2Register = new RegisterRequest();
		user2Register.setName("Jane Doe");
		user2Register.setEmail("jane@example.com");
		user2Register.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user2Register)))
			.andExpect(status().isCreated());

		LoginRequest user2Login = new LoginRequest();
		user2Login.setEmail("jane@example.com");
		user2Login.setPassword("password123");

		MvcResult user2Result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user2Login)))
			.andExpect(status().isOk())
			.andReturn();

		String user2Response = user2Result.getResponse().getContentAsString();
		user2Token = objectMapper.readTree(user2Response).get("token").asText();

		// User 2 joins the family
		JoinFamilyRequest joinRequest = new JoinFamilyRequest();
		joinRequest.setJoinCode(familyJoinCode);

		mockMvc.perform(post("/families/join")
				.header("Authorization", "Bearer " + user2Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(joinRequest)))
			.andExpect(status().isOk());

		// User 1 adds food logs
		addFoodLog(user1Token, riceId, riceCupPortionId, MealType.BREAKFAST);
		addFoodLog(user1Token, chickenId, chicken100gPortionId, MealType.LUNCH);

		// User 2 adds food logs
		addFoodLog(user2Token, riceId, riceCupPortionId, MealType.BREAKFAST);
		addFoodLog(user2Token, chickenId, chicken100gPortionId, MealType.DINNER);
	}

	@AfterEach
	void cleanup() {
		foodLogRepository.deleteAll();
		userRepository.deleteAll();
	}

	private void addFoodLog(String token, UUID foodId, UUID portionId, MealType mealType) throws Exception {
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(foodId);
		request.setPortionId(portionId);
		request.setMealType(mealType);

		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated());
	}

	@Test
	void getUserDailyDashboard_success() throws Exception {
		// Act & Assert
		MvcResult result = mockMvc.perform(get("/dashboard/daily")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.date").exists())
			.andExpect(jsonPath("$.date").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$.summary").exists())
			.andExpect(jsonPath("$.summary.calories").exists())
			.andExpect(jsonPath("$.summary.calories").isNumber())
			.andExpect(jsonPath("$.summary.protein").exists())
			.andExpect(jsonPath("$.summary.carbs").exists())
			.andExpect(jsonPath("$.summary.fat").exists())
			.andExpect(jsonPath("$.foodLogs").isArray())
			.andExpect(jsonPath("$.foodLogs", hasSize(2))) // User 1 has 2 food logs
			.andReturn();

		// Verify total calories is greater than 0 (sum of 2 meals)
		String response = result.getResponse().getContentAsString();
		double totalCalories = objectMapper.readTree(response).get("summary").get("calories").asDouble();
		org.junit.jupiter.api.Assertions.assertTrue(totalCalories > 300, 
			"Total calories should be sum of 2 meals (Rice + Chicken)");
	}

	@Test
	void getUserDailyDashboard_verifyFoodLogsDetails() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/dashboard/daily")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.foodLogs[0].foodName").exists())
			.andExpect(jsonPath("$.foodLogs[0].portionLabel").exists())
			.andExpect(jsonPath("$.foodLogs[0].calories").exists())
			.andExpect(jsonPath("$.foodLogs[0].mealType").exists())
			// Verify meal types
			.andExpect(jsonPath("$.foodLogs[*].mealType", hasItems("BREAKFAST", "LUNCH")))
			// Verify food names
			.andExpect(jsonPath("$.foodLogs[*].foodName", hasItems("Rice", "Chicken Breast")));
	}

	@Test
	void getUserDailyDashboard_withSpecificDate_success() throws Exception {
		// Act & Assert - Query with today's date explicitly
		String today = LocalDate.now().toString();

		mockMvc.perform(get("/dashboard/daily")
				.param("date", today)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.date").value(today))
			.andExpect(jsonPath("$.foodLogs", hasSize(2)));
	}

	@Test
	void getUserDailyDashboard_differentDate_returnsEmpty() throws Exception {
		// Act & Assert - Query with a different date (no logs)
		String yesterday = LocalDate.now().minusDays(1).toString();

		mockMvc.perform(get("/dashboard/daily")
				.param("date", yesterday)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.date").value(yesterday))
			.andExpect(jsonPath("$.foodLogs", hasSize(0)))
			.andExpect(jsonPath("$.summary.calories").value(0.0))
			.andExpect(jsonPath("$.summary.protein").value(0.0));
	}

	@Test
	void getUserDailyDashboard_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Act & Assert (Spring Security returns 403 Forbidden)
		mockMvc.perform(get("/dashboard/daily"))
			.andExpect(status().isForbidden());
	}

	@Test
	void getFamilyDailyDashboard_success() throws Exception {
		// Act & Assert
		MvcResult result = mockMvc.perform(get("/dashboard/family")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", hasSize(2))) // 2 family members
			.andExpect(jsonPath("$[0].userName").exists())
			.andExpect(jsonPath("$[0].dashboard").exists())
			.andExpect(jsonPath("$[0].dashboard.date").value(LocalDate.now().toString()))
			.andExpect(jsonPath("$[0].dashboard.summary").exists())
			.andExpect(jsonPath("$[0].dashboard.foodLogs").isArray())
			.andReturn();

		// Verify both family members are present
		String response = result.getResponse().getContentAsString();
		String json = response;
		org.junit.jupiter.api.Assertions.assertTrue(json.contains("John Doe"), "Should include John Doe");
		org.junit.jupiter.api.Assertions.assertTrue(json.contains("Jane Doe"), "Should include Jane Doe");
	}

	@Test
	void getFamilyDailyDashboard_verifyIndividualTotals() throws Exception {
		// Act & Assert - Each family member should have their own totals
		// Both users logged 2 meals each, so both should have calories > 300
		mockMvc.perform(get("/dashboard/family")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].dashboard.summary.calories").exists())
			.andExpect(jsonPath("$[*].dashboard.foodLogs", everyItem(hasSize(2))));
	}

	@Test
	void getFamilyDailyDashboard_withSpecificDate_success() throws Exception {
		// Act & Assert
		String today = LocalDate.now().toString();

		mockMvc.perform(get("/dashboard/family")
				.param("date", today)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].dashboard.date").value(today));
	}

	@Test
	void getFamilyDailyDashboard_differentDate_returnsEmpty() throws Exception {
		// Act & Assert - Query with a different date (no logs)
		String yesterday = LocalDate.now().minusDays(1).toString();

		mockMvc.perform(get("/dashboard/family")
				.param("date", yesterday)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", hasSize(0))); // No logs for that date
	}

	@Test
	void getFamilyDailyDashboard_user2CanSeeAllMembers() throws Exception {
		// Act & Assert - User 2 (MEMBER role) can also see family dashboard
		mockMvc.perform(get("/dashboard/family")
				.header("Authorization", "Bearer " + user2Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].userName", hasItems("John Doe", "Jane Doe")));
	}

	@Test
	void getFamilyDailyDashboard_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Act & Assert (Spring Security returns 403 Forbidden)
		mockMvc.perform(get("/dashboard/family"))
			.andExpect(status().isForbidden());
	}

	@Test
	void getUserDailyDashboard_verifySummaryCalculation() throws Exception {
		// Act
		MvcResult result = mockMvc.perform(get("/dashboard/daily")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andReturn();

		// Assert - Verify summary contains sum of all macros
		String response = result.getResponse().getContentAsString();
		double calories = objectMapper.readTree(response).get("summary").get("calories").asDouble();
		double protein = objectMapper.readTree(response).get("summary").get("protein").asDouble();
		double carbs = objectMapper.readTree(response).get("summary").get("carbs").asDouble();
		double fat = objectMapper.readTree(response).get("summary").get("fat").asDouble();

		// Verify all values are positive (user has 2 meals)
		org.junit.jupiter.api.Assertions.assertTrue(calories > 0, "Calories should be calculated");
		org.junit.jupiter.api.Assertions.assertTrue(protein > 0, "Protein should be calculated");
		org.junit.jupiter.api.Assertions.assertTrue(carbs > 0, "Carbs should be calculated");
		org.junit.jupiter.api.Assertions.assertTrue(fat > 0, "Fat should be calculated");

		// Verify food logs count matches
		int foodLogsCount = objectMapper.readTree(response).get("foodLogs").size();
		org.junit.jupiter.api.Assertions.assertEquals(2, foodLogsCount, "Should have 2 food logs");
	}

	@Test
	void fullDashboardFlow_addLogsAndView_success() throws Exception {
		// Already setup with 2 users, each with 2 food logs in same family

		// Step 1: User 1 views their dashboard
		mockMvc.perform(get("/dashboard/daily")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.foodLogs", hasSize(2)));

		// Step 2: User 2 views their dashboard
		mockMvc.perform(get("/dashboard/daily")
				.header("Authorization", "Bearer " + user2Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.foodLogs", hasSize(2)));

		// Step 3: View family dashboard (shows both users)
		mockMvc.perform(get("/dashboard/family")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].userName", hasItems("John Doe", "Jane Doe")));
	}

}

