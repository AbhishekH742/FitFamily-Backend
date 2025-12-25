package com.fitfamily.app.service;

import com.fitfamily.app.dto.*;
import com.fitfamily.app.model.FoodLog;
import com.fitfamily.app.model.User;
import com.fitfamily.app.repository.FoodLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

	private final FoodLogRepository foodLogRepository;

	public DashboardService(FoodLogRepository foodLogRepository) {
		this.foodLogRepository = foodLogRepository;
	}

	/**
	 * Get daily dashboard for a specific user
	 * 
	 * @param user The user whose dashboard to retrieve
	 * @param date The date for the dashboard
	 * @return UserDailyDashboardResponse with summary and food logs
	 */
	public UserDailyDashboardResponse getUserDailyDashboard(User user, LocalDate date) {
		// Fetch food logs for user and date
		List<FoodLog> foodLogs = foodLogRepository.findByUserAndDate(user, date);

		// Calculate total calories and macros
		double totalCalories = 0;
		double totalProtein = 0;
		double totalCarbs = 0;
		double totalFat = 0;

		for (FoodLog log : foodLogs) {
			totalCalories += log.getCalories();
			totalProtein += log.getProtein();
			totalCarbs += log.getCarbs();
			totalFat += log.getFat();
		}

		// Create macro summary
		DailyMacroSummary summary = new DailyMacroSummary(
			totalCalories,
			totalProtein,
			totalCarbs,
			totalFat
		);

		// Map food logs to response DTOs
		List<FoodLogResponse> foodLogResponses = foodLogs.stream()
				.map(this::mapToFoodLogResponse)
				.collect(Collectors.toList());

		// Return dashboard response
		return new UserDailyDashboardResponse(date, summary, foodLogResponses);
	}

	/**
	 * Get daily dashboard for all members of user's family
	 * 
	 * @param user The user (member of the family)
	 * @param date The date for the dashboard
	 * @return List of FamilyMemberDashboardResponse for each family member
	 */
	public List<FamilyMemberDashboardResponse> getFamilyDailyDashboard(User user, LocalDate date) {
		// Check if user has a family
		if (user.getFamily() == null) {
			return new ArrayList<>();
		}

		// Fetch all food logs for the family on the given date
		List<FoodLog> familyLogs = foodLogRepository.findByFamilyAndDate(user.getFamily(), date);

		// Group logs by user
		Map<User, List<FoodLog>> logsByUser = familyLogs.stream()
				.collect(Collectors.groupingBy(FoodLog::getUser));

		// Build dashboard for each family member
		List<FamilyMemberDashboardResponse> familyDashboards = new ArrayList<>();

		for (Map.Entry<User, List<FoodLog>> entry : logsByUser.entrySet()) {
			User familyMember = entry.getKey();
			List<FoodLog> memberLogs = entry.getValue();

			// Calculate totals for this member
			double totalCalories = 0;
			double totalProtein = 0;
			double totalCarbs = 0;
			double totalFat = 0;

			for (FoodLog log : memberLogs) {
				totalCalories += log.getCalories();
				totalProtein += log.getProtein();
				totalCarbs += log.getCarbs();
				totalFat += log.getFat();
			}

			// Create macro summary
			DailyMacroSummary summary = new DailyMacroSummary(
				totalCalories,
				totalProtein,
				totalCarbs,
				totalFat
			);

			// Map food logs to response DTOs
			List<FoodLogResponse> foodLogResponses = memberLogs.stream()
					.map(this::mapToFoodLogResponse)
					.collect(Collectors.toList());

			// Create user dashboard
			UserDailyDashboardResponse userDashboard = new UserDailyDashboardResponse(
				date,
				summary,
				foodLogResponses
			);

			// Wrap in family member response
			FamilyMemberDashboardResponse memberDashboard = new FamilyMemberDashboardResponse(
				familyMember.getName(),
				userDashboard
			);

			familyDashboards.add(memberDashboard);
		}

		return familyDashboards;
	}

	/**
	 * Map FoodLog entity to FoodLogResponse DTO
	 * 
	 * @param foodLog The food log entity
	 * @return FoodLogResponse DTO
	 */
	private FoodLogResponse mapToFoodLogResponse(FoodLog foodLog) {
		return new FoodLogResponse(
			foodLog.getFood().getName(),
			foodLog.getPortion().getLabel(),
			foodLog.getCalories(),
			foodLog.getMealType()
		);
	}

}

