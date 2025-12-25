package com.fitfamily.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberDashboardResponse {

	private String userName;
	private UserDailyDashboardResponse dashboard;

}

