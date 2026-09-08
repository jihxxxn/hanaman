package com.hanaman.service;

import com.hanaman.domain.CycleSummary;
import com.hanaman.domain.Exercise;
import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import com.hanaman.repository.CycleSummaryRepository;
import com.hanaman.repository.ExerciseRepository;
import com.hanaman.repository.UserExerciseRepository;
import com.hanaman.repository.UserRepository;
import com.hanaman.repository.WeeklyGoalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** 4주 사이클이 끝나는 순간 CycleSummary가 올바르게 생성되는지 검증 (기능 2). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class WeeklyEvaluationServiceCycleSummaryTest {

    @Autowired private WeeklyEvaluationService weeklyEvaluationService;
    @Autowired private UserRepository userRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private UserExerciseRepository userExerciseRepository;
    @Autowired private WeeklyGoalRepository weeklyGoalRepository;
    @Autowired private CycleSummaryRepository cycleSummaryRepository;

    @Test
    void 마스터_조건을_채우면_요약이_MASTERED로_생성된다() {
        User user = userRepository.save(User.builder().nickname("마스터테스트").build());
        Exercise exercise = exerciseRepository.findAll().get(0);

        UserExercise ue = userExerciseRepository.save(UserExercise.builder()
                .user(user).exercise(exercise).status(UserExercise.Status.ACTIVE)
                .cycleStartDate(LocalDate.now().minusWeeks(3))
                .currentWeek(4).currentTarget(5).orderIndex(0)
                .build());

        for (int week = 1; week <= 3; week++) {
            weeklyGoalRepository.save(WeeklyGoal.builder()
                    .userExercise(ue).weekNumber(week).targetValue(5)
                    .successDays(5).result(WeeklyGoal.Result.SUCCESS).build());
        }
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).weekNumber(4).targetValue(5)
                .successDays(5).result(WeeklyGoal.Result.IN_PROGRESS).build());

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now());

        UserExercise updated = userExerciseRepository.findById(ue.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserExercise.Status.MASTERED);

        CycleSummary summary = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseOrderByCycleEndDateDesc(user).orElseThrow();
        assertThat(summary.getOutcome()).isEqualTo(CycleSummary.Outcome.MASTERED);
        assertThat(summary.getSuccessWeeks()).isEqualTo(4);
        assertThat(summary.getTotalWeeks()).isEqualTo(4);
        assertThat(summary.getAcknowledged()).isFalse();
    }

    @Test
    void 마스터_실패시_EXTENDED로_생성되고_사이클이_페널티_없이_연장된다() {
        User user = userRepository.save(User.builder().nickname("연장테스트").build());
        Exercise exercise = exerciseRepository.findAll().get(0);

        UserExercise ue = userExerciseRepository.save(UserExercise.builder()
                .user(user).exercise(exercise).status(UserExercise.Status.ACTIVE)
                .cycleStartDate(LocalDate.now().minusWeeks(3))
                .currentWeek(4).currentTarget(4).orderIndex(0)
                .build());

        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).weekNumber(1).targetValue(5).successDays(2).result(WeeklyGoal.Result.FAIL).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).weekNumber(2).targetValue(4).successDays(2).result(WeeklyGoal.Result.FAIL).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).weekNumber(3).targetValue(4).successDays(5).result(WeeklyGoal.Result.SUCCESS).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).weekNumber(4).targetValue(4).successDays(2).result(WeeklyGoal.Result.IN_PROGRESS).build());

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now());

        UserExercise updated = userExerciseRepository.findById(ue.getId()).orElseThrow();
        // 연장은 페널티 없이 같은 동작으로 이어지므로 최종 상태는 다시 ACTIVE, 목표는 마지막 값 유지
        assertThat(updated.getStatus()).isEqualTo(UserExercise.Status.ACTIVE);
        assertThat(updated.getCurrentWeek()).isEqualTo(1);
        assertThat(updated.getCurrentTarget()).isEqualTo(4);

        CycleSummary summary = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseOrderByCycleEndDateDesc(user).orElseThrow();
        assertThat(summary.getOutcome()).isEqualTo(CycleSummary.Outcome.EXTENDED);
        assertThat(summary.getSuccessWeeks()).isEqualTo(1);
    }
}
