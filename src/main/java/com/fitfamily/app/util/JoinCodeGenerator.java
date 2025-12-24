package com.fitfamily.app.util;

import java.security.SecureRandom;

public class JoinCodeGenerator {

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int CODE_LENGTH = 4;
	private static final String PREFIX = "FIT-";
	private static final SecureRandom random = new SecureRandom();

	/**
	 * Generates a random join code in the format FIT-XXXX
	 * where XXXX is 4 random alphanumeric uppercase characters
	 * 
	 * @return A random join code (e.g., FIT-A1B2)
	 */
	public static String generateJoinCode() {
		StringBuilder code = new StringBuilder(PREFIX);
		
		for (int i = 0; i < CODE_LENGTH; i++) {
			int index = random.nextInt(CHARACTERS.length());
			code.append(CHARACTERS.charAt(index));
		}
		
		return code.toString();
	}

	// Private constructor to prevent instantiation
	private JoinCodeGenerator() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

}

