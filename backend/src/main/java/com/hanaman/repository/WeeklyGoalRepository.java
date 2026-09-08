package com.hanaman.repository;

import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, UUID> {

    /** 특정 사이클(cycleNumber)에 속한 주차들만 — 마스터 판정은 "지금 사이클" 4주만 봐야 하므로 이걸 쓴다. */
    List<WeeklyGoal> findAllByUserExerciseAndCycleNumberOrderByWeekNumberAsc(UserExercise userExercise, Integer cycleNumber);

    Optional<WeeklyGoal> findByUserExerciseAndCycleNumberAndWeekNumber(
            UserExercise userExercise, Integer cycleNumber, Integer weekNumber);
}
