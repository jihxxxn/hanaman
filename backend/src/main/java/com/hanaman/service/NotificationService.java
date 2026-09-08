package com.hanaman.service;

import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * "오늘의 미션" 화면 상단에 뜨는 한 줄 알림 문구를 결정하는 로직.
 * 직접 설계 영역 — 실제 푸시 발송 인프라는 아직 없어서, 우선은 이 문구를
 * TodayMissionResponse에 실어 프론트가 배너로 보여주는 형태로 시작한다.
 *
 * 우선순위 (위에서부터 먼저 매칭되는 것을 쓴다):
 *  1. 사이클 마지막날 — 4주차 7일째
 *  2. 주 마감 임박 — 그 주 6~7일차인데 아직 그 주 성공 기준(5일)을 못 채운 경우
 *  3. 시작 — 1주차 (위 1, 2에 해당 안 하는 날)
 *  4. 중반 — 2주차 이상 (위 1, 2에 해당 안 하는 날)
 *
 * 문구에는 절대 "실패"라는 단어를 쓰지 않는다 — 앱 전체의 원칙과 동일.
 */
@Service
public class NotificationService {

    public String buildTodayNotification(UserExercise activeExercise, WeeklyGoal currentWeekGoal, LocalDate today) {
        int dayInWeek = dayInWeek(activeExercise, today);
        int successDays = currentWeekGoal != null ? currentWeekGoal.getSuccessDays() : 0;
        boolean weekAlreadySucceeded = successDays >= WeeklyEvaluationService.SUCCESS_DAYS_THRESHOLD;

        boolean isLastDayOfCycle = activeExercise.getCurrentWeek() >= WeeklyEvaluationService.WEEKS_PER_CYCLE
                && dayInWeek >= 7;
        if (isLastDayOfCycle) {
            return "오늘이 이번 사이클 마지막 날이에요";
        }

        boolean isWeekDeadlineNear = dayInWeek >= 6 && !weekAlreadySucceeded;
        if (isWeekDeadlineNear) {
            int remaining = Math.max(0, WeeklyEvaluationService.SUCCESS_DAYS_THRESHOLD - successDays);
            return "이번 주까지 " + remaining + "일 채우면 이번 주는 성공이에요";
        }

        if (activeExercise.getCurrentWeek() <= 1) {
            return "시작이에요, 오늘 하나만 채우면 돼요";
        }

        return "오늘, 하나만";
    }

    /** 이번 주(currentWeek)의 며칠째인지 — 1~7로 클램프. */
    private int dayInWeek(UserExercise activeExercise, LocalDate today) {
        LocalDate weekStart = activeExercise.getCycleStartDate().plusWeeks(activeExercise.getCurrentWeek() - 1L);
        long diff = ChronoUnit.DAYS.between(weekStart, today) + 1;
        return (int) Math.min(Math.max(diff, 1), 7);
    }

    // 너무 작은 값(예: 1일)에서까지 "최고 기록"이라고 하면 과장처럼 느껴져서 최소치를 둔다.
    private static final int RECORD_MIN_THRESHOLD = 3;

    /**
     * 롤링 스트릭이 오늘 새 개인 기록을 세웠을 때만 담담한 톤으로 알려준다.
     * "경신!", "축하" 같은 경쟁/성취 프레이밍은 쓰지 않는다 — 실패 라벨을 안 쓰는 것과 같은 이유로,
     * 잘했을 때도 과도하게 띄우지 않는 톤을 유지한다.
     */
    public String buildRollingStreakRecordMessage(User user, int rollingCount, LocalDate today) {
        if (rollingCount < RECORD_MIN_THRESHOLD) {
            return null;
        }
        Integer best = user.getBestRollingStreak();
        LocalDate bestDate = user.getBestRollingStreakDate();
        if (best == null || bestDate == null) {
            return null;
        }
        if (rollingCount != best || !bestDate.equals(today)) {
            return null;
        }
        return "지금까지 중 가장 꾸준했던 주예요";
    }
}
