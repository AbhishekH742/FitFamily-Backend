package com.fitfamily.app.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JoinCodeGeneratorTest {

	@Test
	void generateJoinCode_hasCorrectFormat() {
		// Act
		String joinCode = JoinCodeGenerator.generateJoinCode();

		// Assert
		assertNotNull(joinCode);
		assertTrue(joinCode.startsWith("FIT-"), "Join code should start with FIT-");
		assertEquals(8, joinCode.length(), "Join code should be 8 characters (FIT-XXXX)");
	}

	@Test
	void generateJoinCode_containsOnlyValidCharacters() {
		// Act
		String joinCode = JoinCodeGenerator.generateJoinCode();

		// Assert
		String suffix = joinCode.substring(4); // Get XXXX part
		assertTrue(suffix.matches("[A-Z0-9]{4}"), 
			"Code suffix should contain only uppercase letters and numbers");
	}

	@Test
	void generateJoinCode_generatesUniqueCodesMultipleTimes() {
		// Arrange
		Set<String> generatedCodes = new HashSet<>();
		int numberOfCodes = 1000;

		// Act
		for (int i = 0; i < numberOfCodes; i++) {
			String code = JoinCodeGenerator.generateJoinCode();
			generatedCodes.add(code);
		}

		// Assert
		// We should get close to 1000 unique codes (allowing for small possibility of collision)
		assertTrue(generatedCodes.size() > 990, 
			"Should generate mostly unique codes. Got " + generatedCodes.size() + " unique out of " + numberOfCodes);
	}

	@Test
	void generateJoinCode_followsExpectedPattern() {
		// Act
		for (int i = 0; i < 10; i++) {
			String code = JoinCodeGenerator.generateJoinCode();
			
			// Assert
			assertTrue(code.matches("FIT-[A-Z0-9]{4}"), 
				"Join code should match pattern FIT-XXXX where X is alphanumeric: " + code);
		}
	}

	@Test
	void generateJoinCode_isDifferentOnEachCall() {
		// Act
		String code1 = JoinCodeGenerator.generateJoinCode();
		String code2 = JoinCodeGenerator.generateJoinCode();
		String code3 = JoinCodeGenerator.generateJoinCode();

		// Assert
		// While there's a tiny chance of collision, it's extremely unlikely for 3 consecutive calls
		// With 36^4 possible combinations (1,679,616), collision is very rare
		assertNotEquals(code1, code2, "First two codes should be different");
		assertNotEquals(code2, code3, "Last two codes should be different");
		assertNotEquals(code1, code3, "First and last codes should be different");
	}

	@Test
	void constructor_throwsException() {
		// Act & Assert
		java.lang.reflect.InvocationTargetException exception = assertThrows(
			java.lang.reflect.InvocationTargetException.class,
			() -> {
				// Use reflection to try to instantiate
				java.lang.reflect.Constructor<JoinCodeGenerator> constructor = 
					JoinCodeGenerator.class.getDeclaredConstructor();
				constructor.setAccessible(true);
				constructor.newInstance();
			}
		);

		// Verify the cause is UnsupportedOperationException
		assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
		assertEquals("Utility class cannot be instantiated", exception.getCause().getMessage());
	}

}

