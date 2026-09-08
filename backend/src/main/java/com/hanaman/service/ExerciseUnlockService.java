package com.hanaman.service;

import com.hanaman.domain.Exercise;
import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import com.hanaman.repository.ExerciseRepository;
import com.hanaman.repository.UserExerciseRepository;
import com.hanaman.repository.WeeklyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 4주 마스터 이후에만 호출 가능한 "다음 동작 시작" 로직.
 * 이미 ACTIVE 상태의 UserExercise가 있으면 예외를 던져 동시에 2개 동작이 진행되지 않도록 강제한다.
 */
@Service
@RequiredArgsConstructor
public class ExerciseUnlockService {

    private final UserExerciseRepository userExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final WeeklyGoalRepository weeklyGoalRepository;

    @Transactional
    public UserExercise startNewExercise(User user, UUID exerciseId) {
        userExerciseRepository.findByUserAndStatus(user, UserExercise.Status.ACTIVE)
                .ifPresent(existing -> {
                    throw new IllegalStateException("이미 진행 중인 활성 동작이 있습니다. 한 번에 하나만 진행할 수 있어요.");
                });

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동작입니다."));

        int nextOrderIndex = userExerciseRepository.findAllByUserOrderByOrderIndexAsc(user).size();

        UserExercise userExercise = userExerciseRepository.save(UserExercise.builder()
                .user(user)
                .exercise(exercise)
                .status(UserExercise.Status.ACTIVE)
                .cycleStartDate(LocalDate.now())
                .currentWeek(1)
                .currentTarget(exercise.getDefaultStartValue())
                .orderIndex(nextOrderIndex)
                .build());

        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(userExercise)
                .weekNumber(1)
                .targetValue(exercise.getDefaultStartValue())
                .successDays(0)
                .result(WeeklyGoal.Result.IN_PROGRESS)
                .build());

        return userExercise;
    }
}
