package com.fitfamily.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyDashboardResponse {

	private LocalDate date;
	private DailyMacroSummary summary;
	private List<FoodLogResponse> foodLogs;

}

