package com.fitfamily.app.config;

import com.fitfamily.app.model.Food;
import com.fitfamily.app.model.FoodPortion;
import com.fitfamily.app.repository.FoodPortionRepository;
import com.fitfamily.app.repository.FoodRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

	private final FoodRepository foodRepository;
	private final FoodPortionRepository foodPortionRepository;

	public DataSeeder(FoodRepository foodRepository, FoodPortionRepository foodPortionRepository) {
		this.foodRepository = foodRepository;
		this.foodPortionRepository = foodPortionRepository;
	}

	@Override
	public void run(String... args) throws Exception {
		// Check if foods table is already populated
		if (foodRepository.count() > 0) {
			System.out.println("Database already seeded. Skipping data seeding.");
			return;
		}

		System.out.println("Seeding database with sample food data...");

		// Seed Rice
		Food rice = createFood("Rice", 130, 2.7, 28.2, 0.3);
		createPortion(rice, "100g", 100);
		createPortion(rice, "1 cup (cooked)", 158);
		createPortion(rice, "1 bowl", 200);
		createPortion(rice, "1 serving", 150);

		// Seed Chapati
		Food chapati = createFood("Chapati", 297, 9.6, 50.8, 6.1);
		createPortion(chapati, "1 small (40g)", 40);
		createPortion(chapati, "1 medium (50g)", 50);
		createPortion(chapati, "1 large (60g)", 60);

		// Seed Chicken Breast
		Food chickenBreast = createFood("Chicken Breast", 165, 31, 0, 3.6);
		createPortion(chickenBreast, "100g", 100);
		createPortion(chickenBreast, "1 piece (150g)", 150);
		createPortion(chickenBreast, "1 serving (200g)", 200);

		System.out.println("Database seeding completed successfully!");
	}

	private Food createFood(String name, double calories, double protein, double carbs, double fat) {
		Food food = new Food();
		food.setName(name);
		food.setCaloriesPer100g(calories);
		food.setProteinPer100g(protein);
		food.setCarbsPer100g(carbs);
		food.setFatPer100g(fat);
		return foodRepository.save(food);
	}

	private void createPortion(Food food, String label, double grams) {
		FoodPortion portion = new FoodPortion();
		portion.setFood(food);
		portion.setLabel(label);
		portion.setGrams(grams);
		foodPortionRepository.save(portion);
	}

}

