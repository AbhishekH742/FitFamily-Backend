package com.fitfamily.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "family_id")
	private Family family;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "food_id", nullable = false)
	private Food food;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "portion_id", nullable = false)
	private FoodPortion portion;

	@Column(nullable = false)
	private double calories;

	@Column(nullable = false)
	private double protein;

	@Column(nullable = false)
	private double carbs;

	@Column(nullable = false)
	private double fat;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MealType mealType;

	@Column(nullable = false)
	private LocalDate date;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

}

