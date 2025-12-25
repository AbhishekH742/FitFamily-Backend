package com.fitfamily.app.controller;

import com.fitfamily.app.dto.FamilyMemberDashboardResponse;
import com.fitfamily.app.dto.UserDailyDashboardResponse;
import com.fitfamily.app.model.User;
import com.fitfamily.app.service.DashboardService;
import com.fitfamily.app.util.SecurityUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;
	private final SecurityUtil securityUtil;

	public DashboardController(DashboardService dashboardService, SecurityUtil securityUtil) {
		this.dashboardService = dashboardService;
		this.securityUtil = securityUtil;
	}

	/**
	 * Get daily dashboard for the logged-in user
	 * 
	 * GET /dashboard/daily
	 * GET /dashboard/daily?date=2025-12-25
	 * 
	 * @param date Optional date parameter (defaults to today)
	 * @return UserDailyDashboardResponse with summary and food logs
	 */
	@GetMapping("/daily")
	public ResponseEntity<UserDailyDashboardResponse> getDailyDashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		
		User currentUser = securityUtil.getCurrentUser();
		
		// Default to today if no date provided
		LocalDate targetDate = date != null ? date : LocalDate.now();
		
		UserDailyDashboardResponse dashboard = dashboardService.getUserDailyDashboard(currentUser, targetDate);
		
		return ResponseEntity.ok(dashboard);
	}

	/**
	 * Get family-wide dashboard for all family members
	 * 
	 * GET /dashboard/family
	 * GET /dashboard/family?date=2025-12-25
	 * 
	 * @param date Optional date parameter (defaults to today)
	 * @return List of FamilyMemberDashboardResponse for each family member
	 */
	@GetMapping("/family")
	public ResponseEntity<List<FamilyMemberDashboardResponse>> getFamilyDashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		
		User currentUser = securityUtil.getCurrentUser();
		
		// Default to today if no date provided
		LocalDate targetDate = date != null ? date : LocalDate.now();
		
		List<FamilyMemberDashboardResponse> familyDashboards = dashboardService.getFamilyDailyDashboard(currentUser, targetDate);
		
		return ResponseEntity.ok(familyDashboards);
	}

}

