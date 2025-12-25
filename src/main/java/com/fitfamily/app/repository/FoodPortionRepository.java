package com.fitfamily.app.repository;

import com.fitfamily.app.model.FoodPortion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FoodPortionRepository extends JpaRepository<FoodPortion, UUID> {

}

