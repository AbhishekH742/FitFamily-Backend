package com.fitfamily.app.service;

import com.fitfamily.app.exception.FamilyAlreadyExistsException;
import com.fitfamily.app.exception.InvalidJoinCodeException;
import com.fitfamily.app.model.Family;
import com.fitfamily.app.model.Role;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.FamilyRepository;
import com.fitfamily.app.repository.UserRepository;
import com.fitfamily.app.util.JoinCodeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

	@Mock
	private FamilyRepository familyRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private FamilyService familyService;

	private MockedStatic<JoinCodeGenerator> joinCodeGeneratorMock;

	private User user;
	private Family family;

	@BeforeEach
	void setUp() {
		// Setup mock for static JoinCodeGenerator
		joinCodeGeneratorMock = mockStatic(JoinCodeGenerator.class);

		// Setup user (not in any family)
		user = new User();
		user.setName("John Doe");
		user.setEmail("john@example.com");
		user.setPassword("hashedPassword");
		user.setRole(Role.MEMBER);
		user.setFamily(null); // User is not in any family

		// Setup family
		family = new Family();
		family.setId(UUID.randomUUID());
		family.setName("Doe Family");
		family.setJoinCode("FIT-A1B2");
	}

	@AfterEach
	void tearDown() {
		// Close the static mock to prevent memory leaks
		if (joinCodeGeneratorMock != null) {
			joinCodeGeneratorMock.close();
		}
	}

	@Test
	void createFamily_success() {
		// Arrange
		String familyName = "Doe Family";
		String generatedJoinCode = "FIT-A1B2";

		// Mock join code generation
		joinCodeGeneratorMock.when(JoinCodeGenerator::generateJoinCode).thenReturn(generatedJoinCode);

		// Mock repository to return empty (join code is unique)
		when(familyRepository.findByJoinCode(generatedJoinCode)).thenReturn(Optional.empty());

		// Mock family save
		when(familyRepository.save(any(Family.class))).thenReturn(family);

		// Mock user save
		when(userRepository.save(any(User.class))).thenReturn(user);

		// Act
		Family result = familyService.createFamily(familyName, user);

		// Assert
		assertNotNull(result);
		assertEquals("Doe Family", result.getName());
		assertEquals("FIT-A1B2", result.getJoinCode());
		assertEquals(Role.ADMIN, user.getRole()); // User should be ADMIN
		assertEquals(family, user.getFamily()); // User should be assigned to family

		// Verify interactions
		verify(familyRepository, times(1)).findByJoinCode(generatedJoinCode);
		verify(familyRepository, times(1)).save(any(Family.class));
		verify(userRepository, times(1)).save(user);
		joinCodeGeneratorMock.verify(JoinCodeGenerator::generateJoinCode, times(1));
	}

	@Test
	void createFamily_userAlreadyInFamily_throwsException() {
		// Arrange
		user.setFamily(family); // User is already in a family

		// Act & Assert
		FamilyAlreadyExistsException exception = assertThrows(
			FamilyAlreadyExistsException.class,
			() -> familyService.createFamily("New Family", user)
		);

		assertEquals(
			"You are already a member of a family. Please leave your current family before creating a new one.",
			exception.getMessage()
		);

		// Verify no repository interactions
		verify(familyRepository, never()).save(any(Family.class));
		verify(userRepository, never()).save(any(User.class));
		joinCodeGeneratorMock.verify(JoinCodeGenerator::generateJoinCode, never());
	}

	@Test
	void createFamily_generatesUniqueJoinCode() {
		// Arrange
		String firstCode = "FIT-A1B2";
		String secondCode = "FIT-C3D4";

		// Mock join code generation to return two different codes
		joinCodeGeneratorMock.when(JoinCodeGenerator::generateJoinCode)
			.thenReturn(firstCode, secondCode);

		// First code already exists, second code is unique
		when(familyRepository.findByJoinCode(firstCode)).thenReturn(Optional.of(family));
		when(familyRepository.findByJoinCode(secondCode)).thenReturn(Optional.empty());

		// Mock saves
		Family newFamily = new Family();
		newFamily.setName("New Family");
		newFamily.setJoinCode(secondCode);
		when(familyRepository.save(any(Family.class))).thenReturn(newFamily);
		when(userRepository.save(any(User.class))).thenReturn(user);

		// Act
		Family result = familyService.createFamily("New Family", user);

		// Assert
		assertNotNull(result);
		assertEquals("FIT-C3D4", result.getJoinCode()); // Should use the second, unique code

		// Verify join code generation was called twice
		joinCodeGeneratorMock.verify(JoinCodeGenerator::generateJoinCode, times(2));
		verify(familyRepository, times(1)).findByJoinCode(firstCode);
		verify(familyRepository, times(1)).findByJoinCode(secondCode);
	}

	@Test
	void joinFamily_success() {
		// Arrange
		String joinCode = "FIT-A1B2";

		// Mock repository to find family by join code
		when(familyRepository.findByJoinCode(joinCode)).thenReturn(Optional.of(family));

		// Mock user save
		User updatedUser = new User();
		updatedUser.setName(user.getName());
		updatedUser.setEmail(user.getEmail());
		updatedUser.setRole(Role.MEMBER);
		updatedUser.setFamily(family);
		when(userRepository.save(any(User.class))).thenReturn(updatedUser);

		// Act
		User result = familyService.joinFamily(joinCode, user);

		// Assert
		assertNotNull(result);
		assertEquals(family, user.getFamily()); // User should be assigned to family
		assertEquals(Role.MEMBER, user.getRole()); // User should be MEMBER (not ADMIN)

		// Verify interactions
		verify(familyRepository, times(1)).findByJoinCode(joinCode);
		verify(userRepository, times(1)).save(user);
	}

	@Test
	void joinFamily_userAlreadyInFamily_throwsException() {
		// Arrange
		user.setFamily(family); // User is already in a family
		String joinCode = "FIT-X9Y8";

		// Act & Assert
		FamilyAlreadyExistsException exception = assertThrows(
			FamilyAlreadyExistsException.class,
			() -> familyService.joinFamily(joinCode, user)
		);

		assertEquals(
			"You are already a member of a family. Please leave your current family before joining another one.",
			exception.getMessage()
		);

		// Verify no repository interactions
		verify(familyRepository, never()).findByJoinCode(anyString());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void joinFamily_invalidJoinCode_throwsException() {
		// Arrange
		String invalidJoinCode = "FIT-ZZZZ";

		// Mock repository to return empty (join code not found)
		when(familyRepository.findByJoinCode(invalidJoinCode)).thenReturn(Optional.empty());

		// Act & Assert
		InvalidJoinCodeException exception = assertThrows(
			InvalidJoinCodeException.class,
			() -> familyService.joinFamily(invalidJoinCode, user)
		);

		assertEquals("Invalid join code. Please check and try again.", exception.getMessage());

		// Verify interactions
		verify(familyRepository, times(1)).findByJoinCode(invalidJoinCode);
		verify(userRepository, never()).save(any(User.class));
	}

}

