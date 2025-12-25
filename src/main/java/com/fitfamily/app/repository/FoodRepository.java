package com.fitfamily.app.repository;

import com.fitfamily.app.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FoodRepository extends JpaRepository<Food, UUID> {

	List<Food> findByNameContainingIgnoreCase(String name);

}

