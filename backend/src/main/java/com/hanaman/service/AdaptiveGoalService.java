package com.hanaman.service;

import com.hanaman.domain.Exercise;
import org.springframework.stereotype.Service;

/**
 * 지난 주 성공률을 바탕으로 다음 주 목표치를 계산하는 핵심 도메인 로직.
 * 직접 설계 영역 — 규칙:
 *   성공률 >= 80%  → +15% (반올림, 최소 1 증가 보장)
 *   40% <= 성공률 < 80% → 유지
 *   성공률 < 40%   → -20% (동작별 minValue 하한 보장)
 */
@Service
public class AdaptiveGoalService {

    private static final double INCREASE_THRESHOLD = 0.8;
    private static final double DECREASE_THRESHOLD = 0.4;
    private static final double INCREASE_RATE = 0.15;
    private static final double DECREASE_RATE = 0.20;

    public int calculateNextWeekTarget(int currentTarget, int successDaysLastWeek, int daysInWeek, Exercise exercise) {
        double successRate = daysInWeek == 0 ? 0.0 : (double) successDaysLastWeek / daysInWeek;

        int nextTarget;
        if (successRate >= INCREASE_THRESHOLD) {
            int increase = (int) Math.round(currentTarget * INCREASE_RATE);
            nextTarget = currentTarget + Math.max(1, increase);
        } else if (successRate < DECREASE_THRESHOLD) {
            int decrease = (int) Math.round(currentTarget * DECREASE_RATE);
            nextTarget = currentTarget - Math.max(1, decrease);
        } else {
            nextTarget = currentTarget;
        }

        return Math.max(nextTarget, exercise.getMinValue());
    }
}
