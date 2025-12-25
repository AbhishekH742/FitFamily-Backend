package com.fitfamily.app.dto;

import com.fitfamily.app.model.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddFoodLogRequest {

	@NotNull(message = "Food ID is required")
	private UUID foodId;

	@NotNull(message = "Portion ID is required")
	private UUID portionId;

	@NotNull(message = "Meal type is required")
	private MealType mealType;

}

