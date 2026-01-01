package com.fitfamily.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfamily.app.dto.AddFoodLogRequest;
import com.fitfamily.app.dto.CreateFamilyRequest;
import com.fitfamily.app.dto.LoginRequest;
import com.fitfamily.app.dto.RegisterRequest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FoodLogControllerIT {

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

	private String user1Token;
	private String user2Token;
	private UUID riceId;
	private UUID riceCupPortionId;
	private UUID chickenId;
	private UUID chicken100gPortionId;

	@BeforeEach
	void setUp() throws Exception {
		// Note: DataSeeder automatically runs and preloads food data

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

		// Register and login user 1
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

		// Create family for user 1 (optional - family can be null)
		CreateFamilyRequest familyRequest = new CreateFamilyRequest();
		familyRequest.setName("Doe Family");

		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(familyRequest)))
			.andExpect(status().isCreated());

		// Register and login user 2
		RegisterRequest user2Register = new RegisterRequest();
		user2Register.setName("Jane Smith");
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
	}

	@AfterEach
	void cleanup() {
		// Clean up in proper order due to foreign key constraints
		foodLogRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void addFoodLog_success() throws Exception {
		// Arrange
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.BREAKFAST);

		// Act & Assert
		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.foodName").value("Rice"))
			.andExpect(jsonPath("$.portion").value(containsString("cup")))
			.andExpect(jsonPath("$.mealType").value("BREAKFAST"))
			.andExpect(jsonPath("$.calories").exists())
			.andExpect(jsonPath("$.calories").isNumber())
			.andExpect(jsonPath("$.protein").exists())
			.andExpect(jsonPath("$.carbs").exists())
			.andExpect(jsonPath("$.fat").exists())
			.andExpect(jsonPath("$.message").value("Food logged successfully!"));
	}

	@Test
	void addFoodLog_differentMealTypes_success() throws Exception {
		// Test LUNCH
		AddFoodLogRequest lunchRequest = new AddFoodLogRequest();
		lunchRequest.setFoodId(chickenId);
		lunchRequest.setPortionId(chicken100gPortionId);
		lunchRequest.setMealType(MealType.LUNCH);

		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(lunchRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.mealType").value("LUNCH"));

		// Test SNACK
		AddFoodLogRequest snackRequest = new AddFoodLogRequest();
		snackRequest.setFoodId(riceId);
		snackRequest.setPortionId(riceCupPortionId);
		snackRequest.setMealType(MealType.SNACK);

		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(snackRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.mealType").value("SNACK"));
	}

	@Test
	void addFoodLog_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Arrange
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.BREAKFAST);

		// Act & Assert (Spring Security returns 403 Forbidden)
		mockMvc.perform(post("/food-logs")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void addFoodLog_invalidFoodId_returnsNotFound() throws Exception {
		// Arrange
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(UUID.randomUUID()); // Non-existent food
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.BREAKFAST);

		// Act & Assert
		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Food Not Found"));
	}

	@Test
	void addFoodLog_portionDoesNotBelongToFood_returnsBadRequest() throws Exception {
		// Arrange - Use rice food with chicken portion (mismatch)
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(chicken100gPortionId); // Chicken portion!
		request.setMealType(MealType.BREAKFAST);

		// Act & Assert
		mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value("Invalid Food Portion"))
			.andExpect(jsonPath("$.message").value(containsString("does not belong to the selected food")));
	}

	@Test
	void deleteFoodLog_success() throws Exception {
		// Arrange - First create a food log
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.DINNER);

		MvcResult createResult = mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String foodLogId = objectMapper.readTree(createResponse).get("id").asText();

		// Act & Assert - Delete the food log
		mockMvc.perform(delete("/food-logs/" + foodLogId)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isNoContent());

		// Verify deletion - Trying to delete again should fail
		mockMvc.perform(delete("/food-logs/" + foodLogId)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isNotFound());
	}

	@Test
	void deleteFoodLog_notOwner_returnsNotFound() throws Exception {
		// Arrange - User 1 creates a food log
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.DINNER);

		MvcResult createResult = mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String foodLogId = objectMapper.readTree(createResponse).get("id").asText();

		// Act & Assert - User 2 tries to delete User 1's food log
		mockMvc.perform(delete("/food-logs/" + foodLogId)
				.header("Authorization", "Bearer " + user2Token))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Food Log Not Found"))
			.andExpect(jsonPath("$.message").value(containsString("do not have permission")));
	}

	@Test
	void deleteFoodLog_nonExistentId_returnsNotFound() throws Exception {
		// Arrange
		UUID nonExistentId = UUID.randomUUID();

		// Act & Assert
		mockMvc.perform(delete("/food-logs/" + nonExistentId)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Food Log Not Found"));
	}

	@Test
	void deleteFoodLog_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Arrange - Create a food log first
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.DINNER);

		MvcResult createResult = mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String foodLogId = objectMapper.readTree(createResponse).get("id").asText();

		// Act & Assert - Try to delete without authentication (Spring Security returns 403 Forbidden)
		mockMvc.perform(delete("/food-logs/" + foodLogId))
			.andExpect(status().isForbidden());
	}

	@Test
	void addFoodLog_calculatesNutritionCorrectly() throws Exception {
		// Arrange
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(chickenId);
		request.setPortionId(chicken100gPortionId); // 100g portion
		request.setMealType(MealType.LUNCH);

		// Act
		MvcResult result = mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andReturn();

		// Assert - Verify nutrition values are calculated
		String response = result.getResponse().getContentAsString();
		double calories = objectMapper.readTree(response).get("calories").asDouble();
		double protein = objectMapper.readTree(response).get("protein").asDouble();

		// Chicken breast 100g should have approximately 165 calories and 31g protein
		org.junit.jupiter.api.Assertions.assertTrue(calories > 100, "Calories should be calculated");
		org.junit.jupiter.api.Assertions.assertTrue(protein > 20, "Protein should be calculated");
	}

	@Test
	void fullFoodLogFlow_addAndDelete_success() throws Exception {
		// Step 1: Add food log
		AddFoodLogRequest request = new AddFoodLogRequest();
		request.setFoodId(riceId);
		request.setPortionId(riceCupPortionId);
		request.setMealType(MealType.BREAKFAST);

		MvcResult addResult = mockMvc.perform(post("/food-logs")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.foodName").value("Rice"))
			.andReturn();

		String addResponse = addResult.getResponse().getContentAsString();
		String foodLogId = objectMapper.readTree(addResponse).get("id").asText();

		// Step 2: Delete food log
		mockMvc.perform(delete("/food-logs/" + foodLogId)
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isNoContent());
	}

}

