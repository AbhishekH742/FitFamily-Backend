package com.fitfamily.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitfamily.app.dto.CreateFamilyRequest;
import com.fitfamily.app.dto.JoinFamilyRequest;
import com.fitfamily.app.dto.LoginRequest;
import com.fitfamily.app.dto.RegisterRequest;
import com.fitfamily.app.repository.FamilyRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FamilyControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FamilyRepository familyRepository;

	private String user1Token;
	private String user2Token;

	@BeforeEach
	void setUp() throws Exception {
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

		// Register and login user 2
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
	}

	@AfterEach
	void cleanup() {
		// Clean up database after each test (delete in correct order to avoid FK constraint violations)
		userRepository.deleteAll();
		familyRepository.deleteAll();
	}

	@Test
	void createFamily_authenticatedUser_success() throws Exception {
		// Arrange
		CreateFamilyRequest request = new CreateFamilyRequest();
		request.setName("Doe Family");

		// Act & Assert
		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("Doe Family"))
			.andExpect(jsonPath("$.joinCode").exists())
			.andExpect(jsonPath("$.joinCode").value(matchesPattern("FIT-[A-Z0-9]{4}")))
			.andExpect(jsonPath("$.message").value(containsString("Family created successfully")));
	}

	@Test
	void createFamily_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Arrange
		CreateFamilyRequest request = new CreateFamilyRequest();
		request.setName("Doe Family");

		// Act & Assert - No Authorization header (Spring Security returns 403 Forbidden)
		mockMvc.perform(post("/families")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void createFamily_invalidToken_returnsUnauthorized() throws Exception {
		// Arrange
		CreateFamilyRequest request = new CreateFamilyRequest();
		request.setName("Doe Family");

		// Act & Assert - Invalid token (Spring Security returns 403 Forbidden)
		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer invalid.token.here")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	void createFamily_userAlreadyInFamily_returnsConflict() throws Exception {
		// Arrange - Create first family
		CreateFamilyRequest request1 = new CreateFamilyRequest();
		request1.setName("First Family");

		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1)))
			.andExpect(status().isCreated());

		// Arrange - Try to create second family with same user
		CreateFamilyRequest request2 = new CreateFamilyRequest();
		request2.setName("Second Family");

		// Act & Assert
		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error").value("Family Conflict"))
			.andExpect(jsonPath("$.message").value(containsString("already a member of a family")));
	}

	@Test
	void joinFamily_authenticatedUser_success() throws Exception {
		// Arrange - User 1 creates a family
		CreateFamilyRequest createRequest = new CreateFamilyRequest();
		createRequest.setName("Doe Family");

		MvcResult createResult = mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String joinCode = objectMapper.readTree(createResponse).get("joinCode").asText();

		// Arrange - User 2 joins the family
		JoinFamilyRequest joinRequest = new JoinFamilyRequest();
		joinRequest.setJoinCode(joinCode);

		// Act & Assert
		mockMvc.perform(post("/families/join")
				.header("Authorization", "Bearer " + user2Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(joinRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").exists())
			.andExpect(jsonPath("$.familyName").value("Doe Family"))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.message").value(containsString("Successfully joined the family")));
	}

	@Test
	void joinFamily_invalidJoinCode_returnsNotFound() throws Exception {
		// Arrange
		JoinFamilyRequest request = new JoinFamilyRequest();
		request.setJoinCode("FIT-XXXX"); // Non-existent join code

		// Act & Assert
		mockMvc.perform(post("/families/join")
				.header("Authorization", "Bearer " + user2Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error").value("Invalid Join Code"))
			.andExpect(jsonPath("$.message").value(containsString("Invalid join code")));
	}

	@Test
	void joinFamily_userAlreadyInFamily_returnsConflict() throws Exception {
		// Arrange - User 1 creates a family
		CreateFamilyRequest createRequest = new CreateFamilyRequest();
		createRequest.setName("First Family");

		MvcResult createResult = mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isCreated())
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String joinCode = objectMapper.readTree(createResponse).get("joinCode").asText();

		// Act & Assert - User 1 tries to join the same family
		JoinFamilyRequest joinRequest = new JoinFamilyRequest();
		joinRequest.setJoinCode(joinCode);

		mockMvc.perform(post("/families/join")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(joinRequest)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(containsString("already a member of a family")));
	}

	@Test
	void getMyFamily_success() throws Exception {
		// Arrange - User 1 creates a family
		CreateFamilyRequest createRequest = new CreateFamilyRequest();
		createRequest.setName("Doe Family");

		mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isCreated());

		// Act & Assert
		mockMvc.perform(get("/families/me")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.name").value("Doe Family"))
			.andExpect(jsonPath("$.joinCode").exists())
			.andExpect(jsonPath("$.myRole").value("ADMIN")); // Creator should be ADMIN
	}

	@Test
	void getMyFamily_userNotInFamily_returnsNotFound() throws Exception {
		// Act & Assert - User 2 is not in any family
		mockMvc.perform(get("/families/me")
				.header("Authorization", "Bearer " + user2Token))
			.andExpect(status().isNotFound());
	}

	@Test
	void getMyFamily_withoutAuthentication_returnsUnauthorized() throws Exception {
		// Act & Assert (Spring Security returns 403 Forbidden)
		mockMvc.perform(get("/families/me"))
			.andExpect(status().isForbidden());
	}

	@Test
	void fullFamilyFlow_createAndJoin_success() throws Exception {
		// Step 1: User 1 creates a family
		CreateFamilyRequest createRequest = new CreateFamilyRequest();
		createRequest.setName("Doe Family");

		MvcResult createResult = mockMvc.perform(post("/families")
				.header("Authorization", "Bearer " + user1Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(createRequest)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Doe Family"))
			.andReturn();

		String createResponse = createResult.getResponse().getContentAsString();
		String joinCode = objectMapper.readTree(createResponse).get("joinCode").asText();

		// Step 2: User 2 joins the family
		JoinFamilyRequest joinRequest = new JoinFamilyRequest();
		joinRequest.setJoinCode(joinCode);

		mockMvc.perform(post("/families/join")
				.header("Authorization", "Bearer " + user2Token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(joinRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyName").value("Doe Family"))
			.andExpect(jsonPath("$.role").value("MEMBER")); // Joiner should be MEMBER

		// Step 3: User 1 checks their family (should be ADMIN)
		mockMvc.perform(get("/families/me")
				.header("Authorization", "Bearer " + user1Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Doe Family"))
			.andExpect(jsonPath("$.myRole").value("ADMIN"));

		// Step 4: User 2 checks their family (should be MEMBER)
		mockMvc.perform(get("/families/me")
				.header("Authorization", "Bearer " + user2Token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Doe Family"))
			.andExpect(jsonPath("$.myRole").value("MEMBER"));
	}

}

