package com.fitfamily.app.repository;

import com.fitfamily.app.model.Family;
import com.fitfamily.app.model.FoodLog;
import com.fitfamily.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, UUID> {

	List<FoodLog> findByUserAndDate(User user, LocalDate date);

	List<FoodLog> findByFamilyAndDate(Family family, LocalDate date);

	Optional<FoodLog> findByIdAndUser(UUID id, User user);

}

