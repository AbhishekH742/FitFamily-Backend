package com.fitfamily.app.dto;

import com.fitfamily.app.model.MealType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodLogResponse {

	private String foodName;
	private String portionLabel;
	private double calories;
	private MealType mealType;

}

