package com.hanaman.scheduler;

import com.hanaman.domain.UserExercise;
import com.hanaman.repository.UserExerciseRepository;
import com.hanaman.service.WeeklyEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 매일 자정 실행 — 사용자가 앱을 켜지 않아도 주차/사이클 전환이 정확히 이뤄지도록 보장.
 * "오늘이 그 주의 7일째를 막 넘긴 시점인가"를 판단해 WeeklyEvaluationService에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class CycleTransitionScheduler {

    private final UserExerciseRepository userExerciseRepository;
    private final WeeklyEvaluationService weeklyEvaluationService;

    @Scheduled(cron = "0 5 0 * * *") // 매일 00:05
    public void evaluateDueWeeks() {
        LocalDate today = LocalDate.now();
        List<UserExercise> activeExercises = userExerciseRepository.findAll().stream()
                .filter(ue -> ue.getStatus() == UserExercise.Status.ACTIVE)
                .toList();

        for (UserExercise ue : activeExercises) {
            long daysElapsedInCurrentWeek = ChronoUnit.DAYS.between(
                    weekStartDate(ue), today);

            if (daysElapsedInCurrentWeek >= 7) {
                weeklyEvaluationService.finalizeWeek(ue, today);
            }
        }
    }

    private LocalDate weekStartDate(UserExercise ue) {
        return ue.getCycleStartDate().plusWeeks(ue.getCurrentWeek() - 1L);
    }
}
