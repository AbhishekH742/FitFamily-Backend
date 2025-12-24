package com.fitfamily.app.service;

import com.fitfamily.app.exception.FamilyAlreadyExistsException;
import com.fitfamily.app.exception.InvalidJoinCodeException;
import com.fitfamily.app.model.Family;
import com.fitfamily.app.model.Role;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.FamilyRepository;
import com.fitfamily.app.repository.UserRepository;
import com.fitfamily.app.util.JoinCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FamilyService {

	private final FamilyRepository familyRepository;
	private final UserRepository userRepository;

	public FamilyService(FamilyRepository familyRepository, UserRepository userRepository) {
		this.familyRepository = familyRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Creates a new family and assigns the current user as ADMIN
	 * 
	 * @param familyName Name of the family
	 * @param currentUser User creating the family
	 * @return Created family
	 * @throws FamilyAlreadyExistsException if user already belongs to a family
	 */
	@Transactional
	public Family createFamily(String familyName, User currentUser) {
		// Ensure user does not already belong to a family
		if (currentUser.getFamily() != null) {
			throw new FamilyAlreadyExistsException("You are already a member of a family. Please leave your current family before creating a new one.");
		}

		// Generate unique join code
		String joinCode = generateUniqueJoinCode();

		// Create new family
		Family family = new Family();
		family.setName(familyName);
		family.setJoinCode(joinCode);
		
		// Save family first to get the ID
		family = familyRepository.save(family);

		// Assign current user as ADMIN and link to family
		currentUser.setFamily(family);
		currentUser.setRole(Role.ADMIN);
		userRepository.save(currentUser);

		return family;
	}

	/**
	 * Allows a user to join an existing family using a join code
	 * 
	 * @param joinCode Family join code
	 * @param currentUser User joining the family
	 * @return Updated user
	 * @throws FamilyAlreadyExistsException if user already belongs to a family
	 * @throws InvalidJoinCodeException if join code is invalid
	 */
	@Transactional
	public User joinFamily(String joinCode, User currentUser) {
		// Ensure user does not already belong to a family
		if (currentUser.getFamily() != null) {
			throw new FamilyAlreadyExistsException("You are already a member of a family. Please leave your current family before joining another one.");
		}

		// Find family by join code
		Family family = familyRepository.findByJoinCode(joinCode)
				.orElseThrow(() -> new InvalidJoinCodeException("Invalid join code. Please check and try again."));

		// Assign user to family and set role as MEMBER
		currentUser.setFamily(family);
		currentUser.setRole(Role.MEMBER);

		// Save and return updated user
		return userRepository.save(currentUser);
	}

	/**
	 * Generates a unique join code that doesn't exist in the database
	 * 
	 * @return Unique join code
	 */
	private String generateUniqueJoinCode() {
		String joinCode;
		do {
			joinCode = JoinCodeGenerator.generateJoinCode();
		} while (familyRepository.findByJoinCode(joinCode).isPresent());
		return joinCode;
	}

}

