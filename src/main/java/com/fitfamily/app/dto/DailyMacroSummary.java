package com.fitfamily.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyMacroSummary {

	private double calories;
	private double protein;
	private double carbs;
	private double fat;

}

