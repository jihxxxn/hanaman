package com.hanaman.repository;

import com.hanaman.domain.CheckIn;
import com.hanaman.domain.UserExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Optional<CheckIn> findByUserExerciseAndDate(UserExercise userExercise, LocalDate date);

    List<CheckIn> findAllByUserExerciseAndDateBetween(UserExercise userExercise, LocalDate start, LocalDate end);
}
