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
                    .userExercise(ue).cycleNumber(1).weekNumber(week).targetValue(5)
                    .successDays(5).result(WeeklyGoal.Result.SUCCESS).build());
        }
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).cycleNumber(1).weekNumber(4).targetValue(5)
                .successDays(5).result(WeeklyGoal.Result.IN_PROGRESS).build());

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now());

        UserExercise updated = userExerciseRepository.findById(ue.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserExercise.Status.MASTERED);

        CycleSummary summary = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.MASTERED)
                .orElseThrow();
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
                .userExercise(ue).cycleNumber(1).weekNumber(1).targetValue(5).successDays(2).result(WeeklyGoal.Result.FAIL).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).cycleNumber(1).weekNumber(2).targetValue(4).successDays(2).result(WeeklyGoal.Result.FAIL).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).cycleNumber(1).weekNumber(3).targetValue(4).successDays(5).result(WeeklyGoal.Result.SUCCESS).build());
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).cycleNumber(1).weekNumber(4).targetValue(4).successDays(2).result(WeeklyGoal.Result.IN_PROGRESS).build());

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now());

        UserExercise updated = userExerciseRepository.findById(ue.getId()).orElseThrow();
        // 연장은 페널티 없이 같은 동작으로 이어지므로 최종 상태는 다시 ACTIVE, 목표는 마지막 값 유지
        assertThat(updated.getStatus()).isEqualTo(UserExercise.Status.ACTIVE);
        assertThat(updated.getCurrentWeek()).isEqualTo(1);
        assertThat(updated.getCurrentTarget()).isEqualTo(4);

        CycleSummary summary = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.EXTENDED)
                .orElseThrow();
        assertThat(summary.getOutcome()).isEqualTo(CycleSummary.Outcome.EXTENDED);
        assertThat(summary.getSuccessWeeks()).isEqualTo(1);
    }

    @Test
    void 두_번째_연장도_이전_사이클과_주차가_안_섞인다() {
        // 회귀 테스트: cycleNumber로 구분하기 전에는 연장이 두 번째로 일어날 때
        // weekNumber=1인 WeeklyGoal이 사이클마다 새로 생기면서 이전 사이클 것과 충돌해
        // NonUniqueResultException이 났었다.
        User user = userRepository.save(User.builder().nickname("두번째연장테스트").build());
        Exercise exercise = exerciseRepository.findAll().get(0);

        UserExercise ue = userExerciseRepository.save(UserExercise.builder()
                .user(user).exercise(exercise).status(UserExercise.Status.ACTIVE)
                .cycleStartDate(LocalDate.now().minusWeeks(3))
                .currentWeek(4).currentTarget(4).orderIndex(0)
                .build());

        for (int week = 1; week <= 4; week++) {
            weeklyGoalRepository.save(WeeklyGoal.builder()
                    .userExercise(ue).cycleNumber(1).weekNumber(week).targetValue(4)
                    .successDays(2).result(week == 4 ? WeeklyGoal.Result.IN_PROGRESS : WeeklyGoal.Result.FAIL)
                    .build());
        }

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now()); // 1차 연장 → cycleNumber 2로

        UserExercise afterFirstExtension = userExerciseRepository.findById(ue.getId()).orElseThrow();
        assertThat(afterFirstExtension.getCycleNumber()).isEqualTo(2);
        assertThat(afterFirstExtension.getCurrentWeek()).isEqualTo(1);

        // 2번째 사이클의 2~4주차를 채우고 다시 4주차까지 진행
        for (int week = 2; week <= 4; week++) {
            afterFirstExtension.setCurrentWeek(week);
            userExerciseRepository.save(afterFirstExtension);
            weeklyGoalRepository.save(WeeklyGoal.builder()
                    .userExercise(afterFirstExtension).cycleNumber(2).weekNumber(week).targetValue(4)
                    .successDays(2).result(week == 4 ? WeeklyGoal.Result.IN_PROGRESS : WeeklyGoal.Result.FAIL)
                    .build());
        }

        // 이전에는 여기서 currentWeekGoal 조회가 cycleNumber 없이 weekNumber=1로만 걸려서
        // 1차/2차 사이클의 weekNumber=1 두 행이 걸려 NonUniqueResultException이 났었다.
        weeklyEvaluationService.finalizeWeek(afterFirstExtension, LocalDate.now());

        UserExercise afterSecondExtension = userExerciseRepository.findById(ue.getId()).orElseThrow();
        assertThat(afterSecondExtension.getCycleNumber()).isEqualTo(3);
        assertThat(afterSecondExtension.getCurrentWeek()).isEqualTo(1);

        // 2번째 사이클의 successWeeks가 1번째 사이클 것과 안 섞였는지 확인 (둘 다 0으로 계산돼야 함)
        CycleSummary secondSummary = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.EXTENDED)
                .orElseThrow();
        assertThat(secondSummary.getSuccessWeeks()).isEqualTo(0);
    }
}
