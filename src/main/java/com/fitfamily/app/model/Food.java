package com.fitfamily.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "foods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Food {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false)
	private double caloriesPer100g;

	@Column(nullable = false)
	private double proteinPer100g;

	@Column(nullable = false)
	private double carbsPer100g;

	@Column(nullable = false)
	private double fatPer100g;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "food", fetch = FetchType.LAZY)
	private List<FoodPortion> foodPortions;

}

