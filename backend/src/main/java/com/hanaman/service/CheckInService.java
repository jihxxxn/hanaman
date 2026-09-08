package com.hanaman.service;

import com.hanaman.domain.CheckIn;
import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import com.hanaman.repository.CheckInRepository;
import com.hanaman.repository.UserRepository;
import com.hanaman.repository.WeeklyGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * "오늘의 미션" 체크인을 기록하는 핵심 서비스.
 * 직접 설계 영역 — AI 위임 없이 도메인 규칙(하루 1회, 목표치 스냅샷, 주간 성공일수 갱신)을 그대로 구현.
 */
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final WeeklyGoalRepository weeklyGoalRepository;
    private final RollingStreakService rollingStreakService;
    private final UserRepository userRepository;

    @Transactional
    public CheckIn recordCheckIn(UserExercise activeExercise, LocalDate date, int completedValue) {
        // 하루 1회 원칙: 이미 체크인이 있으면 값만 갱신 (사용자가 나중에 더 채운 경우를 허용)
        CheckIn checkIn = checkInRepository.findByUserExerciseAndDate(activeExercise, date)
                .orElseGet(() -> CheckIn.builder()
                        .userExercise(activeExercise)
                        .date(date)
                        .targetValueSnapshot(activeExercise.getCurrentTarget())
                        .build());

        boolean wasAchieved = Boolean.TRUE.equals(checkIn.getAchieved());

        checkIn.setCompletedValue(completedValue);
        boolean achieved = completedValue >= checkIn.getTargetValueSnapshot();
        checkIn.setAchieved(achieved);

        CheckIn saved = checkInRepository.save(checkIn);

        // 새로 달성 상태가 된 경우에만 주간 성공일수 +1 (이미 달성된 날을 중복 기록하지 않도록)
        if (achieved && !wasAchieved) {
            incrementWeeklySuccessDays(activeExercise);
        } else if (!achieved && wasAchieved) {
            decrementWeeklySuccessDays(activeExercise);
        }

        // Redis 롤링 7일 캐시 갱신 (실시간 "이번 주 리듬" 조회용)
        rollingStreakService.markDay(activeExercise.getUser().getId(), date, achieved);
        updateBestRollingStreakIfNeeded(activeExercise.getUser(), date);

        return saved;
    }

    /** 방금 체크인으로 롤링 스트릭이 지금까지 최고 기록을 넘겼으면 User에 저장해둔다 (그날만 "새 기록" 문구를 보여주기 위함). */
    private void updateBestRollingStreakIfNeeded(User user, LocalDate date) {
        int freshRollingCount = rollingStreakService.getRollingSuccessCount(user.getId(), date);
        Integer best = user.getBestRollingStreak();
        if (best == null || freshRollingCount > best) {
            user.setBestRollingStreak(freshRollingCount);
            user.setBestRollingStreakDate(date);
            userRepository.save(user);
        }
    }

    private void incrementWeeklySuccessDays(UserExercise activeExercise) {
        WeeklyGoal weeklyGoal = weeklyGoalRepository
                .findByUserExerciseAndCycleNumberAndWeekNumber(
                        activeExercise, activeExercise.getCycleNumber(), activeExercise.getCurrentWeek())
                .orElseThrow(() -> new IllegalStateException("현재 주차의 WeeklyGoal이 존재하지 않습니다."));
        weeklyGoal.setSuccessDays(weeklyGoal.getSuccessDays() + 1);
        weeklyGoalRepository.save(weeklyGoal);
    }

    private void decrementWeeklySuccessDays(UserExercise activeExercise) {
        WeeklyGoal weeklyGoal = weeklyGoalRepository
                .findByUserExerciseAndCycleNumberAndWeekNumber(
                        activeExercise, activeExercise.getCycleNumber(), activeExercise.getCurrentWeek())
                .orElseThrow(() -> new IllegalStateException("현재 주차의 WeeklyGoal이 존재하지 않습니다."));
        weeklyGoal.setSuccessDays(Math.max(0, weeklyGoal.getSuccessDays() - 1));
        weeklyGoalRepository.save(weeklyGoal);
    }
}
