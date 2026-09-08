package com.hanaman.service;

import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private final NotificationService service = new NotificationService();
    private final LocalDate today = LocalDate.of(2026, 9, 8);

    @Test
    void 사이클_1주차_시작_문구() {
        UserExercise active = exercise(today, 1); // 오늘이 1주차 1일째
        String message = service.buildTodayNotification(active, weeklyGoal(0), today);
        assertThat(message).isEqualTo("시작이에요, 오늘 하나만 채우면 돼요");
    }

    @Test
    void 사이클_2주차_이상은_중반_문구() {
        UserExercise active = exercise(today.minusDays(2), 2); // 2주차 3일째
        String message = service.buildTodayNotification(active, weeklyGoal(1), today);
        assertThat(message).isEqualTo("오늘, 하나만");
    }

    @Test
    void 주_마감_임박이면_남은_일수_안내() {
        UserExercise active = exercise(today.minusDays(5), 1); // 1주차 6일째
        String message = service.buildTodayNotification(active, weeklyGoal(2), today);
        assertThat(message).isEqualTo("이번 주까지 3일 채우면 이번 주는 성공이에요");
    }

    @Test
    void 이미_이번주_성공했으면_마감_임박_문구_안_뜸() {
        UserExercise active = exercise(today.minusDays(5), 1); // 1주차 6일째, 이미 5일 달성
        String message = service.buildTodayNotification(active, weeklyGoal(5), today);
        assertThat(message).isEqualTo("시작이에요, 오늘 하나만 채우면 돼요");
    }

    @Test
    void 사이클_마지막날은_다른_조건보다_우선() {
        // 4주차 7일째 — 아직 그 주 성공 기준도 못 채운 상태(마감 임박 조건도 동시에 성립)지만
        // "사이클 마지막날" 문구가 우선해야 한다.
        UserExercise active = exercise(today.minusWeeks(3).minusDays(6), 4);
        String message = service.buildTodayNotification(active, weeklyGoal(1), today);
        assertThat(message).isEqualTo("오늘이 이번 사이클 마지막 날이에요");
    }

    @Test
    void 오늘_개인_최고_기록을_세우면_담담한_문구를_보여준다() {
        User user = User.builder().bestRollingStreak(4).bestRollingStreakDate(today).build();
        String message = service.buildRollingStreakRecordMessage(user, 4, today);
        assertThat(message).isEqualTo("지금까지 중 가장 꾸준했던 주예요");
    }

    @Test
    void 최고_기록이어도_오늘_세운_게_아니면_문구_안_뜸() {
        User user = User.builder().bestRollingStreak(4).bestRollingStreakDate(today.minusDays(1)).build();
        String message = service.buildRollingStreakRecordMessage(user, 4, today);
        assertThat(message).isNull();
    }

    @Test
    void 최소_기준_미만이면_기록이어도_문구_안_뜸() {
        User user = User.builder().bestRollingStreak(2).bestRollingStreakDate(today).build();
        String message = service.buildRollingStreakRecordMessage(user, 2, today);
        assertThat(message).isNull();
    }

    @Test
    void 지금_최고_기록이_아니면_문구_안_뜸() {
        User user = User.builder().bestRollingStreak(6).bestRollingStreakDate(today.minusDays(2)).build();
        String message = service.buildRollingStreakRecordMessage(user, 4, today);
        assertThat(message).isNull();
    }

    private UserExercise exercise(LocalDate cycleStartDate, int currentWeek) {
        return UserExercise.builder()
                .cycleStartDate(cycleStartDate)
                .currentWeek(currentWeek)
                .build();
    }

    private WeeklyGoal weeklyGoal(int successDays) {
        return WeeklyGoal.builder().successDays(successDays).build();
    }
}
