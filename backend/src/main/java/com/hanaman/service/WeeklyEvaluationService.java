package com.hanaman.service;

import com.hanaman.domain.CycleSummary;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import com.hanaman.repository.CycleSummaryRepository;
import com.hanaman.repository.UserExerciseRepository;
import com.hanaman.repository.WeeklyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 주차 종료/4주 사이클 종료 시점에 실행되는 핵심 판정 로직.
 * 직접 설계 영역 — 매일 자정 스케줄러(CycleTransitionScheduler)에서 호출된다.
 *
 * 규칙:
 *  - 한 주(7일)의 성공 기준: successDays >= 5
 *  - 4주 중 3주 이상 SUCCESS → 동작 마스터, 다음 동작 해금 가능
 *  - 마스터 실패 시 페널티 없이 같은 동작으로 사이클 연장 (currentWeek=1로 리셋, cycleStartDate 갱신)
 */
@Service
@RequiredArgsConstructor
public class WeeklyEvaluationService {

    // NotificationService에서도 같은 기준을 참조하므로 public으로 노출한다.
    public static final int SUCCESS_DAYS_THRESHOLD = 5;
    public static final int WEEKS_PER_CYCLE = 4;
    private static final int MASTER_REQUIRED_SUCCESS_WEEKS = 3;

    private final WeeklyGoalRepository weeklyGoalRepository;
    private final UserExerciseRepository userExerciseRepository;
    private final CycleSummaryRepository cycleSummaryRepository;
    private final AdaptiveGoalService adaptiveGoalService;

    @Transactional
    public void finalizeWeek(UserExercise activeExercise, LocalDate cycleReferenceDate) {
        WeeklyGoal currentWeekGoal = weeklyGoalRepository
                .findByUserExerciseAndWeekNumber(activeExercise, activeExercise.getCurrentWeek())
                .orElseThrow(() -> new IllegalStateException("현재 주차 WeeklyGoal 없음"));

        boolean success = currentWeekGoal.getSuccessDays() >= SUCCESS_DAYS_THRESHOLD;
        currentWeekGoal.setResult(success ? WeeklyGoal.Result.SUCCESS : WeeklyGoal.Result.FAIL);
        weeklyGoalRepository.save(currentWeekGoal);

        if (activeExercise.getCurrentWeek() >= WEEKS_PER_CYCLE) {
            finalizeCycle(activeExercise, cycleReferenceDate);
            return;
        }

        // 다음 주로 전환: 목표치 적응형 조정 후 새 WeeklyGoal 생성
        int nextTarget = adaptiveGoalService.calculateNextWeekTarget(
                activeExercise.getCurrentTarget(),
                currentWeekGoal.getSuccessDays(),
                7,
                activeExercise.getExercise()
        );

        activeExercise.setCurrentWeek(activeExercise.getCurrentWeek() + 1);
        activeExercise.setCurrentTarget(nextTarget);
        userExerciseRepository.save(activeExercise);

        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(activeExercise)
                .weekNumber(activeExercise.getCurrentWeek())
                .targetValue(nextTarget)
                .successDays(0)
                .result(WeeklyGoal.Result.IN_PROGRESS)
                .build());
    }

    private void finalizeCycle(UserExercise activeExercise, LocalDate cycleReferenceDate) {
        long successWeeks = weeklyGoalRepository.findAllByUserExerciseOrderByWeekNumberAsc(activeExercise)
                .stream()
                .filter(w -> w.getResult() == WeeklyGoal.Result.SUCCESS)
                .count();

        CycleSummary.CycleSummaryBuilder summaryBuilder = CycleSummary.builder()
                .user(activeExercise.getUser())
                .userExercise(activeExercise)
                .exerciseName(activeExercise.getExercise().getName())
                .successWeeks((int) successWeeks)
                .totalWeeks(WEEKS_PER_CYCLE)
                .cycleEndDate(cycleReferenceDate)
                .acknowledged(false);

        if (successWeeks >= MASTER_REQUIRED_SUCCESS_WEEKS) {
            activeExercise.setStatus(UserExercise.Status.MASTERED);
            userExerciseRepository.save(activeExercise);
            // 다음 동작 선택은 컨트롤러/프론트에서 "MASTERED 상태 존재 + 현재 ACTIVE 없음"을 감지해 해금 UI를 노출
            cycleSummaryRepository.save(summaryBuilder.outcome(CycleSummary.Outcome.MASTERED).build());
        } else {
            // 페널티 없이 동일 동작으로 사이클 연장
            activeExercise.setStatus(UserExercise.Status.EXTENDED);
            activeExercise.setCurrentWeek(1);
            activeExercise.setCycleStartDate(cycleReferenceDate);
            // 목표치는 마지막 주 값을 그대로 유지 (급격한 리셋 방지)
            userExerciseRepository.save(activeExercise);

            weeklyGoalRepository.save(WeeklyGoal.builder()
                    .userExercise(activeExercise)
                    .weekNumber(1)
                    .targetValue(activeExercise.getCurrentTarget())
                    .successDays(0)
                    .result(WeeklyGoal.Result.IN_PROGRESS)
                    .build());

            activeExercise.setStatus(UserExercise.Status.ACTIVE);
            userExerciseRepository.save(activeExercise);
            cycleSummaryRepository.save(summaryBuilder.outcome(CycleSummary.Outcome.EXTENDED).build());
        }
    }
}
