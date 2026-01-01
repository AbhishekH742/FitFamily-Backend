package com.fitfamily.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfamily.app.dto.LoginRequest;
import com.fitfamily.app.dto.RegisterRequest;
import com.fitfamily.app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FoodControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	private String jwtToken;

	@BeforeEach
	void setUp() throws Exception {
		// Note: DataSeeder automatically runs and preloads food data

		// Register and login user to get JWT token
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setName("Test User");
		registerRequest.setEmail("test@example.com");
		registerRequest.setPassword("password123");

		mockMvc.perform(post("/auth/register")
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(registerRequest)))
			.andExpect(status().isCreated());

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setEmail("test@example.com");
		loginRequest.setPassword("password123");

		MvcResult loginResult = mockMvc.perform(post("/auth/login")
				.contentType("application/json")
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andExpect(status().isOk())
			.andReturn();

		String loginResponse = loginResult.getResponse().getContentAsString();
		jwtToken = objectMapper.readTree(loginResponse).get("token").asText();
	}

	@AfterEach
	void cleanup() {
		// Clean up users (food data is reused across tests)
		userRepository.deleteAll();
	}

	@Test
	void searchFoods_rice_returnsRiceWithPortions() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/foods/search")
				.param("query", "rice")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].id").exists())
			.andExpect(jsonPath("$[0].name").value("Rice"))
			.andExpect(jsonPath("$[0].portions").isArray())
			.andExpect(jsonPath("$[0].portions", hasSize(greaterThanOrEqualTo(3))))
			.andExpect(jsonPath("$[0].portions[0].id").exists())
			.andExpect(jsonPath("$[0].portions[0].label").exists())
			// Verify specific portion labels from DataSeeder
			.andExpect(jsonPath("$[0].portions[*].label", 
				hasItems("100g", "1 cup (cooked)", "1 bowl", "1 serving")));
	}

	@Test
	void searchFoods_chicken_returnsChickenWithPortions() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/foods/search")
				.param("query", "chicken")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].id").exists())
			.andExpect(jsonPath("$[0].name").value("Chicken Breast"))
			.andExpect(jsonPath("$[0].portions").isArray())
			.andExpect(jsonPath("$[0].portions", hasSize(greaterThanOrEqualTo(3))))
			.andExpect(jsonPath("$[0].portions[*].label", 
				hasItems("100g", "1 piece (150g)", "1 serving (200g)")));
	}

	@Test
	void searchFoods_chapati_returnsChapatigWithPortions() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/foods/search")
				.param("query", "chapati")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].id").exists())
			.andExpect(jsonPath("$[0].name").value("Chapati"))
			.andExpect(jsonPath("$[0].portions").isArray())
			.andExpect(jsonPath("$[0].portions", hasSize(3)))
			.andExpect(jsonPath("$[0].portions[*].label", 
				hasItems("1 small (40g)", "1 medium (50g)", "1 large (60g)")));
	}

	@Test
	void searchFoods_caseInsensitive_works() throws Exception {
		// Act & Assert - Search with uppercase
		mockMvc.perform(get("/foods/search")
				.param("query", "RICE")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].name").value("Rice"));
	}

	@Test
	void searchFoods_partialMatch_works() throws Exception {
		// Act & Assert - Partial search
		mockMvc.perform(get("/foods/search")
				.param("query", "chick")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].name").value("Chicken Breast"));
	}

	@Test
	void searchFoods_noMatch_returnsEmptyArray() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/foods/search")
				.param("query", "pizza")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void searchFoods_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Act & Assert - No Authorization header (Spring Security returns 403 Forbidden)
		mockMvc.perform(get("/foods/search")
				.param("query", "rice"))
			.andExpect(status().isForbidden());
	}

	@Test
	void searchFoods_invalidToken_returnsUnauthorized() throws Exception {
		// Act & Assert - Invalid token (Spring Security returns 403 Forbidden)
		mockMvc.perform(get("/foods/search")
				.param("query", "rice")
				.header("Authorization", "Bearer invalid.token.here"))
			.andExpect(status().isForbidden());
	}

	@Test
	void searchFoods_emptyQuery_returnsAllFoods() throws Exception {
		// Act & Assert
		mockMvc.perform(get("/foods/search")
				.param("query", "")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
	}

	@Test
	void searchFoods_verifyPortionStructure() throws Exception {
		// Act & Assert - Verify complete structure
		mockMvc.perform(get("/foods/search")
				.param("query", "rice")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]").exists())
			.andExpect(jsonPath("$[0].id").isString())
			.andExpect(jsonPath("$[0].name").isString())
			.andExpect(jsonPath("$[0].portions").isArray())
			.andExpect(jsonPath("$[0].portions[0].id").isString())
			.andExpect(jsonPath("$[0].portions[0].label").isString())
			// Verify no extra fields are exposed (security)
			.andExpect(jsonPath("$[0].caloriesPer100g").doesNotExist())
			.andExpect(jsonPath("$[0].proteinPer100g").doesNotExist());
	}

	@Test
	void searchFoods_verifyAllSeededFoods() throws Exception {
		// Act & Assert - Verify all 3 seeded foods are searchable
		mockMvc.perform(get("/foods/search")
				.param("query", "")
				.header("Authorization", "Bearer " + jwtToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[*].name", hasItems("Rice", "Chapati", "Chicken Breast")));
	}

}

