package com.hanaman.repository;

import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, UUID> {

    List<WeeklyGoal> findAllByUserExerciseOrderByWeekNumberAsc(UserExercise userExercise);

    Optional<WeeklyGoal> findByUserExerciseAndWeekNumber(UserExercise userExercise, Integer weekNumber);
}
